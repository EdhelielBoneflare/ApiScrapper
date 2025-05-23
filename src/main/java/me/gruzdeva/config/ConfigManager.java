package me.gruzdeva.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Properties properties = new Properties();
    private static boolean initialized = false;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("ErrConfig001. Unable to find config.properties");
                throw new RuntimeException("ErrConfig001");
            }
            properties.load(input);
            initialized = true;
            logger.info("Configuration loaded successfully");
        } catch (IOException e) {
            logger.error("ErrConfig002. Failed to load configuration: {}", e.getMessage());
            throw new RuntimeException("ErrConfig002", e);
        }
    }

    public static String getProperty(String key) {
        if (!initialized) {
            logger.error("ErrConfig003. Configuration not initialized when requesting property: {}", key);
            throw new RuntimeException("ErrConfig003");
        }
        return properties.getProperty(key);
    }
}