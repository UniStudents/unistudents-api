package com.unistudents.api.common;

import gr.unistudents.services.student.components.Options;
import gr.unistudents.services.student.exceptions.NotAuthorizedException;
import gr.unistudents.services.student.exceptions.NotReachableException;
import gr.unistudents.services.student.exceptions.ParserException;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {

    public enum Type {
        STUDENT("student-api"),
        PARSER("student-api"),
        ELEARNING("elearning-api"),
        ;

        public String name;

        Type(String name) {
            this.name = name;
        }
    }

    private static JSONArray getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return new JSONArray(stringWriter.toString().replace("\t", "").split("\n"));
    }

    private static String getSeverity(Exception e) {
        if (e == null) return "info";

        if (e instanceof NotReachableException
                || e instanceof NotAuthorizedException
                || e instanceof gr.unistudents.services.elearning.exceptions.NotReachableException
                || e instanceof gr.unistudents.services.elearning.exceptions.NotAuthorizedException
        ) return "warning";

        return "error";
    }

    public static void log(Type type, Options opts, Exception exception, Throwable th, long timestamp) {
        OkHttpClient client = new OkHttpClient.Builder().build();

        JSONObject payload = new JSONObject()
                .put("type", type.name)
                .put("severity", getSeverity(exception))
                .put("metadata", new JSONObject()
                        .put("username", opts.username)
                        .put("university", opts.university)
                        .put("system", opts.system)
                        .put("duration", (System.currentTimeMillis() - timestamp) / 1000)
                        .put("device", new JSONObject().put("user_agent", opts.userAgent))
                        .put("other", new JSONObject())
                );

        if (exception != null) {
            payload.put("error", new JSONObject()
                    .put("name", exception.getClass().getName())
                    .put("message", exception.getMessage())
                    .put("stack", getStackTraceAsString(exception))
            );

            if (th != null) {
                payload.getJSONObject("metadata").getJSONObject("other")
                        .put("message", th.getMessage())
                        .put("stack", getStackTraceAsString(th));
            }

            if (exception instanceof ParserException) {
                payload.getJSONObject("metadata").getJSONObject("other")
                        .put("documents", ((ParserException) exception).document);
            }
        }

        if (type == Type.PARSER) {
            payload.getJSONObject("metadata").getJSONObject("other").put("is_parser", true);
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder()
                .url("https://logs.unistudents.app/logs")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) {
            }
        });
    }
}
