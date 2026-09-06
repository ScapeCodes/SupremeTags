package net.noscape.project.supremetags.editorweb;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.noscape.project.supremetags.SupremeTags;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Set;

public class TagEditorSessionClient {
    private static final Gson GSON = new Gson();
    // Avoid HTTP/2 negotiation problems with hosting proxies; certificate validation stays enabled.
    private static final HttpClient HTTP = newClient(false);
    private static final HttpClient TLS_COMPAT = newClient(true);
    private final String apiUrl;
    private final String frontendUrl;

    public TagEditorSessionClient(SupremeTags plugin) {
        this(plugin.getConfig().getString("editor.api-url", ""),
                plugin.getConfig().getString("editor.frontend-url", ""));
    }

    TagEditorSessionClient(String apiUrl, String frontendUrl) {
        this.apiUrl = normalize(apiUrl);
        this.frontendUrl = normalize(frontendUrl);
    }

    private static HttpClient newClient(boolean tlsCompatibility) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20));
        if (tlsCompatibility) builder.sslParameters(new SSLParameters(new String[]{"TLSv1.2"}));
        return builder.build();
    }

    public boolean isConfigured() { return !apiUrl.isBlank(); }

    public CreateSessionResponse createSession(String payloadJson) throws IOException, InterruptedException {
        String body = "{\"payload\":" + payloadJson + ",\"frontendUrl\":" + GSON.toJson(frontendUrl) + "}";
        CreateSessionResponse session = parse(send(request("/sessions").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build()), CreateSessionResponse.class);
        if (session == null || blank(session.id) || blank(session.applyToken) || blank(session.editToken)) {
            throw new IOException("Session API response is missing session credentials.");
        }
        return session;
    }

    public CreateDumpResponse createDump(String payloadJson) throws IOException, InterruptedException {
        String body = "{\"payload\":" + payloadJson + ",\"frontendUrl\":" + GSON.toJson(frontendUrl) + "}";
        CreateDumpResponse dump = parse(send(request("/dumps").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build()), CreateDumpResponse.class);
        if (dump == null || blank(dump.dumpUrl)) throw new IOException("Dump API response is missing its link.");
        return dump;
    }

    public EditedResult fetchEditedResult(String sessionId, String applyToken, boolean force) throws IOException, InterruptedException {
        return fetchEditedResult(sessionId, applyToken, force, Set.of());
    }

    public EditedResult fetchEditedResult(String sessionId, String applyToken, boolean force, Set<String> appliedRevisions) throws IOException, InterruptedException {
        HttpRequest request = request("/sessions/" + encode(sessionId) + "/result" + (force ? "?force=true" : ""))
                .header("Authorization", "Bearer " + applyToken).GET().build();
        for (int attempt = 0; attempt < 6; attempt++) {
            HttpResponse<String> response = send(request);
            if (response.statusCode() != 409 || errorRequiresForce(response.body())) {
                PayloadResponse payload = parse(response, PayloadResponse.class);
                if (payload == null || payload.payload == null || blank(payload.revision)) {
                    throw new IOException("Session API result is missing its payload or revision. Update the editor backend before applying.");
                }
                // A different KV location can briefly return an older browser save. Never re-import an applied revision.
                if (!appliedRevisions.contains(payload.revision)) {
                    return new EditedResult(GSON.toJson(payload.payload), payload.revision, payload.forceRequired);
                }
            }
            if (attempt < 5) Thread.sleep(500L * (attempt + 1));
        }
        throw new IOException("A new editor save is not available yet. Click Apply Changes in the editor, wait for the save, then retry this command.");
    }

    public String fetchEditedPayload(String sessionId, String applyToken) throws IOException, InterruptedException {
        return fetchEditedResult(sessionId, applyToken, false).payloadJson;
    }

    public void markApplied(String sessionId, String applyToken, String revision) throws IOException, InterruptedException {
        String body = "{\"revision\":" + GSON.toJson(revision) + "}";
        HttpRequest request = request("/sessions/" + encode(sessionId) + "/applied")
                .header("Authorization", "Bearer " + applyToken).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        JsonObject result = parse(send(request), JsonObject.class);
        if (result == null || !result.has("forceRequired") || !result.get("forceRequired").getAsBoolean()) {
            throw new IOException("Session API did not confirm the successful apply.");
        }
    }

    public String buildEditorUrl(String sessionId, String editToken) {
        if (frontendUrl.isBlank() || blank(sessionId) || blank(editToken)) return "";
        String editorBase = frontendUrl.endsWith("/editor") ? frontendUrl : frontendUrl + "/editor";
        return editorBase + "/?token=" + encode(editToken);
    }

    private HttpRequest.Builder request(String path) throws IOException {
        try {
            URI base = URI.create(apiUrl);
            if (base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                    || !("https".equalsIgnoreCase(base.getScheme()) || "http".equalsIgnoreCase(base.getScheme()))) {
                throw new IllegalArgumentException();
            }
            return HttpRequest.newBuilder(URI.create(apiUrl + path)).timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json").header("User-Agent", "SupremeTags-Editor/3");
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid editor.api-url in config.yml; use the HTTP(S) API base URL.", exception);
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        HttpClient client = HTTP;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (attempt == 2 || !(status == 408 || status == 429 || status == 500 || status == 502 || status == 503 || status == 504)) {
                    return response;
                }
            } catch (IOException exception) {
                if (hasCertificateFailure(exception)) {
                    throw new IOException("Cannot verify the editor API TLS certificate. Check the server Java trust store and HTTPS proxy.", exception);
                }
                if (exception instanceof SSLHandshakeException) client = TLS_COMPAT;
                if (attempt == 2) {
                    String reason = exception instanceof SSLHandshakeException ? "TLS handshake failed"
                            : exception instanceof HttpTimeoutException ? "request timed out" : "connection failed";
                    throw new IOException("Editor API " + reason + " after 3 attempts. Check editor.api-url, outbound HTTPS access, and the server Java installation.", exception);
                }
            }
            Thread.sleep(500L * (attempt + 1));
        }
        throw new IOException("Editor API request failed.");
    }

    private static boolean hasCertificateFailure(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof CertificateException || cause instanceof java.security.cert.CertPathValidatorException
                    || cause instanceof java.security.cert.CertPathBuilderException) return true;
        }
        return false;
    }

    private static <T> T parse(HttpResponse<String> response, Class<T> type) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String detail = "";
            try {
                JsonObject error = GSON.fromJson(response.body(), JsonObject.class);
                if (error != null && error.has("error") && error.get("error").isJsonPrimitive()) {
                    detail = ": " + error.get("error").getAsString().replaceAll("[\\r\\n]", " ");
                    if (detail.length() > 250) detail = detail.substring(0, 250);
                }
            } catch (RuntimeException ignored) { /* Don't print proxy HTML or credentials to chat. */ }
            throw new IOException("Session API returned HTTP " + response.statusCode() + detail);
        }
        try {
            return GSON.fromJson(response.body(), type);
        } catch (JsonSyntaxException exception) {
            throw new IOException("Session API returned invalid JSON.", exception);
        }
    }

    private static boolean errorRequiresForce(String json) {
        try {
            JsonObject object = GSON.fromJson(json, JsonObject.class);
            return object != null && object.has("forceRequired") && object.get("forceRequired").getAsBoolean();
        } catch (RuntimeException ignored) { return false; }
    }

    private static String normalize(String value) { return value == null ? "" : value.trim().replaceAll("/+$", ""); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }

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

    public static class EditedResult {
        public final String payloadJson;
        public final String revision;
        public final boolean forceRequired;
        EditedResult(String payloadJson, String revision, boolean forceRequired) {
            this.payloadJson = payloadJson;
            this.revision = revision;
            this.forceRequired = forceRequired;
        }
    }

    private static class PayloadResponse {
        Object payload;
        String revision;
        boolean forceRequired;
    }
}
