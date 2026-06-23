package io.subbu.ai.firedrill.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Jackson deserializer that handles both JSON arrays and plain strings.
 * LLMs sometimes return array values like ["Java","Spring"] for fields that expect
 * a comma-separated string like "Java, Spring". This deserializer converts either
 * form into a single comma-separated String.
 */
public class ArrayOrStringDeserializer extends StdDeserializer<String> {

    public ArrayOrStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            List<String> items = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                String text = p.getValueAsString();
                if (text != null && !text.isBlank()) {
                    items.add(text.trim());
                }
            }
            return String.join(", ", items);
        }
        // Plain string or null
        return p.getValueAsString();
    }
}
