package datamind;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

public class Slack {

    private String webhookUrl;

    public Slack(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void sendMessage(String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.out.println("Erro: URL do Slack não foi configurada corretamente.");
            return;
        }

        try {
            URI uri = URI.create(webhookUrl);

            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("text", message);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Mensagem enviada com sucesso!");
            } else {
                System.out.println("Falha ao enviar a mensagem: " + response.statusCode());
                System.out.println("Corpo da resposta: " + response.body());
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: URL do Slack inválida.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erro ao enviar mensagem para o Slack.");
            e.printStackTrace();
        }
    }

    public String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return sdf.format(new Date());
    }
}
