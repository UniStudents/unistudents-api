package com.unistudents.api.common;

import com.unistudents.api.service.CryptoService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;

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

    public String uploadLogFile(Exception exception, String document, String university) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String text = sw.toString() + "\n\n======================\n\n" + document;
        System.out.println(text);
        CryptoService crypto = new CryptoService();
        text = crypto.encrypt(text);

        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            File tmpFile = File.createTempFile("_unistudents_bug_" + university.toUpperCase() + "_" + timestamp, ".txt");
            FileWriter writer = new FileWriter(tmpFile);
            writer.write(text);
            writer.close();

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addPart("file", new FileBody(tmpFile))
                    .build();

            HttpPost request = new HttpPost("https://file.io?expires=1d");
            request.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();
            HttpResponse response = client.execute(request);
            entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
