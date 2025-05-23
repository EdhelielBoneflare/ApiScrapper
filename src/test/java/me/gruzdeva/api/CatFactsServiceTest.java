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

class CatFactsServiceTest {

    private CatFactsService service;
    private JsonNode mockJsonNode;

    @BeforeEach
    void setUp() {
        service = new CatFactsService();
        mockJsonNode = mock(JsonNode.class);
    }

    @Test
    void getServiceName_shouldReturnCorrectName() {
        assertEquals(ApiClient.SERVICE_CAT_FACTS, service.getServiceName());
    }

    @Test
    void fetchData_shouldReturnValidJson() throws Exception {
        String expectedJson = "{\"fact\":\"Cats sleep 16 hours a day.\"}";
        JsonNode mockResultNode = new ObjectMapper().readTree(expectedJson);

        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            // Mock the API call
            apiClientMock.when(() -> ApiClient.callApi(anyString())).thenReturn(mockResultNode);

            // Mock JSON serialization if it's using ApiClient.serializeToJson
            apiClientMock.when(() -> ApiClient.serializeToJson(any())).thenReturn(expectedJson);

            // Execute test
            String result = service.fetchData();

            // Assert
            assertNotNull(result);
            JsonNode resultNode = new ObjectMapper().readTree(result);
            assertTrue(resultNode.has("fact"));

            // Verify API call was made with correct URL
            apiClientMock.verify(() -> ApiClient.callApi(contains("/fact")));
        }
    }

    @Test
    void fetchData_whenJsonProcessingException_shouldThrowIllegalArgumentException() throws Exception {
        // Instead of modifying the objectMapper directly, we'll mock the ApiClient's static methods
        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.callApi(anyString()))
                    .thenReturn(mockJsonNode);

            // mock the entire JSON serialization process
            apiClientMock.when(() -> ApiClient.serializeToJson(any(JsonNode.class)))
                    .thenThrow(new JsonProcessingException("JSON error") {});

            Exception exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.fetchData()
            );

            assertEquals("ErrCatFact001.", exception.getMessage());
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
}