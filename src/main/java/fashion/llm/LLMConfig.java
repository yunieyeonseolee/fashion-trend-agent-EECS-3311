package fashion.llm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Local values only; never implements a toString that could expose credentials. */
public final class LLMConfig {
    private final String apiKey;
    private final String model;
    public LLMConfig(String apiKey, String model) { this.apiKey = apiKey; this.model = model; }
    public String apiKey() { return apiKey; }
    public String model() { return model; }

    public static LLMConfig load(Path path, Map<String, String> environment) throws IOException {
        var values = new HashMap<String, String>();
        if (Files.exists(path)) {
            for (String line : Files.readAllLines(path)) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int equals = line.indexOf('=');
                if (equals < 1) throw new IOException("Invalid .env format; expected KEY=value (values are not displayed)");
                String key = line.substring(0, equals).strip();
                String value = line.substring(equals + 1).strip();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) value = value.substring(1,value.length()-1);
                values.put(key,value);
            }
        }
        for (String key : new String[]{"LLM_API_KEY","OPENAI_API_KEY","LLM_MODEL"})
            if (environment.containsKey(key)) values.put(key,environment.get(key));
        String key = values.getOrDefault("LLM_API_KEY", values.get("OPENAI_API_KEY"));
        String model = values.get("LLM_MODEL");
        if (placeholder(key) || placeholder(model))
            throw new IOException("LLM is not configured. Set LLM_API_KEY and LLM_MODEL in the local .env file; deterministic commands still work.");
        return new LLMConfig(key,model);
    }
    private static boolean placeholder(String value) {
        return value == null || value.isBlank() || value.startsWith("YOUR_");
    }
}
