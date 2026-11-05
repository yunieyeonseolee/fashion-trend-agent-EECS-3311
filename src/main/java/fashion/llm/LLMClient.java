package fashion.llm;

import com.google.gson.JsonObject;
import java.io.IOException;

public interface LLMClient {
    record Reply(String text, String model, long inputTokens, long outputTokens) {}
    /** schema is null for prose, otherwise a strict JSON Schema for the response. */
    Reply complete(String instructions, String input, JsonObject schema) throws IOException;
}
