package functions;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class MainFunction implements HttpFunction {
    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(MainFunction.class.getName());
    private static final Firestore FIRESTORE = FirestoreOptions.getDefaultInstance().getService();

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String name;
        Integer age;
        String place;
        String phone;
        JsonObject requestJson = null;
        try {
            JsonElement requestParsed = gson.fromJson(request.getReader(), JsonElement.class);

            if (requestParsed != null && requestParsed.isJsonObject()) {
                requestJson = requestParsed.getAsJsonObject();
            }

            if (requestJson != null || !requestJson.has("name") || !requestJson.has("age") || !requestJson.has("place") ||
                    !requestJson.has("phone")) {
                name = requestJson.get("name").getAsString();
                age = requestJson.get("age").getAsInt();
                place = requestJson.get("place").getAsString();
                phone = requestJson.get("phone").getAsString();
            } else {
                response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }
            User user = new User(name, age, place, phone);
            store(user);

        } catch (Exception e) {
            logger.severe("Error input: " + e.getMessage());
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            return;
        }

        BufferedWriter writer = response.getWriter();
        writer.write("{\"status\":\"success\"}");
    }

    private void store(User user) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult =
                FIRESTORE.collection("hires")
                         .document(user.name)
                         .set(user);
        System.out.println("Update time : " + writeResult.get().getUpdateTime());
    }
}
