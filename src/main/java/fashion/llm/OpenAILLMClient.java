package fashion.llm;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/** Responses API transport. No raw error bodies or credentials are displayed. */
public final class OpenAILLMClient implements LLMClient {
    @FunctionalInterface public interface ConfigSource { LLMConfig load() throws IOException; }
    private final ConfigSource config;
    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient http;

    public OpenAILLMClient() {
        this(() -> LLMConfig.load(Path.of(".env"),System.getenv()),
                URI.create("https://api.openai.com/v1/responses"),Duration.ofSeconds(60));
    }
    /** Explicit loopback endpoint is available for network-contract tests only. */
    public OpenAILLMClient(ConfigSource config, URI endpoint, Duration timeout) {
        if (!endpoint.toString().equals("https://api.openai.com/v1/responses")
                && !("http".equals(endpoint.getScheme()) && "127.0.0.1".equals(endpoint.getHost())))
            throw new IllegalArgumentException("Unsupported LLM endpoint");
        this.config=config; this.endpoint=endpoint; this.timeout=timeout;
        this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public Reply complete(String instructions, String input, JsonObject schema) throws IOException {
        var settings = config.load();
        var body = new JsonObject();
        body.addProperty("model",settings.model());
        body.addProperty("instructions",instructions);
        body.addProperty("input",input);
        body.addProperty("store",false);
        body.addProperty("max_output_tokens",2000);
        if (schema != null) body.add("text", Json.GSON.toJsonTree(Map.of("format",Map.of(
                "type","json_schema","name","trend_plan","strict",true,"schema",schema))));
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Authorization","Bearer " + settings.apiKey())
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Json.write(body))).build();
            var response = http.send(request,HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IOException("LLM request failed (HTTP " + response.statusCode()
                    + "). Check credentials, model access, quota or service availability.");
            var root = Json.GSON.fromJson(response.body(),JsonObject.class);
            if (!"completed".equals(root.get("status").getAsString()))
                throw new IOException("LLM response did not complete; no partial narrative was used.");
            var text = new StringBuilder();
            for (var item : root.getAsJsonArray("output")) {
                var output = item.getAsJsonObject();
                if (!"message".equals(output.get("type").getAsString())) continue;
                for (var part : output.getAsJsonArray("content")) {
                    var content=part.getAsJsonObject();
                    if ("refusal".equals(content.get("type").getAsString()))
                        throw new IOException("The model declined this request. Try a specific fashion trend question.");
                    if ("output_text".equals(content.get("type").getAsString())) text.append(content.get("text").getAsString());
                }
            }
            if (text.toString().isBlank()) throw new IOException("LLM returned no usable text.");
            var usage = root.getAsJsonObject("usage");
            return new Reply(text.toString(),settings.model(),tokenCount(usage,"input_tokens"),tokenCount(usage,"output_tokens"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("LLM request interrupted.");
        } catch (java.net.http.HttpTimeoutException e) {
            throw new IOException("LLM request timed out; please try again.");
        } catch (IOException e) {
            // Our messages are safe, but transport exception text may include addresses.
            if (e.getMessage()!=null && (e.getMessage().startsWith("LLM") || e.getMessage().startsWith("The model"))) throw e;
            throw new IOException("Unable to reach the LLM service.");
        } catch (RuntimeException e) {
            throw new IOException("Invalid LLM response or local configuration; no raw response was displayed.");
        }
    }
    private long tokenCount(JsonObject usage,String field) {
        return usage == null || !usage.has(field) ? -1 : usage.get(field).getAsLong();
    }
}
