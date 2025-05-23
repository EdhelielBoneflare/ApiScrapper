package me.gruzdeva.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NoArgsConstructor;
import me.gruzdeva.config.ConfigManager;

import java.util.Random;

@NoArgsConstructor
public class NYTimesService implements ApiClient {
    private static final String API_KEY = ConfigManager.getProperty("nytimes.api.key");
    private static final String URL_BASE = ConfigManager.getProperty("nytimes.url.base");
    private static final String URL_MOST_POPULAR = ConfigManager.getProperty("nytimes.url.most_popular");

    private static final Integer[] TIME_PERIODS = {1}; //can 7 and 30, but the results are unnecessarily large

    private final Random random = new Random();

    @Override
    public String getServiceName() {
        return ApiClient.SERVICE_NYTIMES;
    }

    @Override
    public String fetchData() throws Exception {
        logger.info("Fetching data from {} service", getServiceName());
        int timePeriod = TIME_PERIODS[random.nextInt(TIME_PERIODS.length)];
        String result = null;

        JsonNode resultNode = fetchMostViewedArticles(timePeriod);
        try {
            result = objectMapper.writeValueAsString(resultNode);
        } catch (JsonProcessingException e) {
            logger.error("ErrNYT001. Error parsing data from {}: {}", getServiceName(), e.getMessage());
            throw new IllegalArgumentException("ErrNYT001.", e);
        }
        return result;
    }

    public static JsonNode fetchMostViewedArticles(int timePeriod) throws Exception{
        String url = URL_BASE + URL_MOST_POPULAR + "/viewed/" + timePeriod + ".json?api-key=" + API_KEY;
        return ApiClient.callApi(url);
    };

}
