import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherApplication {
    public static void main(String[] args) {
        String apiKey = System.getenv("YANDEX_WEATHER_API_KEY");

        String lat = "59.93";
        String lon = "30.31";
        int limit = 3;

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("Задайте ключ в переменной окружения YANDEX_WEATHER_API_KEY");
            return;
        }

        try {
            String url = "https://api.weather.yandex.ru/v2/forecast?lat=" + lat
                    + "&lon=" + lon + "&limit=" + limit;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Yandex-Weather-Key", apiKey)
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String json = response.body();

            System.out.println("Полный JSON-ответ:");
            System.out.println(json);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            int currentTemperature = root.get("fact").get("temp").asInt();
            System.out.println("Текущая температура: " + currentTemperature + " °C");

            // cредняя дневная температура за первые limit дней
            int sum = 0;
            for (int i = 0; i < limit; i++) {
                int dayTemperature = root.get("forecasts").get(i)
                        .get("parts").get("day").get("temp_avg").asInt();
                sum += dayTemperature;
            }
            double averageTemperature = (double) sum / limit;
            System.out.println("Средняя дневная температура за " + limit
                    + " дня: " + averageTemperature + " °C");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}
