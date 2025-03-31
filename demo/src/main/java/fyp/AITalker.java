package fyp;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AITalker {
    private static final Logger log = LoggerFactory.getLogger(AITalker.class);
    // Update link here
    private static String REMOTE_MODEL_URL = "https://fate-balloon-ea-helping.trycloudflare.com/generate";

    public AITalker() {}

    public static void setRemoteModelURL(String url) {
        REMOTE_MODEL_URL = url;
        log.info("Updated Remote Model URL: " + url);
    }

    public String analyzeCodeWithModel(String prompt) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.MINUTES)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(new RequestPayload(prompt));
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(REMOTE_MODEL_URL).post(body).build();

        int retries = 3;
        while (retries > 0) {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else if (response.code() == 503) {
                    log.warn("API unavailable. Retrying...");
                    retries--;
                    Thread.sleep(5000);
                } else {
                    log.error("API Error {}: {}", response.code(), response.body() != null ? response.body().string() : "Unknown error");
                    return "";
                }
            } catch (InterruptedException e) {
                log.error("Retry interrupted: ", e);
                Thread.currentThread().interrupt();
                return "";
            }
        }
        return "";
    }

    private static class RequestPayload {
        public String prompt;
        public RequestPayload(String prompt) {
            this.prompt = prompt;
        }
    }
}
