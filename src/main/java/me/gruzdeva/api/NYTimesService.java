package me.gruzdeva.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NoArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.Random;

@NoArgsConstructor
public class NYTimesService implements ApiClient {
    private static final String API_KEY = "AStqMUScrXkTUmrc2ebopeDuEnrImBnQ";
    private static final String URL_BASE = "https://api.nytimes.com";
    private static final String URL_MOST_POPULAR = "/svc/mostpopular/v2";

    private static final Integer[] TIME_PERIODS = {1}; //todo: add 7 and 30

    private final Random random = new Random();

    @Override
    public String getServiceName() {
        return ApiClient.SERVICE_NYTIMES;
    }

    @Override
    public String fetchData() {
        logger.info("Fetching data from NYTimes service");
        int timePeriod = TIME_PERIODS[random.nextInt(TIME_PERIODS.length)];
        String result = null;
        try {
            JsonNode resultNode = fetchMostViewedArticles(timePeriod);
            result = objectMapper.writeValueAsString(resultNode);
        } catch (Exception e) {
            logger.error("Error fetching data from NYTimes: {}", e.getMessage());
        }
        return result;
    }

    public static JsonNode fetchMostViewedArticles(int timePeriod) {
        String url = URL_BASE + URL_MOST_POPULAR + "/viewed/" + timePeriod + ".json?api-key=" + API_KEY;
        return ApiClient.callApi(url);
    };

}
