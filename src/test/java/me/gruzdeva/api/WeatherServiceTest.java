package me.gruzdeva.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WeatherServiceTest {

    private WeatherService service;
    private JsonNode mockJsonNode;

    @BeforeEach
    void setUp() {
        service = new WeatherService();
        mockJsonNode = mock(JsonNode.class);
    }

    @Test
    void getServiceName_shouldReturnCorrectName() {
        assertEquals(ApiClient.SERVICE_WEATHER, service.getServiceName());
    }

    @Test
    void fetchData_shouldReturnValidJson() throws Exception {
        String mockJson = "{\"location\":{\"name\":\"London\"},\"current\":{\"temperature\":15}}";
        JsonNode mockResultNode = new ObjectMapper().readTree(mockJson);

        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.callApi(anyString()))
                    .thenReturn(mockResultNode);

            apiClientMock.when(() -> ApiClient.serializeToJson(any(JsonNode.class)))
                    .thenReturn(mockJson);

            String result = service.fetchData();

            assertNotNull(result);
            JsonNode resultNode = new ObjectMapper().readTree(result);
            assertTrue(resultNode.has("location"));
            assertTrue(resultNode.has("current"));
        }
    }

    @Test
    void fetchCurrentWeather_shouldCallApiWithCorrectUrl() throws Exception {
        String city = "Tokyo";

        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.callApi(anyString()))
                    .thenReturn(mockJsonNode);

            WeatherService.fetchCurrentWeather(city);

            apiClientMock.verify(() -> ApiClient.callApi(contains("query=" + city)));
            apiClientMock.verify(() -> ApiClient.callApi(contains("access_key=")));
        }
    }

    @Test
    void fetchData_whenJsonProcessingException_shouldThrowIllegalArgumentException() throws Exception {
        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            // Mock the API call
            apiClientMock.when(() -> ApiClient.callApi(anyString()))
                    .thenReturn(mockJsonNode);

            // Mock JSON serialization to throw exception
            apiClientMock.when(() -> ApiClient.serializeToJson(any(JsonNode.class)))
                    .thenThrow(new JsonProcessingException("JSON error") {});

            // Test & verify exception
            Exception exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.fetchData()
            );

            assertEquals("ErrWeather001.", exception.getMessage());
            assertNotNull(exception.getCause());
            assertInstanceOf(JsonProcessingException.class, exception.getCause());
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
        }
    }

    @Test
    void fetchCurrentWeather_usesCorrectUrlStructure() throws Exception {
        String city = "London";

        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.callApi(anyString())).thenReturn(mockJsonNode);

            WeatherService.fetchCurrentWeather(city);

            apiClientMock.verify(() -> ApiClient.callApi(matches(".*access_key=[^&]+&query=" + city)));
        }
    }
}