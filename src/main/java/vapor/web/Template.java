package vapor.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// Minimal template: replaces {{name}} with an escaped value.
public class Template {
    private String text;
    private final Map<String, String> values = new HashMap<>();

    // ponytail: the file is read on every request; cache the text in a map if it gets slow
    public static Template load(String name) throws IOException {
        Template template = new Template();
        try (InputStream input = Template.class.getResourceAsStream("/templates/" + name + ".html")) {
            if (input == null) {
                throw new IOException("Template not found: " + name);
            }
            template.text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        return template;
    }

    public static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public Template set(String name, String value) {
        values.put(name, escape(value));
        return this;
    }

    public Template set(String name, Object value) {
        return set(name, String.valueOf(value));
    }

    // Only for HTML that was already rendered by another template.
    public void setHtml(String name, String html) {
        values.put(name, html);
    }

    public String render() {
        StringBuilder output = new StringBuilder();
        int position = 0;
        while (true) {
            int start = text.indexOf("{{", position);
            if (start < 0) {
                output.append(text.substring(position));
                return output.toString();
            }
            int end = text.indexOf("}}", start);
            output.append(text, position, start);
            output.append(resolve(text.substring(start + 2, end).trim()));
            position = end + 2;
        }
    }

    private String resolve(String name) {
        if (!values.containsKey(name)) {
            throw new IllegalStateException("Template value not set: " + name);
        }
        return values.get(name);
    }
}
