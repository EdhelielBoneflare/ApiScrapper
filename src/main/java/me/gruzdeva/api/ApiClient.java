package me.gruzdeva.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public interface ApiClient {
    Logger logger = LoggerFactory.getLogger(ApiClient.class);
    ObjectMapper objectMapper = new ObjectMapper();

    String SERVICE_NYTIMES = "NYTimes";
    String SERVICE_CAT_FACTS = "CatFacts";
    String SERVICE_WEATHER = "Weather";
    Set<String> SERVICES = Set.of(SERVICE_NYTIMES, SERVICE_CAT_FACTS, SERVICE_WEATHER);

    String fetchData() throws IOException;
    String getServiceName();

    static ApiClient getApiClient(String serviceName) {
        ApiClient apiClient = null;
        switch (serviceName) {
            case SERVICE_NYTIMES -> apiClient = new NYTimesService();
            case SERVICE_CAT_FACTS -> apiClient = new CatFactsService();
            case SERVICE_WEATHER -> apiClient = new WeatherService();
            // default case is covered by the args check
        }
        return apiClient;
    }

    static JsonNode callApi(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 200) {
                    throw new HttpException(url + " Failed to fetch data : " + response.getReasonPhrase());
                }
                String json = EntityUtils.toString(response.getEntity());
                return objectMapper.readTree(json);
            } catch (HttpException e) {
                logger.error("{} Received status code is not 200: {}", url, e.getMessage());
            }
        } catch (IOException e) {
            logger.error("{} Error connecting to  API: {}", url, e.getMessage());
        }
        return null;
    }
}
