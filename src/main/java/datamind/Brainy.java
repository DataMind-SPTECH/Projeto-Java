package datamind;

import org.json.JSONObject;

import java.io.IOException;

public class Brainy {

    public static void main(String[] args) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("text", "Ola, sou Brainy, o robô desenvolvido pela Datamind😊");

        Slack.sendMessage(json);
    }
}
