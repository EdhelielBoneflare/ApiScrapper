package me.gruzdeva.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.io.IOException;

public class DataProcessor {
    private final static Logger logger = LoggerFactory.getLogger(DataProcessor.class);

    private final static String OUT_DIR = "./result";
    private final static String OUT_FILE = "output";
    private final static String OUT_FILE_NAME = OUT_DIR + File.separator + OUT_FILE;
    private final static String FORMAT_JSON = "json";
    private final static String FORMAT_CSV = "csv";
    public final static Set<String> FORMATS = Set.of(FORMAT_JSON, FORMAT_CSV);

    private final String format;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public DataProcessor(String format) {
        this.format = format;
        deleteFileIfExists(Path.of(OUT_FILE_NAME + "." + format));
        createOutputDirectory();
    }

    private void deleteFileIfExists(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Existing file deleted: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("ErrDeleteFile01. Error deleting file {}: {}", filePath, e.getMessage());
            throw new RuntimeException("ErrDeleteFile01." + filePath, e);
        }
    }

    private void createOutputDirectory() {
        File dir = new File(OUT_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("Created output directory: {}", OUT_DIR);
            }
        }
    }

    public synchronized void process(String serviceName, String data) throws Exception {
        if (data == null || data.isEmpty()) {
            logger.warn("No data to process for service {}", serviceName);
            return;
        }

        try {
            logger.trace("Writing data for service {}: {}", serviceName, data);
            switch (format) {
                case FORMAT_JSON -> writeJson(serviceName, data);
                case FORMAT_CSV -> writeCsv(serviceName, data);
            }
        } catch (Exception e) {
            logger.error("ErrProcess001. Error processing data for service {}: {}", serviceName, e.getMessage());
            throw new Exception("ErrProcess001.");
        }
    }

    private void writeIntoFile(String data) throws IOException {
        try {
            Files.writeString(
                    Path.of(OUT_FILE_NAME + "." + format),
                    data,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("ErrJson001. Error writing data to file: {}", e.getMessage());
            throw new IOException("ErrJson001", e);
        }
    }

    private void writeJson(String serviceName, String data) throws IOException  {
        String prettyJson;
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            logger.error("ErrJson002. Error writing JSON data for service {}: {}", serviceName, e.getMessage());
            throw new IOException("ErrJson002", e);
        }

        writeIntoFile(prettyJson + System.lineSeparator());
    }

    private void writeCsv(String serviceName, String data) throws IOException {
        try {
            // Parse the JSON data
            JsonNode rootNode = objectMapper.readTree(data);
            StringBuilder csvData = new StringBuilder();

            // Special handling for NYTimes data
            if (serviceName.equals("NYTimes")) {
                processNYTimesData(rootNode, csvData);
            } else if (rootNode.isObject()) {
                processSingleObject(rootNode, csvData);
            } else if (rootNode.isArray() && !rootNode.isEmpty()) {
                processJsonArray(rootNode, csvData);
            } else {
                logger.warn("ErrCsv001. Empty or unsupported JSON structure for service {}", serviceName);
            }

            writeIntoFile(csvData.toString());
        } catch (JsonProcessingException e) {
            logger.error("ErrCsv002. Invalid JSON format for service {}: {}", serviceName, e.getMessage());
            throw new IOException("ErrCsv002", e);
        } catch (IOException e) {
            logger.error("ErrCsv003. Error writing CSV data for service {}: {}", serviceName, e.getMessage());
            throw new IOException("ErrCsv003", e);
        } catch (Exception e) {
            logger.error("ErrCsv004. Unexpected error converting JSON to CSV for service {}: {}",
                    serviceName, e.getMessage());
            throw new IOException("ErrCsv004", e);
        }
    }

    private void processNYTimesData(JsonNode rootNode, StringBuilder csvData) {
        // NYTimes most popular articles API typically has a "results" array
        JsonNode resultsNode = rootNode.get("results");

        if (resultsNode != null && resultsNode.isArray() && !resultsNode.isEmpty()) {
            // Define custom fields we want to extract
            String[] fieldNames = {"title", "abstract", "url", "published_date", "byline", "section"};

            // Create header row with our selected fields
            StringBuilder header = new StringBuilder();
            for (String field : fieldNames) {
                if (!header.isEmpty()) {
                    header.append(",");
                }
                header.append(escapeField(field));
            }
            csvData.append(header).append(System.lineSeparator());

            // Process each article
            for (JsonNode article : resultsNode) {
                StringBuilder row = new StringBuilder();

                for (String field : fieldNames) {
                    if (!row.isEmpty()) {
                        row.append(",");
                    }

                    JsonNode value = article.get(field);
                    row.append(formatNodeValue(value));
                }

                csvData.append(row).append(System.lineSeparator());
            }
        } else {
            logger.warn("ErrCsv005. NYTimes data doesn't contain expected 'results' array");
            // Fall back to default processing
            if (rootNode.isObject()) {
                processSingleObject(rootNode, csvData);
            } else if (rootNode.isArray() && !rootNode.isEmpty()) {
                processJsonArray(rootNode, csvData);
            }
        }
    }

    private void processSingleObject(JsonNode objectNode, StringBuilder csvData) {
        // Create header row and value row for single object
        StringBuilder headers = new StringBuilder();
        StringBuilder values = new StringBuilder();

        objectNode.fieldNames().forEachRemaining(field -> {
            if (!headers.isEmpty()) {
                headers.append(",");
                values.append(",");
            }
            headers.append(escapeField(field));
            JsonNode value = objectNode.get(field);

            values.append(formatNodeValue(value));
        });

        csvData.append(headers).append(System.lineSeparator());
        csvData.append(values).append(System.lineSeparator());
    }

    private void processJsonArray(JsonNode arrayNode, StringBuilder csvData) {
        JsonNode firstItem = arrayNode.get(0);

        // Create header row from first item's fields
        StringBuilder headers = new StringBuilder();
        firstItem.fieldNames().forEachRemaining(field -> {
            if (!headers.isEmpty()) {
                headers.append(",");
            }
            headers.append(escapeField(field));
        });
        csvData.append(headers).append(System.lineSeparator());

        // Process each array item as a row
        for (JsonNode item : arrayNode) {
            processArrayItem(item, firstItem, csvData);
        }
    }

    private void processArrayItem(JsonNode item, JsonNode templateNode, StringBuilder csvData) {
        StringBuilder row = new StringBuilder();

        templateNode.fieldNames().forEachRemaining(field -> {
            if (!row.isEmpty()) {
                row.append(",");
            }

            JsonNode value = item.get(field);
            row.append(formatNodeValue(value));
        });

        csvData.append(row).append(System.lineSeparator());
    }

    private String formatNodeValue(JsonNode value) {
        if (value == null) {
            return "";
        }

        // Handle nested objects/arrays by converting them to string
        if (value.isObject() || value.isArray()) {
            return escapeField(value.toString());
        } else {
            return escapeField(value.asText());
        }
    }

    private String escapeField(String value) {
        if (value == null) {
            return "";
        }

        // If the value contains comma, quote, or newline, wrap it in quotes and escape any quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
