package me.gruzdeva.utils;

import me.gruzdeva.api.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiTaskPoolerTest {

    private ApiTaskPooler taskPooler;
    private ApiClient mockApiClient;
    private DataProcessor mockDataProcessor;
    private final int TEST_THREADS = 2;
    private final int TEST_TIMEOUT = 100; // Short timeout for tests

    @BeforeEach
    void setUp() {
        taskPooler = new ApiTaskPooler(TEST_THREADS);
        mockApiClient = mock(ApiClient.class);
        mockDataProcessor = mock(DataProcessor.class);
    }

    @Test
    void addTask_shouldAcceptTask() {
        // No exceptions means the task was accepted
        assertDoesNotThrow(() -> taskPooler.addTask(mockApiClient, TEST_TIMEOUT, mockDataProcessor));
    }

    @Test
    void start_shouldBeginProcessingTasks() throws Exception {
        // Arrange
        String testData = "{\"test\":\"data\"}";
        when(mockApiClient.fetchData()).thenReturn(testData);
        when(mockApiClient.getServiceName()).thenReturn("TestService");

        // Add task and start pooler
        taskPooler.addTask(mockApiClient, TEST_TIMEOUT, mockDataProcessor);
        taskPooler.start();

        // Allow time for execution
        Thread.sleep(TEST_TIMEOUT * 2);

        // Verify task execution occurred
        verify(mockApiClient, atLeastOnce()).fetchData();
        verify(mockDataProcessor, atLeastOnce()).process(eq("TestService"), eq(testData));
    }

    @Test
    void executeTask_shouldHandleNullData() throws Exception {
        when(mockApiClient.fetchData()).thenReturn(null);
        when(mockApiClient.getServiceName()).thenReturn("TestService");

        taskPooler.addTask(mockApiClient, TEST_TIMEOUT, mockDataProcessor);
        taskPooler.start();

        // Allow time for execution
        Thread.sleep(TEST_TIMEOUT * 2);

        // Verify processor was never called with null data
        verify(mockApiClient, atLeastOnce()).fetchData();
        verify(mockDataProcessor, never()).process(anyString(), isNull());
    }

    @Test
    void executeTask_shouldHandleApiClientException() throws Exception {
        when(mockApiClient.fetchData()).thenThrow(new RuntimeException("Test exception"));
        when(mockApiClient.getServiceName()).thenReturn("TestService");

        taskPooler.addTask(mockApiClient, TEST_TIMEOUT, mockDataProcessor);
        taskPooler.start();

        Thread.sleep(TEST_TIMEOUT * 2);

        verify(mockApiClient, atLeastOnce()).fetchData();
        verify(mockDataProcessor, never()).process(anyString(), anyString());
    }

    @Test
    void shutdown_shouldStopProcessingTasks() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        when(mockApiClient.fetchData()).thenAnswer(inv -> {
            latch.countDown(); // Signal first execution
            return "data";
        });
        when(mockApiClient.getServiceName()).thenReturn("TestService");

        // Add task and start pooler
        taskPooler.addTask(mockApiClient, TEST_TIMEOUT * 10, mockDataProcessor);
        taskPooler.start();

        // Wait for first execution
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        taskPooler.shutdown();

        // Reset the mocks to track calls after shutdown
        reset(mockApiClient, mockDataProcessor);

        // Allow time to ensure no further executions
        Thread.sleep(TEST_TIMEOUT * 2);

        // Verify no more executions occurred after shutdown
        verifyNoInteractions(mockApiClient, mockDataProcessor);
    }

    @Test
    void multipleThreads_shouldProcessTasksConcurrently() throws Exception {
        // Arrange - create multiple clients
        ApiClient client1 = mock(ApiClient.class);
        ApiClient client2 = mock(ApiClient.class);
        when(client1.fetchData()).thenReturn("data1");
        when(client2.fetchData()).thenReturn("data2");
        when(client1.getServiceName()).thenReturn("Service1");
        when(client2.getServiceName()).thenReturn("Service2");

        // Add tasks and start pooler
        taskPooler.addTask(client1, TEST_TIMEOUT, mockDataProcessor);
        taskPooler.addTask(client2, TEST_TIMEOUT, mockDataProcessor);
        taskPooler.start();

        // Allow time for execution
        Thread.sleep(TEST_TIMEOUT * 3);

        // Verify both clients were used
        verify(client1, atLeastOnce()).fetchData();
        verify(client2, atLeastOnce()).fetchData();
        verify(mockDataProcessor, atLeastOnce()).process("Service1", "data1");
        verify(mockDataProcessor, atLeastOnce()).process("Service2", "data2");
    }
}