package functions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class MainFunction implements HttpFunction {
    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(MainFunction.class.getName());
    private static final Firestore FIRESTORE = FirestoreOptions.getDefaultInstance().getService();
    private static final String PROJECT_ID = System.getenv("GOOGLE_CLOUD_PROJECT");

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        logger.info("Request received to update hire");
        String name;
        Integer age;
        String place;
        String phone;
        JsonObject requestJson = null;
        try {
            JsonElement requestParsed = gson.fromJson(request.getReader(), JsonElement.class);

            if (requestParsed != null && requestParsed.isJsonObject()) {
                logger.info("Started to parse request body");
                requestJson = requestParsed.getAsJsonObject();
            }

            if (requestJson != null || !requestJson.has("name") || !requestJson.has("age") || !requestJson.has("place") ||
                    !requestJson.has("phone")) {
                name = requestJson.get("name").getAsString();
                age = requestJson.get("age").getAsInt();
                place = requestJson.get("place").getAsString();
                phone = requestJson.get("phone").getAsString();
                logger.info("values parsed from JSON input");
            } else {
                logger.info("invalid input");
                response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }
            User user = new User(name, age, place);
            store(user);
            publish(user);
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
        logger.info("Stored in datastore " + writeResult.get().getUpdateTime());
    }

    private void publish(User user) throws IOException {
        String topicName = "hired";
        Publisher publisher = Publisher.newBuilder(
                ProjectTopicName.of(PROJECT_ID, topicName)).build();

        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String userJson = objectWriter.writeValueAsString(user);

        ByteString data = ByteString.copyFromUtf8(userJson);
        PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(data).build();

        try {
            publisher.publish(pubsubApiMessage).get();
            logger.info("Published message");
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Error publishing Pub/Sub message: " + e.getMessage());
        }
    }
}
