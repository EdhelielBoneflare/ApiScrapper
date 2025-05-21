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
        createOutputFile();
    }

    private void deleteFileIfExists(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Existing file deleted: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("Error deleting file {}: {}", filePath, e.getMessage());
        }
    }

    private void createOutputFile() {
        File dir = new File(OUT_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("Created output directory: {}", OUT_DIR);
            }
        }
    }

    public synchronized void process(String serviceName, String data) {
        if (data == null || data.isEmpty()) {
            logger.warn("No data to process for service {}", serviceName);
            return;
        }

        try {
            logger.trace("Writing data for service {}: {}", serviceName, data);
            switch (format) {
                case FORMAT_JSON -> writeJson(serviceName, data);
                case FORMAT_CSV -> writeCsv(serviceName, data);
                default -> logger.warn("Unsupported format: {}", format);
            }
        } catch (Exception e) {
            logger.error("Error processing data for service {}: {}", serviceName, e.getMessage());
        }
    }

    private void writeIntoFile(String data) {
        try {
            Files.writeString(
                    Path.of(OUT_FILE_NAME + "." + format),
                    data,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("Error writing data to file: {}", e.getMessage());
        }
    }

    private void writeJson(String serviceName, String data) {
        String prettyJson;
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            logger.error("Error writing JSON data for service {}: {}", serviceName, e.getMessage());
            return;
        }

        writeIntoFile(prettyJson + System.lineSeparator());
    }

    private void writeCsv(String serviceName, String data) {
        try {
            // Parse the JSON data
            JsonNode rootNode = objectMapper.readTree(data);
            StringBuilder csvData = new StringBuilder();

            // Special handling for NYTimes response which has a "results" array
            if (rootNode.has("results") && rootNode.get("results").isArray()) {
                JsonNode resultsArray = rootNode.get("results");

                if (!resultsArray.isEmpty()) {
                    // Get fields from first article for headers
                    JsonNode firstArticle = resultsArray.get(0);
                    StringBuilder headers = new StringBuilder();

                    firstArticle.fieldNames().forEachRemaining(field -> {
                        if (!headers.isEmpty()) {
                            headers.append(",");
                        }
                        headers.append(escapeField(field));
                    });

                    csvData.append(headers).append(System.lineSeparator());

                    // Process each article
                    for (JsonNode article : resultsArray) {
                        StringBuilder row = new StringBuilder();

                        firstArticle.fieldNames().forEachRemaining(field -> {
                            if (!row.isEmpty()) {
                                row.append(",");
                            }
                            JsonNode value = article.get(field);
                            // Handle nested objects/arrays by converting them to string
                            if (value != null) {
                                if (value.isObject() || value.isArray()) {
                                    row.append(escapeField(value.toString()));
                                } else {
                                    row.append(escapeField(value.asText()));
                                }
                            } else {
                                row.append("");
                            }
                        });

                        csvData.append(row).append(System.lineSeparator());
                    }

                    writeIntoFile(csvData.toString());
                    return;
                }
            }

            // Fall back to standard processing for other JSON structures
            if (rootNode.isObject()) {
                // For a single object: create header row and value row
                StringBuilder headers = new StringBuilder();
                StringBuilder values = new StringBuilder();

                rootNode.fieldNames().forEachRemaining(field -> {
                    if (!headers.isEmpty()) {
                        headers.append(",");
                        values.append(",");
                    }
                    headers.append(escapeField(field));
                    JsonNode value = rootNode.get(field);

                    // Handle nested objects/arrays by converting them to string
                    if (value.isObject() || value.isArray()) {
                        values.append(escapeField(value.toString()));
                    } else {
                        values.append(escapeField(value.asText()));
                    }
                });

                // Combine both rows with a newline
                csvData.append(headers).append(System.lineSeparator());
                csvData.append(values).append(System.lineSeparator());

            } else if (rootNode.isArray()) {
                // Process normal array (not NYTimes-specific)
                // [existing array processing code]
                if (!rootNode.isEmpty()) {
                    JsonNode firstItem = rootNode.get(0);
                    StringBuilder headers = new StringBuilder();

                    firstItem.fieldNames().forEachRemaining(field -> {
                        if (!headers.isEmpty()) {
                            headers.append(",");
                        }
                        headers.append(escapeField(field));
                    });

                    csvData.append(headers).append(System.lineSeparator());

                    // Process each item
                    for (JsonNode item : rootNode) {
                        StringBuilder row = new StringBuilder();

                        firstItem.fieldNames().forEachRemaining(field -> {
                            if (!row.isEmpty()) {
                                row.append(",");
                            }
                            JsonNode value = item.get(field);

                            if (value != null) {
                                if (value.isObject() || value.isArray()) {
                                    row.append(escapeField(value.toString()));
                                } else {
                                    row.append(escapeField(value.asText()));
                                }
                            } else {
                                row.append("");
                            }
                        });

                        csvData.append(row).append(System.lineSeparator());
                    }
                }
            }

            writeIntoFile(csvData.toString());
        } catch (IOException e) {
            logger.error("Error writing CSV data for service {}: {}", serviceName, e.getMessage());
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
