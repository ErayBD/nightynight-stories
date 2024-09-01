package com.erayberkdalkiran.speechtotext;

import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageToVideo {
    private static final String AUTH_KEY = "YOUR_aivideoapi_API_KEY";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Logger LOGGER = Logger.getLogger(ImageToVideo.class.getName());

    public static JSONObject imageToVideoPOST(String imageUrl, String prompt) throws IOException, JSONException {
        String url = "https://api.aivideoapi.com/runway/generate/imageDescription";

        JSONObject payload = new JSONObject();
        payload.put("text_prompt", prompt);
        payload.put("model", "gen3");
        payload.put("image_as_end_frame", false);
        payload.put("width", 1280);  // gen3 icin sabit, 1280
        payload.put("height", 768);  // gen3 icin sabit, 768
        payload.put("img_prompt", imageUrl);
        payload.put("motion", 5);
        payload.put("seed", 0);
        payload.put("upscale", true); // gen3 icin sabit, false
        payload.put("interpolate", true);
        payload.put("callback_url", "");
        payload.put("time", 5);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(authorization("POST", AUTH_KEY))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.log(Level.WARNING, "Request failed with code: " + response.code());
                return null;
            }

            String responseBody = response.body().string();
            LOGGER.log(Level.INFO, "Response: " + responseBody);
            return new JSONObject(responseBody);
        }
    }

    public static String imageToVideoGET(String uuid) throws IOException, JSONException {
        String url = "https://api.aivideoapi.com/status?uuid=" + uuid;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(authorization("GET", AUTH_KEY))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.log(Level.WARNING, "Request failed with code: " + response.code());
                return null;
            }

            String responseBody = response.body().string();
            LOGGER.log(Level.INFO, "Response: " + responseBody);
            JSONObject jsonResponse = new JSONObject(responseBody);

            // Check if the video is ready
            if (jsonResponse.getString("status").equals("success")) {
                LOGGER.log(Level.INFO, "URL Status: Sent");
                return jsonResponse.getString("url");
            } else {
                LOGGER.log(Level.INFO, "URL Status: Null");
                return null; // Video not ready yet

            }
        }
    }

    public static Headers authorization(String httpType, String key) {
        Headers.Builder headersBuilder = new Headers.Builder();
        headersBuilder.add("accept", "application/json");
        if ("POST".equals(httpType)) {
            headersBuilder.add("content-type", "application/json");
        }
        headersBuilder.add("Authorization", key);
        return headersBuilder.build();
    }
}

