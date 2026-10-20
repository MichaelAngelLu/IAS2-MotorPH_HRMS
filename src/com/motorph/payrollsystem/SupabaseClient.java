package com.motorph.payrollsystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Thin wrapper over Supabase's HTTP APIs. Needs Java 11+ and the Gson jar.
 *
 * Config (never commit real values):
 *   env vars SUPABASE_URL and SUPABASE_ANON_KEY, or config/supabase.properties
 * ANON_KEY is the PUBLISHABLE key (sb_publishable_... or legacy anon).
 * The service_role / secret key must NEVER appear anywhere in this app.
 */
public final class SupabaseClient {

    public static class SupabaseException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public final int status;
        public SupabaseException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final String URL;
    private static final String ANON_KEY;

    static {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("config/supabase.properties"))) {
            p.load(in);
        } catch (IOException ignored) { /* fall back to environment variables */ }
        URL = pick("SUPABASE_URL", p).replaceAll("/+$", "");
        ANON_KEY = pick("SUPABASE_ANON_KEY", p);
    }

    private SupabaseClient() { }

    private static String pick(String key, Properties p) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = p.getProperty(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing configuration: " + key);
        return v.trim();
    }

    /**
     * @param method     GET, POST, PATCH, PUT or DELETE
     * @param path       e.g. "/rest/v1/employees?select=*"
     * @param body       JSON body or null
     * @param jwt        the signed-in user's access token, or null (login / public calls)
     * @param returnRows true to get the affected rows back (use for PATCH/DELETE so a
     *                   silent RLS denial shows up as an empty array instead of success)
     */
    public static JsonElement request(String method, String path, JsonElement body,
                                      String jwt, boolean returnRows) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(URL + path))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", ANON_KEY)
                .header("Content-Type", "application/json");
        if (jwt != null) b.header("Authorization", "Bearer " + jwt);
        if (returnRows) b.header("Prefer", "return=representation");
        b.method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body.toString()));

        try {
            HttpResponse<String> r = HTTP.send(b.build(), BodyHandlers.ofString());
            String text = r.body() == null ? "" : r.body().trim();
            JsonElement json = text.isEmpty() ? JsonNull.INSTANCE : JsonParser.parseString(text);
            if (r.statusCode() >= 300) {
                throw new SupabaseException(r.statusCode(), errorMessage(json, r.statusCode()));
            }
            return json;
        } catch (IOException e) {
            throw new SupabaseException(0, "Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SupabaseException(0, "Request interrupted");
        }
    }

    private static String errorMessage(JsonElement json, int status) {
        if (json != null && json.isJsonObject()) {
            JsonObject o = json.getAsJsonObject();
            for (String k : new String[] { "message", "msg", "error_description", "error" }) {
                if (o.has(k) && !o.get(k).isJsonNull()) return o.get(k).getAsString();
            }
        }
        return "HTTP " + status;
    }
}
