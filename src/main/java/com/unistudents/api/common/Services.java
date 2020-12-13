package com.unistudents.api.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Services {

    private final static String NODEJS = "https://unistudents-nodejs.herokuapp.com";

    public static String[] jsUnFuck(String decodedString) {
        String json = "{\n" +
                "    \"data\": \"" + decodedString + "\"\n" +
                "}";

        try {
            URL url = new URL(NODEJS + "/eval");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.connect();

            OutputStream os = con.getOutputStream();
            os.write(json.getBytes("UTF-8"));
            os.close();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                con.disconnect();

                String responseString = response.toString();
                String[] keyValue = responseString.split(",");

                keyValue[0] = keyValue[0].split(":")[1].replace("\"", "");
                keyValue[1] = keyValue[1].split(":")[1].replace("\"", "").replace("}", "");

                return keyValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
