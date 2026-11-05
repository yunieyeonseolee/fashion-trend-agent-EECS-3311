package fashion.llm;

import com.google.gson.*;
import java.time.LocalDate;

/** Central JSON configuration; local dates are ISO strings rather than reflected JDK fields. */
public final class Json {
    private Json() {}
    public static final Gson GSON = new GsonBuilder().serializeNulls()
            .setStrictness(Strictness.STRICT)
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (date, type, context) -> new JsonPrimitive(date.toString()))
            .create();
    public static String write(Object value) { return GSON.toJson(value); }
}
