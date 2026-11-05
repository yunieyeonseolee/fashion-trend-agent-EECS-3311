package fashion;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import fashion.agent.*;
import fashion.application.*;
import fashion.command.*;
import fashion.data.*;
import fashion.llm.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import static fashion.application.AgentRequest.Operation.*;

/** Explicit local test doubles: no external requests, real API keys or production fake mode. */
public final class LLMIntegrationTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        configChecks();
        workflowChecks();
        transportChecks();
        System.out.println("Passed " + checks + " LLM integration checks (offline/local fake API only).");
    }
    private static TrendRepository repository() throws IOException {
        var repo=new TrendRepository(new CsvTrendAdapter(new CsvReader()));
        repo.importCsv(Path.of("data/sample-trends.csv")); return repo;
    }
    private static String plan(String operation,String name) {
        var step=new LinkedHashMap<String,Object>();
        step.put("operation",operation); step.put("name",name); step.put("secondName",null);
        step.put("category",null); step.put("from",null); step.put("to",null); step.put("ranking","MOMENTUM");
        return Json.write(Map.of("steps",List.of(step),"clarification",""));
    }
    private static LLMClient.Reply reply(String text) { return new LLMClient.Reply(text,"test-only-model",10,5); }
    private static void workflowChecks() throws Exception {
        var calls=new AtomicInteger();
        var evidence=new AtomicReference<String>();
        LLMClient fake=(instructions,input,schema) -> {
            calls.incrementAndGet();
            if (schema!=null) return reply(plan("ANALYZE","mary jane flats"));
            evidence.set(input);
            return reply("TEST ONLY: synthetic observations show calculated growth.");
        };
        var repo=repository();
        var agent=new FashionTrendAgent(repo,fake);
        var controller=new AgentController();
        var response=controller.execute(new AnalyzeCommand(agent,AgentRequest.trend(ASK,"How are Mary Jane flats doing?",null,null,null)));
        check(response.success() && calls.get()==2,"plan and synthesis are separate calls");
        check(response.analyses().get(0).momentum()>0,"actual analyzer executes between calls");
        check(evidence.get().contains("synthetic-example") && evidence.get().contains("momentum"),"model receives calculated metrics and sources");
        check(response.report().usage().size()==2,"usage retained for both replies");
        check(response.report().render().contains("TEST ONLY") && response.report().render().contains("ANALYZE"),"report shows trace and separate narrative");
        var direct=controller.execute(new ReportCommand(agent,AgentRequest.trend(REPORT,"mary jane flats",null,null,null)));
        check(direct.report().evidence().results().size()==3,"report executes history, analysis and category retrieval");
        check(direct.report().evidence().sources().equals(List.of("synthetic-example")),"report sources preserved");
        check(direct.signals().size()==4 && direct.analyses().size()==2,"report includes history and related trends");
        LLMClient unavailable=(i,input,schema) -> {throw new IOException("Test service unavailable");};
        var offline=new FashionTrendAgent(repository(),unavailable);
        var fallback=controller.execute(new ReportCommand(offline,AgentRequest.trend(REPORT,"mary jane flats",null,null,null)));
        check(fallback.success() && fallback.report().narrative().isEmpty() && !fallback.analyses().isEmpty(),"API failure preserves numeric report");
        check(fallback.warnings().stream().anyMatch(w -> w.contains("AI interpretation unavailable")),"fallback is explicit");
        check(!controller.execute(new AnalyzeCommand(offline,AgentRequest.trend(ASK,"question",null,null,null))).success(),"planning outage is an actionable error");
        var clarify=new FashionTrendAgent(repository(),(i,input,schema)->reply("{\"steps\":[],\"clarification\":\"Which footwear trend?\"}"));
        var clarified=controller.execute(new AnalyzeCommand(clarify,AgentRequest.trend(ASK,"Is it growing?",null,null,null)));
        check(clarified.message().equals("Which footwear trend?") && clarified.report()==null,"ambiguous input requests clarification without tools");
        var planner=new AgentPlanner(fake,repo);
        for (String invalid:List.of(plan("IMPORT","mary jane flats"),plan("ASK","mary jane flats"),plan("ANALYZE","invented trend"),
                plan("ANALYZE","mary jane flats").replace("\"from\":null","\"from\":\"bad-date\""),
                "{\"steps\":[],\"clarification\":\"\"}","not JSON")) {
            try {planner.parse(reply(invalid));throw new AssertionError("unsafe plan accepted");}
            catch(IOException expected){checks++;}
        }
        try {new ToolManager(agent,repo).invoke(AgentRequest.importCsv(Path.of("data/sample-trends.csv")));throw new AssertionError("write tool allowed");}
        catch(IllegalArgumentException expected){checks++;}
        var steps=new JsonArray();
        for(int i=0;i<5;i++) steps.add(Json.GSON.fromJson(plan("ANALYZE","mary jane flats"),JsonObject.class).getAsJsonArray("steps").get(0));
        var tooMany=new JsonObject();tooMany.add("steps",steps);tooMany.addProperty("clarification","");
        try{planner.parse(reply(tooMany.toString()));throw new AssertionError("unbounded plan accepted");}
        catch(IOException expected){checks++;}
    }
    private static void configChecks() throws Exception {
        Path file=Files.createTempFile("fashion-env-test-",".txt");
        try {
            Files.writeString(file,"# local\nLLM_API_KEY='test-not-a-real-key'\nLLM_MODEL=file-model\n");
            var settings=LLMConfig.load(file,Map.of("LLM_MODEL","environment-model"));
            check(settings.model().equals("environment-model") && settings.apiKey().equals("test-not-a-real-key"),"config quotes and env precedence");
            check(!settings.toString().contains("test-not-a-real-key"),"config toString does not expose key");
            Files.writeString(file,"LLM_API_KEY=YOUR_LOCAL_API_KEY_HERE\nLLM_MODEL=YOUR_CHOSEN_API_MODEL_ID\n");
            try{LLMConfig.load(file,Map.of());throw new AssertionError("placeholder accepted");}
            catch(IOException expected){check(expected.getMessage().contains("not configured"),"placeholder configuration rejected");}
        } finally {Files.deleteIfExists(file);}
    }
    private static void transportChecks() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var status=new AtomicInteger(200);
        var body=new AtomicReference<>(successBody("transport response"));
        var captured=new AtomicReference<JsonObject>();
        var auth=new AtomicReference<String>();
        server.createContext("/v1/responses",exchange->{
            captured.set(Json.GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8),JsonObject.class));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes=body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");
            exchange.sendResponseHeaders(status.get(),bytes.length);
            try(var output=exchange.getResponseBody()){output.write(bytes);}
            exchange.close();
        });
        server.start();
        try {
            var client=new OpenAILLMClient(()->new LLMConfig("fake-local-test-key","test-model"),
                    URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v1/responses"),Duration.ofSeconds(2));
            var result=client.complete("test instructions","test input",AgentPlanner.schema());
            check(result.text().equals("transport response") && result.inputTokens()==7,"Responses output and token usage parsed");
            check(auth.get().equals("Bearer fake-local-test-key"),"bearer authorization sent");
            check(!captured.get().get("store").getAsBoolean() && captured.get().get("model").getAsString().equals("test-model"),"request model/storage settings");
            check(captured.get().getAsJsonObject("text").getAsJsonObject("format").get("strict").getAsBoolean(),"strict plan schema sent");
            client.complete("test","test",null);
            check(!captured.get().has("text"),"narrative request uses plain text");
            status.set(401);body.set("fake-local-test-key must never be displayed");
            try{client.complete("test","test",null);throw new AssertionError("HTTP error accepted");}
            catch(IOException expected){check(expected.getMessage().contains("401") && !expected.getMessage().contains("fake-local-test-key"),"HTTP errors redact raw body");}
            status.set(200);body.set("{\"status\":\"incomplete\"}");
            try{client.complete("test","test",null);throw new AssertionError("incomplete accepted");}
            catch(IOException expected){check(expected.getMessage().contains("did not complete"),"incomplete output rejected");}
            body.set("{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\",\"refusal\":\"no\"}]}]}");
            try{client.complete("test","test",null);throw new AssertionError("refusal accepted");}
            catch(IOException expected){check(expected.getMessage().contains("declined"),"refusal recognized");}
            body.set("not JSON");
            try{client.complete("test","test",null);throw new AssertionError("malformed accepted");}
            catch(IOException expected){check(expected.getMessage().contains("Invalid LLM response"),"malformed response handled");}
        } finally {server.stop(0);}
    }
    private static String successBody(String text) {
        return Json.write(Map.of("status","completed","output",List.of(Map.of("type","message","content",List.of(Map.of("type","output_text","text",text)))),
                "usage",Map.of("input_tokens",7,"output_tokens",3)));
    }
    private static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);checks++;
    }
}
