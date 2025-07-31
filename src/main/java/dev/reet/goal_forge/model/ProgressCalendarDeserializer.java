package dev.reet.goal_forge.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProgressCalendarDeserializer extends JsonDeserializer<Map<String, Double>> {
    @Override
    public Map<String, Double> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Map<String, Double> result = new LinkedHashMap<>();
        JsonNode node = p.getCodec().readTree(p);
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                result.put(entry.getKey(), entry.getValue().asDouble());
            });
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                String date = item.has("date") ? item.get("date").asText() : null;
                double effort = item.has("effort") ? item.get("effort").asDouble() : 0.0;
                if (date != null) {
                    result.put(date, effort);
                }
            }
        } else if (!node.isNull()) {
            throw com.fasterxml.jackson.databind.JsonMappingException.from(p, "Expected object or array for progressCalendar");
        }
        return result;
    }
}
