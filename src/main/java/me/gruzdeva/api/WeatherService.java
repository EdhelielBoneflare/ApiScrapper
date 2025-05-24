package me.gruzdeva.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NoArgsConstructor;
import me.gruzdeva.config.ConfigManager;

import java.util.Random;

@NoArgsConstructor
public class WeatherService implements ApiClient {
    private static final String API_KEY = ConfigManager.getProperty("weather.api.key");
    private static final String URL_BASE = ConfigManager.getProperty("weather.url.base");
    private static final String URL_CURRENT = ConfigManager.getProperty("weather.url.current");

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
    public String fetchData() throws Exception {
        logger.info("Fetching data from WeatherStack service");
        String query = POSSIBLE_CITIES[random.nextInt(POSSIBLE_CITIES.length)];
        String result = null;

        JsonNode resultNode = fetchCurrentWeather(query);
        try {
            result = ApiClient.serializeToJson(resultNode);
        } catch (Exception e) {
            logger.error("ErrWeather001. Error parsing data from WeatherStack: {}", e.getMessage());
            throw new IllegalArgumentException("ErrWeather001.", e);
        }
        return result + "\n";
    }

    public static JsonNode fetchCurrentWeather(String query) throws Exception {
        // StringBuilder instead of string concatenation for performance (because of multiple appends)
        StringBuilder url = new StringBuilder();
        url.append(URL_BASE)
            .append(URL_CURRENT)
            .append("?access_key=").append(API_KEY)
            .append("&query=").append(query);
        return ApiClient.callApi(url.toString());
    }
}
