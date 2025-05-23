package me.gruzdeva.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NYTimesServiceTest {

    private NYTimesService service;
    private JsonNode mockJsonNode;

    @BeforeEach
    void setUp() {
        service = new NYTimesService();
        mockJsonNode = mock(JsonNode.class);
    }

    @Test
    void getServiceName_shouldReturnCorrectName() {
        assertEquals(ApiClient.SERVICE_NYTIMES, service.getServiceName());
    }

    @Test
    void fetchMostViewedArticles_shouldCallApiWithCorrectUrl(){
        int timePeriod = 1;

        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.callApi(anyString()))
                    .thenReturn(mockJsonNode);

            NYTimesService.fetchMostViewedArticles(timePeriod);

            apiClientMock.verify(() -> ApiClient.callApi(contains("/viewed/" + timePeriod + ".json")));
            apiClientMock.verify(() -> ApiClient.callApi(contains("api-key=")));
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    void fetchData_shouldReturnValidJson() {
        try {
            String mockJson = "{\"results\":[{\"title\":\"Test Article\",\"abstract\":\"Test content\"}]}";
            JsonNode mockResultNode = new ObjectMapper().readTree(mockJson);


            try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
                // First mock the API call that happens inside fetchMostViewedArticles
                apiClientMock.when(() -> ApiClient.callApi(anyString())).thenReturn(mockResultNode);

                apiClientMock.when(() -> ApiClient.serializeToJson(any())).thenReturn(mockJson);

                String result = service.fetchData();

                assertNotNull(result);
                JsonNode resultNode = new ObjectMapper().readTree(result);
                assertTrue(resultNode.has("results"));

                // check structure
                assertTrue(resultNode.get("results").isArray());
                assertFalse(resultNode.get("results").isEmpty());
            }
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    void fetchData_whenApiCallFails_shouldPropagateException() throws Exception {
        String errorMessage = "API connection failed";

        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.callApi(anyString()))
                    .thenThrow(new Exception(errorMessage));

            Exception exception = assertThrows(
                    Exception.class,
                    () -> service.fetchData()
            );

            assertEquals(errorMessage, exception.getMessage());
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }
}