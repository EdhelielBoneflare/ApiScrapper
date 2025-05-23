package me.gruzdeva.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NoArgsConstructor;
import me.gruzdeva.config.ConfigManager;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

@NoArgsConstructor
public class CatFactsService implements ApiClient {
    private static final String API_URL = ConfigManager.getProperty("catfacts.url.base");

    @Override
    public String getServiceName() {
        return ApiClient.SERVICE_CAT_FACTS;
    }

    @Override
    public String fetchData() throws Exception {
        logger.info("Fetching data from CatFacts service");
        String result = null;

        JsonNode resultNode = fetchCatFact();
        try {
            result = objectMapper.writeValueAsString(resultNode);
        } catch (Exception e) {
            logger.error("ErrCatFact001. Error processing data from WeatherStack: {}", e.getMessage());
            throw new IllegalArgumentException("ErrCatFact001.", e);
        }
        return result;
    }

    private JsonNode fetchCatFact() throws Exception {
        String url = API_URL + "/fact";
        return ApiClient.callApi(url);
    }


}
