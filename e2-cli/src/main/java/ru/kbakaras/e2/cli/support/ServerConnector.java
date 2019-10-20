package ru.kbakaras.e2.cli.support;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServerConnector {
    private static final Logger logger = Logger.getLogger(ServerConnector.class.getName());

    private String serverAddress;
    private final Charset charset = Charset.forName("UTF-8");

    public ServerConnector(String serverAddress) {
        this.serverAddress = serverAddress;
        //this.serverAddress = ApplicationSettings.getInstance().getDefaultHost();
        //ApplicationSettings.getInstance().addListener(event -> serverAddress = ApplicationSettings.getInstance().getDefaultHost());
    }

    public Map<String, Object> sendGet(String apiUrl, Map<String, String> headers) {
        return sendRequest("GET", apiUrl, headers, "", new HashMap<>());
    }

    public Map<String, Object> sendGet(String apiUrl, Map<String, String> headers, Map<String, List<String>> urlParams) {
        return sendRequest("GET", apiUrl, headers, "", urlParams);
    }

    public Map<String, Object> sendPost(String apiUrl, String data, Map<String, String> headers) {
        return sendRequest("POST", apiUrl, headers, data, new HashMap<>());
    }

    public Map<String, Object> sendPost(String apiUrl, String data, Map<String, String> headers, Map<String, List<String>> urlParams) {
        return sendRequest("POST", apiUrl, headers, data, urlParams);
    }

    private Map<String, Object> sendRequest(String method, String apiUrl, Map<String, String> headers, String data, Map<String, List<String>> urlParams) {
        StringBuilder response = new StringBuilder();
        StringBuilder responseResult = new StringBuilder();
        Map<String, Object> result = new HashMap<>();

        try {
            String requestParams = urlParams.keySet().stream().map(key -> {
                if (urlParams.get(key).size() == 1) {
                    return String.format("%s=%s", key, encodeValue(urlParams.get(key).get(0)));
                } else {
                    return urlParams.get(key).stream().map(val -> String.format("%s=%s", key, encodeValue(val))).collect(Collectors.joining("&"));
                }
            }).collect(Collectors.joining("&"));

            URL serverApiUrl = new URL(serverAddress + apiUrl + "?" + requestParams);

            HttpURLConnection connection = createConnection(method, headers, serverApiUrl);

            if (!data.trim().isEmpty()) {
                try (OutputStream output = connection.getOutputStream()) {
                    if (output != null) {
                        output.write(data.getBytes(charset));
                    }
                }
            }

            prepareResponseResult(responseResult, serverApiUrl, connection);
            result = parseResponse(response, connection);

        } catch (IOException e) {
            logger.log(Level.WARNING, String.format("Ошибка выполнения запроса%n%s", responseResult), e);
        }

        return result;
    }

    private Map<String, Object> parseResponse(StringBuilder answer, HttpURLConnection connection) throws IOException {
        Map<String, List<String>> headers = connection.getHeaderFields();
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("headers", headers);

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            try (InputStreamReader input = new InputStreamReader(connection.getErrorStream(), charset)) {
                int character;
                while (((character = input.read()) != -1)) {
                    answer.append((char) character);
                }
            }
        } else {
            String contentType = connection.getContentType();
            if (contentType.contains("application/json")) {
                try (InputStreamReader input = new InputStreamReader(connection.getInputStream(), charset)) {
                    int character;
                    while (((character = input.read()) != -1)) {
                        answer.append((char) character);
                    }
                }
            } else {
                throw new RuntimeException("Files not supported yet!");
            }
        }

        result.put("body", answer.toString());
        return result;
    }

    private HttpURLConnection createConnection(String method, Map<String, String> headers, URL connectionUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) connectionUrl.openConnection();
        if (!method.equals("GET")) {
            connection.setDoOutput(true);
            connection.setDoInput(true);
        }
        connection.setRequestMethod(method);

        (headers == null ? getHeaders() : headers).forEach(connection::setRequestProperty);

        return connection;
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=UTF-8");
        headers.put("Accept", "application/json");

        return headers;
    }

    private void prepareResponseResult(StringBuilder builder, URL url, HttpURLConnection connection) throws IOException {
        builder.append(String.format("Запрос: %s%n",          url.toExternalForm()));
        builder.append(String.format("Meтoд: %s%n",           connection.getRequestMethod()));
        builder.append(String.format("Koд ответа: %s%n",      connection.getResponseCode()));
        builder.append(String.format("Тип содержимого: %s%n", connection.getContentType()));
        builder.append(String.format("Oтвeт: %s%n",           connection.getResponseMessage()));
    }

    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}