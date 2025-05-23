package me.gruzdeva.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataProcessorTest {

    @TempDir
    Path tempDir;

    private DataProcessor jsonProcessor;
    private DataProcessor csvProcessor;
    private Path jsonOutputPath;
    private Path csvOutputPath;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        String tempDirPath = tempDir.toAbsolutePath().toString();
        jsonProcessor = new DataProcessor("json", tempDirPath);
        csvProcessor = new DataProcessor("csv", tempDirPath);

        jsonOutputPath = tempDir.resolve("output.json");
        csvOutputPath = tempDir.resolve("output.csv");
    }

    @AfterEach
    void tearDown() {
        try {
            Files.deleteIfExists(jsonOutputPath);
            Files.deleteIfExists(csvOutputPath);
        } catch (IOException e) {
            // Log or handle exception
        }
    }

    @Test
    void process_withJsonFormat_shouldWriteJsonToFile() throws Exception {
        String serviceName = "TestService";
        String jsonData = "{\"name\":\"John\",\"age\":30}";

        jsonProcessor.process(serviceName, jsonData);

        assertTrue(Files.exists(jsonOutputPath));
        String fileContent = Files.readString(jsonOutputPath);
        assertNotNull(fileContent);
        assertTrue(fileContent.contains("\"name\" : \"John\""));
        assertTrue(fileContent.contains("\"age\" : 30"));
    }

    @Test
    void process_withCsvFormat_shouldWriteCsvToFile() throws Exception {
        String serviceName = "TestService";
        String jsonData = "{\"name\":\"John\",\"age\":30}";

        csvProcessor.process(serviceName, jsonData);

        assertTrue(Files.exists(csvOutputPath));
        List<String> lines = Files.readAllLines(csvOutputPath);
        assertEquals(2, lines.size());
        assertEquals("name,age", lines.get(0));
        assertEquals("John,30", lines.get(1));
    }

    @Test
    void process_withNYTimesData_shouldProcessSpecially() throws Exception {
        String serviceName = "NYTimes";
        String jsonData = "{\"results\":[{\"title\":\"Test Article\",\"abstract\":\"Test content\",\"url\":\"http://test.com\",\"published_date\":\"2023-01-01\",\"byline\":\"By Test Author\",\"section\":\"test\"}]}";

        csvProcessor.process(serviceName, jsonData);

        assertTrue(Files.exists(csvOutputPath));
        List<String> lines = Files.readAllLines(csvOutputPath);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("title,abstract,url,published_date,byline,section"));
        assertTrue(lines.get(1).contains("Test Article,Test content,http://test.com,2023-01-01,By Test Author,test"));
    }

    @Test
    void process_withJsonArray_shouldWriteAsCsvRows() throws Exception {
        String serviceName = "ArrayService";
        String jsonData = "[{\"id\":1,\"name\":\"Item 1\"},{\"id\":2,\"name\":\"Item 2\"}]";

        csvProcessor.process(serviceName, jsonData);

        assertTrue(Files.exists(csvOutputPath));
        List<String> lines = Files.readAllLines(csvOutputPath);
        assertEquals(3, lines.size());
        assertEquals("id,name", lines.get(0));
        assertEquals("1,Item 1", lines.get(1));
        assertEquals("2,Item 2", lines.get(2));
    }

    @Test
    void process_withNestedData_shouldEscapeNestedStructures() throws Exception {
        String serviceName = "NestedService";
        String jsonData = "{\"user\":{\"name\":\"John\",\"details\":{\"age\":30}},\"active\":true}";

        csvProcessor.process(serviceName, jsonData);

        assertTrue(Files.exists(csvOutputPath));
        List<String> lines = Files.readAllLines(csvOutputPath);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("user,active"));

        String dataLine = lines.get(1);
        assertTrue(dataLine.contains("John"));
        assertTrue(dataLine.contains("age"));
        assertTrue(dataLine.contains("30"));
        assertTrue(dataLine.endsWith("true"));
    }

    @Test
    void process_withCommasInData_shouldEscapeFields() throws Exception {
        String serviceName = "CommaService";
        String jsonData = "{\"description\":\"This, has, commas\",\"normal\":\"value\"}";

        csvProcessor.process(serviceName, jsonData);

        assertTrue(Files.exists(csvOutputPath));
        List<String> lines = Files.readAllLines(csvOutputPath);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("description,normal"));
        assertTrue(lines.get(1).contains("\"This, has, commas\",value"));
    }

    @Test
    void process_withEmptyData_shouldHandleGracefully() throws Exception {
        String serviceName = "EmptyService";
        String emptyData = "";

        jsonProcessor.process(serviceName, emptyData);

        // No file should be created or it should be empty
        if (Files.exists(jsonOutputPath)) {
            String fileContent = Files.readString(jsonOutputPath);
            assertEquals("", fileContent);
        }
    }

    @Test
    void process_withInvalidJson_shouldThrowException() {
        String serviceName = "InvalidService";
        String invalidJson = "{name:Invalid}";

        Exception exception = assertThrows(Exception.class, () ->
                jsonProcessor.process(serviceName, invalidJson)
        );

        assertTrue(exception.getMessage().contains("ErrProcess001"));
    }

    @Test
    void process_withNullData_shouldHandleGracefully() throws Exception {
        String serviceName = "NullService";

        jsonProcessor.process(serviceName, null);

        // No file should be created or it should be empty
        if (Files.exists(jsonOutputPath)) {
            String fileContent = Files.readString(jsonOutputPath);
            assertEquals("", fileContent);
        }
    }
}