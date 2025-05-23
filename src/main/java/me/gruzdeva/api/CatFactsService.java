package me.gruzdeva.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NoArgsConstructor;
import me.gruzdeva.config.ConfigManager;

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
            result = ApiClient.serializeToJson(resultNode);
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
