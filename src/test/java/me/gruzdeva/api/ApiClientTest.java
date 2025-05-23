package me.gruzdeva.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;


class ApiClientTest {

    @Test
    void getHttpClient_shouldReturnCloseableHttpClient() {
        CloseableHttpClient client = ApiClient.getHttpClient();

        assertNotNull(client);
        assertInstanceOf(CloseableHttpClient.class, client);
    }

    @Test
    void when_getApiClientWithValidService_return_correctImplementation() {
        ApiClient nyTimesClient = ApiClient.getApiClient(ApiClient.SERVICE_NYTIMES);
        ApiClient catFactsClient = ApiClient.getApiClient(ApiClient.SERVICE_CAT_FACTS);
        ApiClient weatherClient = ApiClient.getApiClient(ApiClient.SERVICE_WEATHER);

        assertInstanceOf(NYTimesService.class, nyTimesClient);
        assertInstanceOf(CatFactsService.class, catFactsClient);
        assertInstanceOf(WeatherService.class, weatherClient);

        assertEquals("NYTimes", nyTimesClient.getServiceName());
        assertEquals("CatFacts", catFactsClient.getServiceName());
        assertEquals("Weather", weatherClient.getServiceName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Unknown", "", "invalidService", "nytimes"})
    void when_getApiClientWithInvalidService_throw_IllegalArgumentException(String invalidService) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ApiClient.getApiClient(invalidService)
        );
        assertEquals("Unknown service name: " + invalidService, exception.getMessage());
    }

    @Test
    void when_callApiWithValidUrl_return_jsonNode() throws Exception {
        String testJson = "{\"key\":\"value\"}";
        String testUrl = "https://test.url";

        // Use Mockito's mockStatic for HttpClients with the factory method approach
        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
            CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
            HttpEntity mockEntity = mock(HttpEntity.class);

            apiClientMock.when(ApiClient::getHttpClient).thenReturn(mockClient);
            when(mockClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
            when(mockResponse.getCode()).thenReturn(200);
            when(mockResponse.getEntity()).thenReturn(mockEntity);

            try (MockedStatic<EntityUtils> entityUtilsMock = mockStatic(EntityUtils.class)) {
                entityUtilsMock.when(() -> EntityUtils.toString(any())).thenReturn(testJson);

                apiClientMock.when(() -> ApiClient.callApi(anyString())).thenCallRealMethod();

                JsonNode result = ApiClient.callApi(testUrl);

                assertNotNull(result);
                assertTrue(result.has("key"));
                assertEquals("value", result.get("key").asText());
            }
        }
    }

    @Test
    void when_callApiReturnsNon200_throw_exception() throws Exception {
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);

        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            apiClientMock.when(ApiClient::getHttpClient).thenReturn(mockClient);
            when(mockClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
            when(mockResponse.getCode()).thenReturn(404);
            when(mockResponse.getReasonPhrase()).thenReturn("Not Found");

            apiClientMock.when(() -> ApiClient.callApi(anyString())).thenCallRealMethod();

            String testUrl = "https://test.url";
            Exception exception = assertThrows(
                    Exception.class,
                    () -> ApiClient.callApi(testUrl)
            );

            assertEquals(testUrl + " Failed to fetch data : " + "Not Found", exception.getMessage());
            assertTrue(exception.getMessage().contains("Not Found"));
        }
    }

    @Test
    void when_callApiThrowsIOException_throw_wrappedException() throws Exception {
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);

        try (MockedStatic<ApiClient> apiClientMock = mockStatic(ApiClient.class)) {
            apiClientMock.when(ApiClient::getHttpClient).thenReturn(mockClient);
            when(mockClient.execute(any(HttpGet.class))).thenThrow(new IOException("Connection refused"));

            apiClientMock.when(() -> ApiClient.callApi(anyString())).thenCallRealMethod();

            String testUrl = "https://test.url";
            IOException exception = assertThrows(
                    IOException.class,
                    () -> ApiClient.callApi(testUrl)
            );

            assertEquals("ErrApiClient002", exception.getMessage());
            assertTrue(exception.getCause().getMessage().contains("Connection refused"));
        }
    }

    @Test
    void when_servicesSetContainsExpectedValues_return_true() {
        Set<String> expectedServices = Set.of("NYTimes", "CatFacts", "Weather");
        assertEquals(expectedServices, ApiClient.SERVICES);
    }
}