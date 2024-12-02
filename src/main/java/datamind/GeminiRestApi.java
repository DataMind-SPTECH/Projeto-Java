package datamind;

import okhttp3.*;
import com.google.gson.*;

public class GeminiRestApi {
    public static String obterRespostaDaIA(String prompt) {
        String apiKey = "AIzaSyDvkhMiz-PaFvnnaHHWgxjsh8tV4pylVik";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        String jsonBody = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(prompt);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();

                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonObject firstCandidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                return content.getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString().trim();
            } else {
                return "Erro na requisição: " + response.code();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao processar a requisição: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        String prompt = "Gemini qual o limite de caracteres que posso passar no seu prompt?";
        String resposta = obterRespostaDaIA(prompt);

        System.out.println("Resposta da IA: " + resposta);
    }
}
