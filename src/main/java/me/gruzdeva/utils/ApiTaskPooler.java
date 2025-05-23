package me.gruzdeva.utils;

import me.gruzdeva.api.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApiTaskPooler {
    private final static Logger logger = LoggerFactory.getLogger(ApiTaskPooler.class);

    private final int maxThreads;
    private final ExecutorService executor;
    private final BlockingQueue<ServiceTask> taskQueue;

    // prevents tasks from being re-added during application shutdown.
    private volatile AtomicBoolean isRunning = new AtomicBoolean(false);

    public ApiTaskPooler(int maxThreads) {
        this.maxThreads = maxThreads;
        this.executor = Executors.newFixedThreadPool(maxThreads);
        this.taskQueue = new LinkedBlockingQueue<>();
        isRunning.set(true);
    }

    public void shutdown() {
        logger.info("Shutting down API task pooler");
        isRunning.set(false);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate in time, forcing shutdown");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void addTask(ApiClient apiClient, int timeout, DataProcessor dataProcessor) {
        taskQueue.add(new ServiceTask(apiClient, timeout, dataProcessor));
    }

    public void start() {
        for (int i = 0; i < maxThreads; i++) {
            executor.submit(this::pollTask);
        }
    }

    private void pollTask() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ServiceTask task = taskQueue.take();
                executeTask(task);

                if (isRunning.get()) {
                    taskQueue.put(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error processing task: {}", e.getMessage());
            }
        }
    }

    private void executeTask(ServiceTask task) {
        try {
            ApiClient apiClient = task.getApiClient();

            try {
                String data = apiClient.fetchData();
                if (data != null) {
                    task.getDataProcessor().process(apiClient.getServiceName(), data);
                } else {
                    logger.warn("ErrPooler001. No data received from {}", apiClient.getServiceName());
                }
            } catch (Exception e) {
                logger.error("ErrPooler002. Error fetching data from {}: {}", apiClient.getServiceName(), e.getMessage());
                return;
            }

            try {
                Thread.sleep(task.getTimeout());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Task interrupted: {}", e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("If you see error code, please contact support (check logs). " + e.getMessage());
        }
    }

}
