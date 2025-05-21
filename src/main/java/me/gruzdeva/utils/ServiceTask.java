package me.gruzdeva.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.gruzdeva.api.ApiClient;

@AllArgsConstructor
@Getter
public class ServiceTask {
    private final ApiClient apiClient;
    private final int timeout;
    private final DataProcessor dataProcessor;
}
