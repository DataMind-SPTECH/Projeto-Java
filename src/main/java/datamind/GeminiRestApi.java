package datamind;

import okhttp3.*;
import com.google.gson.*;

public class GeminiRestApi {
    public static String obterRespostaDaIA(String prompt) throws InterruptedException {
        String apiKey = "AIzaSyDvkhMiz-PaFvnnaHHWgxjsh8tV4pylVik";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        // Corpo da requisição com o prompt dinâmico
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

        // Criação do Request
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        // Executando a requisição
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // Obtendo a resposta como string
                String responseBody = response.body().string();

                // Parseando a resposta JSON
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonObject firstCandidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                return content.getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString().trim();
            } else {
                // Tratando erro na resposta
                return "Erro na requisição: " + response.code();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao processar a requisição: " + e.getMessage();
        }
        Thread.sleep(5000);
    }

    public static void main(String[] args) {
        // Teste do metodo
        String prompt = "Gemini qual o limite de caracteres que posso passar no seu prompt?";
        String resposta = obterRespostaDaIA(prompt);

        // Exibindo a resposta da IA
        System.out.println("Resposta da IA: " + resposta);
    }
}
