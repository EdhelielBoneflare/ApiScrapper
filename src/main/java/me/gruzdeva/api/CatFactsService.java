package me.gruzdeva.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NoArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

@NoArgsConstructor
public class CatFactsService implements ApiClient {
    private static final String API_URL = "https://catfact.ninja";

    @Override
    public String getServiceName() {
        return ApiClient.SERVICE_CAT_FACTS;
    }

    @Override
    public String fetchData() {
        logger.info("Fetching data from CatFacts service");
        String url = API_URL + "/fact";
        String result = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String json = EntityUtils.toString(response.getEntity());
                JsonNode node = objectMapper.readTree(json);
                result = objectMapper.writeValueAsString(node);
            }
        } catch (Exception e) {
            logger.error("Error fetching data from CatFacts: {}", e.getMessage());
        }
        return result;
    }


}
