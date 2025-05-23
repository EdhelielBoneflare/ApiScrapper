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

    String fetchData() throws Exception;
    String getServiceName();

    static ApiClient getApiClient(String serviceName) {
        ApiClient apiClient = null;
        switch (serviceName) {
            case SERVICE_NYTIMES -> apiClient = new NYTimesService();
            case SERVICE_CAT_FACTS -> apiClient = new CatFactsService();
            case SERVICE_WEATHER -> apiClient = new WeatherService();
            // main is frontend. We do not trust frontend, so we check again
            default -> {
                logger.error("Unknown service name: {}", serviceName);
                throw new IllegalArgumentException("Unknown service name: " + serviceName);
            }
        }
        return apiClient;
    }

    static JsonNode callApi(String url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 200) {
                    throw new Exception(url + " Failed to fetch data : " + response.getReasonPhrase());
                }
                String json = EntityUtils.toString(response.getEntity());
                return objectMapper.readTree(json);
            } catch (Exception e) {
                logger.error("{} - ErrApiClient001 - Received status code is not 200: {}", url, e.getMessage());
                throw new Exception("ErrApiClient001", e);
            }
        } catch (IOException e) {
            logger.error("{} ErrApiClient002. Error connecting to API: {}", url, e.getMessage());
            throw new IOException("ErrApiClient002", e);
        }
    }
}
