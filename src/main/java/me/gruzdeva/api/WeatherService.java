package me.gruzdeva.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NoArgsConstructor;
import java.util.Random;

@NoArgsConstructor
public class WeatherService implements ApiClient {
    private static final String API_KEY = "30753fa30d8bc2809d036b86e7651751";
    private static final String URL_BASE = "http://api.weatherstack.com";
    private static final String URL_CURRENT = "/current";

    private static final String[] POSSIBLE_CITIES = {
            "Tokyo",
            "Paris",
            "London",
            "Shanghai"
    };

    private final Random random = new Random();

    @Override
    public String getServiceName() {
        return ApiClient.SERVICE_WEATHER;
    }

    @Override
    public String fetchData() {
        logger.info("Fetching data from WeatherStack service");
        String query = POSSIBLE_CITIES[random.nextInt(POSSIBLE_CITIES.length)];
        String result = null;
        try {
            JsonNode resultNode = fetchCurrentWeather(query);
            result = objectMapper.writeValueAsString(resultNode);
        } catch (Exception e) {
            logger.error("Error fetching data from WeatherStack: {}", e.getMessage());
        }
        return result;
    }

    public static JsonNode fetchCurrentWeather(String query) {
        // StringBuilder instead of string concatenation for performance (because of multiple appends)
        StringBuilder url = new StringBuilder();
        url.append(URL_BASE)
            .append(URL_CURRENT)
            .append("?access_key=").append(API_KEY)
            .append("&query=").append(query);
        return ApiClient.callApi(url.toString());
    }
}
