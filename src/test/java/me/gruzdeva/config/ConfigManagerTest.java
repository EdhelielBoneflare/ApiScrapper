package me.gruzdeva.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @ParameterizedTest
    @ValueSource(strings = {"nytimes.url.base", "catfacts.url.base", "weather.url.base", "cycles"})
    void whenValidConfigKeyProvided_shouldReturnValue(String key) {
        String value = ConfigManager.getProperty(key);

        assertNotNull(value, "Existing property should return a value");
        assertFalse(value.isEmpty(), "Property value should not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid.key", "nonexistent", "wrongproperty", ""})
    void whenInvalidConfigKeyProvided_shouldReturnNullForIncorrectKey(String key) {
        String value = ConfigManager.getProperty(key);
        assertNull(value, "Non-existent property should return null");
    }
}