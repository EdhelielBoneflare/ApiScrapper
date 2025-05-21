package me.gruzdeva;

import me.gruzdeva.api.ApiClient;
import me.gruzdeva.utils.ApiTaskPooler;
import me.gruzdeva.utils.DataProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private final static String INVALID_MAX_THREADS = "Invalid maximum number of threads. Please provide a valid integer.";
    private final static String INVALID_NUMBER_OF_ARGS = """
            Please provide correct arguments: \
            maxThreads, timeout, services (comma-separated), and output format (json/csv).
            Usage: <threads> <timeout> <services> <format>
            Example: 5 10 NYTimes,CatFacts json""";

    private final static String INVALID_TIMEOUT = "Invalid number format for timeout. Please provide a valid integer.";
    private final static String INVALID_SERVICES = "Invalid services provided.\nPlease provide a valid list of services: " + ApiClient.SERVICES +
            ".\n Example: 'service1,service2'";
    private final static String NO_SERVICES = "No services provided. Please provide a comma-separated list of services. Example: 'service1,service2'";
    private final static String INVALID_OUT_FORM = "Invalid output format. Valid formats: " + DataProcessor.FORMATS;


    /**
     *
     * @param args
     * 1st - maximum number of threads
     * 2nd - number of seconds between calling api
     * 3rd - list of services to be called
     * 4th - result format (json or csv)
     */
    public static void main(String[] args) {

        int maxThreads;
        int timeout;
        String[] services;
        String outFormat;
        try {
            List<Object> arguments = getArguments(args);
            maxThreads = (int) arguments.get(0);
            timeout = (int) arguments.get(1) * 1000; // convert seconds to milliseconds
            services = (String[]) arguments.get(2);
            outFormat = (String) arguments.get(3);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        DataProcessor dataProcessor = new DataProcessor(outFormat);
        ApiTaskPooler apiTaskPooler = new ApiTaskPooler(maxThreads);

        try {
            for (String service : services) {
                ApiClient apiClient = ApiClient.getApiClient(service);
                if (apiClient == null) {
                    logger.error("Err001. Invalid service name: " + service + " skipped during argument check.");
                    throw new IllegalArgumentException("Invalid service name: " + service + " skipped during argument check.");
                }
                apiTaskPooler.addTask(apiClient, timeout, dataProcessor);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }

        apiTaskPooler.start();

        int runDuration = timeout * 3; // Example: run for 3 cycles of the timeout
        logger.info("Application will shut down in {} milliseconds", runDuration);

        try {
            Thread shutdownThread = new Thread(() -> {
                try {
                    Thread.sleep(runDuration);
                    logger.info("Shutdown timer elapsed. Shutting down application...");
                    apiTaskPooler.shutdown();
                    // Some time for tasks to complete
                    Thread.sleep(2000);
                    System.exit(0);
                } catch (InterruptedException e) {
                    logger.error("Shutdown timer interrupted: {}", e.getMessage());
                }
            });
            shutdownThread.setDaemon(true);
            shutdownThread.start();

            // Wait for the shutdown thread to complete
            shutdownThread.join();
        } catch (InterruptedException e) {
            logger.error("Main thread interrupted: {}", e.getMessage());
        }
    }

    public static List<Object> getArguments(String[] args) throws IllegalArgumentException {
        int maxThreads;
        int timeout;
        String[] services;
        String outFormat;

        if (args.length != 4) {
            throw new IllegalArgumentException(INVALID_NUMBER_OF_ARGS);
        }

        try {
            maxThreads = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(INVALID_MAX_THREADS);
        }

        try {
            timeout = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(INVALID_TIMEOUT);
        }

        services = args[2].split(",");
        if (services.length == 0) {
            throw new IllegalArgumentException(NO_SERVICES);
        }
        if (!ApiClient.SERVICES.containsAll(Set.of(services))) {
            throw new IllegalArgumentException(INVALID_SERVICES);
        }

        outFormat = args[3].toLowerCase();
        if (!DataProcessor.FORMATS.contains(outFormat)) {
            throw new IllegalArgumentException(INVALID_OUT_FORM);
        }

        return List.of(maxThreads, timeout, services, outFormat);
    }
}