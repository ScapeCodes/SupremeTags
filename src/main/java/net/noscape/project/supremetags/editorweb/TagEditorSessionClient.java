package net.noscape.project.supremetags.editorweb;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.noscape.project.supremetags.SupremeTags;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TagEditorSessionClient {

    private final SupremeTags plugin;
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TagEditorSessionClient(SupremeTags plugin) {
        this.plugin = plugin;
    }

    public boolean isConfigured() {
        return !getApiUrl().isBlank();
    }

    public CreateSessionResponse createSession(String payloadJson) throws IOException, InterruptedException {
        String body = "{\"payload\":" + payloadJson + ",\"frontendUrl\":" + gson.toJson(getFrontendUrl()) + "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getApiUrl() + "/sessions"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Session API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        try {
            return gson.fromJson(response.body(), CreateSessionResponse.class);
        } catch (JsonSyntaxException exception) {
            throw new IOException("Session API returned invalid JSON.", exception);
        }
    }

    public CreateDumpResponse createDump(String payloadJson) throws IOException, InterruptedException {
        String body = "{\"payload\":" + payloadJson + ",\"frontendUrl\":" + gson.toJson(getFrontendUrl()) + "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getApiUrl() + "/dumps"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Dump API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        try {
            return gson.fromJson(response.body(), CreateDumpResponse.class);
        } catch (JsonSyntaxException exception) {
            throw new IOException("Dump API returned invalid JSON.", exception);
        }
    }

    public String fetchEditedPayload(String sessionId, String applyToken) throws IOException, InterruptedException {
        HttpResponse<String> response = null;
        for (int attempt = 0; attempt < 6; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getApiUrl() + "/sessions/" + sessionId + "/result"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + applyToken)
                    .GET()
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 409) {
                break;
            }
            Thread.sleep(500L + (attempt * 500L));
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Session API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        try {
            PayloadResponse payloadResponse = gson.fromJson(response.body(), PayloadResponse.class);
            if (payloadResponse == null || payloadResponse.payload == null) {
                throw new IOException("Session API result did not include a payload.");
            }
            return gson.toJson(payloadResponse.payload);
        } catch (JsonSyntaxException exception) {
            throw new IOException("Session API returned invalid JSON.", exception);
        }
    }

    public String buildEditorUrl(String editToken) {
        String frontendUrl = getFrontendUrl();
        if (frontendUrl.isBlank() || editToken == null || editToken.isBlank()) {
            return "";
        }

        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        String editorBase = base.endsWith("/editor") ? base : base + "/editor";
        return editorBase + "/?token=" + editToken;
    }

    private String getApiUrl() {
        String apiUrl = plugin.getConfig().getString("editor.api-url", "");
        if (apiUrl == null) {
            return "";
        }
        return apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
    }

    private String getFrontendUrl() {
        String frontendUrl = plugin.getConfig().getString("editor.frontend-url", "");
        if (frontendUrl == null) {
            return "";
        }
        return frontendUrl.trim();
    }

    public static class CreateSessionResponse {
        public String id;
        public String editToken;
        public String applyToken;
        public String editorUrl;
        public String expiresAt;
    }

    public static class CreateDumpResponse {
        public String id;
        public String dumpUrl;
        public String expiresAt;
    }

    private static class PayloadResponse {
        Object payload;
    }
}
