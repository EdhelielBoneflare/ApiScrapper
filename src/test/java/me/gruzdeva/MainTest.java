package me.gruzdeva;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @ParameterizedTest
    @MethodSource("validArgumentsProvider")
    void when_validArgumentsProvided_return_parsedArguments(String maxThreads, String timeout, String services, String format) {
        String[] args = {maxThreads, timeout, services, format};
        List<Object> result = Main.getArguments(args);

        assertEquals(Integer.parseInt(maxThreads), result.get(0));
        assertEquals(Integer.parseInt(timeout), result.get(1));
        assertArrayEquals(services.split(","), (String[])result.get(2));
        assertEquals(format, result.get(3));
    }

    static Stream<Arguments> validArgumentsProvider() {
        return Stream.of(
                Arguments.of("5", "10", "NYTimes,CatFacts,Weather", "json"),
                Arguments.of("5", "10", "NYTimes,CatFacts", "csv")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidArgumentsProvider")
    void when_invalidArgumentsProvided_throw_exception(String[] args, String expectedMessage) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Main.getArguments(args)
        );
        assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> invalidArgumentsProvider() {
        return Stream.of(
                // Wrong number of arguments
                Arguments.of(new String[]{"5", "10", "NYTimes"},
                        "Please provide correct arguments: maxThreads, timeout, services (comma-separated), and output format (json/csv).\nUsage: <threads> <timeout> <services> <format>\nExample: 5 10 NYTimes,CatFacts json"),

                // Invalid maxThreads
                Arguments.of(new String[]{"abc", "10", "NYTimes", "json"},
                        Main.INVALID_MAX_THREADS),
                Arguments.of(new String[]{"0", "10", "NYTimes", "json"},
                        Main.NEGATIVE_MAX_THREADS),
                Arguments.of(new String[]{"-5", "10", "NYTimes", "json"},
                        Main.NEGATIVE_MAX_THREADS),

                // Invalid timeout
                Arguments.of(new String[]{"5", "abc", "NYTimes", "json"},
                        Main.INVALID_TIMEOUT),
                Arguments.of(new String[]{"5", "0", "NYTimes", "json"},
                        Main.NEGATIVE_TIMEOUT),
                Arguments.of(new String[]{"5", "-10", "NYTimes", "json"},
                        Main.NEGATIVE_TIMEOUT),

                // Invalid services
                Arguments.of(new String[]{"5", "10", "", "json"},
                        Main.INVALID_SERVICES),
                Arguments.of(new String[]{"5", "10", "InvalidService", "json"},
                        Main.INVALID_SERVICES),

                // Invalid output format
                Arguments.of(new String[]{"5", "10", "NYTimes", "xml"},
                        Main.INVALID_OUT_FORM)
        );
    }

    @Test
    void when_trimmedInputsProvided_return_correctlyParsedArguments() {
        // Given: arguments with extra spaces
        String[] args = {" 5 ", " 10 ", " NYTimes,CatFacts ", " json "};

        // When
        List<Object> result = Main.getArguments(args);

        // Then
        assertEquals(5, result.get(0));
        assertEquals(10, result.get(1));
        assertArrayEquals(new String[]{"NYTimes", "CatFacts"}, (String[]) result.get(2));
        assertEquals("json", result.get(3));
    }

    @Test
    void when_differentCaseOutputFormat_return_normalizedFormat() {
        // Given: output format with mixed case
        String[] args = {"5", "10", "NYTimes", "JSON"};

        // When
        List<Object> result = Main.getArguments(args);

        // Then
        assertEquals("json", result.get(3));
    }
}