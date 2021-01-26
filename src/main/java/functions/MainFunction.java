package functions;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.BufferedWriter;
import java.net.HttpURLConnection;
import java.util.logging.Logger;

public class MainFunction implements HttpFunction {
    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(MainFunction.class.getName());

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String name = null;
        Integer age = 0;
        String place = null;
        String phone = null;
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

        } catch (Exception e) {
            logger.severe("Error input: " + e.getMessage());
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            return;
        }

        BufferedWriter writer = response.getWriter();
        writer.write("{\"status\":\"success\"}");
    }


}
