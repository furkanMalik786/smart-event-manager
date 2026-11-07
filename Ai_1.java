package eventms;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** Minimal client for the Anthropic Messages API (no external JSON library needed). */
final class Ai {
    private Ai() { }

    /** @param history list of {role, content}, role = "user" or "assistant", ending with a user turn */
    static String ask(String system, List<String[]> history) throws Exception {
        String key = Config.get("ai.key").replaceAll("\\s", "");
        if (key.isEmpty()) throw new IllegalStateException("The AI module is not set up yet. Admin: open Settings and add the AI API key.");
        boolean or = key.startsWith("sk-or-") || Config.get("ai.url").contains("openrouter");
        String model = Config.get("ai.model", "claude-sonnet-5-5").trim();
        String url = Config.get("ai.url", "").trim();
        if (or) {
            // OpenRouter (OpenAI-style API). A Claude model name from the default setting is replaced by the free router.
            if (!model.contains("/")) model = "openrouter/free";
            if (url.isEmpty() || !url.contains("openrouter")) url = "https://openrouter.ai/api/v1/chat/completions";
        } else if (url.isEmpty()) {
            url = "https://api.anthropic.com/v1/messages";
        }
        StringBuilder msgs = new StringBuilder("[");
        if (or) msgs.append("{\"role\":\"system\",\"content\":").append(q(system)).append("},");
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) msgs.append(',');
            msgs.append("{\"role\":\"").append(history.get(i)[0]).append("\",\"content\":").append(q(history.get(i)[1])).append('}');
        }
        msgs.append(']');
        String body = or
                ? "{\"model\":" + q(model) + ",\"max_tokens\":1024,\"messages\":" + msgs + "}"
                : "{\"model\":" + q(model) + ",\"max_tokens\":1024,\"system\":" + q(system) + ",\"messages\":" + msgs + "}";
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(90))
                .header("content-type", "application/json");
        if (or) rb.header("Authorization", "Bearer " + key).header("X-Title", "Smart Event Manager");
        else rb.header("x-api-key", key).header("anthropic-version", "2023-06-01");
        HttpRequest req = rb.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() / 100 != 2) {
            String m = field(res.body(), "message");
            throw new IOException("AI request failed (" + res.statusCode() + "): " + (m != null ? m : res.body()));
        }
        String t = field(res.body(), or ? "content" : "text");
        return t == null || t.isBlank() ? "(The AI returned an empty answer.)" : t.trim();
    }

    private static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.append('"').toString();
    }

    /** Returns the first JSON string value stored under {@code key}, or null. */
    private static String field(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        while (i >= 0) {
            int j = i + key.length() + 2;
            while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
            if (j < json.length() && json.charAt(j) == ':') {
                j++;
                while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
                if (j < json.length() && json.charAt(j) == '"') return readString(json, j + 1);
            }
            i = json.indexOf("\"" + key + "\"", i + 1);
        }
        return null;
    }

    private static String readString(String s, int i) {
        StringBuilder sb = new StringBuilder();
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') break;
            if (c != '\\') { sb.append(c); continue; }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; }
                default -> sb.append(n);
            }
        }
        return sb.toString();
    }
}
