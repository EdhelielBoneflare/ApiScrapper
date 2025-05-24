# API Integration Project

This project is a Java-based API integration service that allows users to fetch data from multiple public APIs concurrently and output the results in either JSON or CSV format.
### Note: 
This is a university project, so some code may not be seen as best practice due to the task requirements or time constraints.
Some of the improvements that could be made are:
1. Services can be written in separate files, to allow creating a valid output file that can be easily fetched.
It is not done, as the task required to create a single file with all the data.

## Features

- Concurrent API calls using a configurable thread pool
- Support for multiple services (NYTimes, CatFacts, WeatherStack (Called Weather))
- Configurable polling intervals
- Automatic application shutdown after a specified duration
- Output formatting in JSON or CSV

## Requirements

- Java 17 or higher
- Gradle

## Dependencies

- Apache HttpClient 5
- Jackson for JSON processing
- SLF4J and Logback for logging

## Usage

Build the project using Gradle:

```bash
./gradlew build
```

Run the application with the following command-line arguments:

```bash
java -jar api-integration.jar <threads> <timeout> <services> <format>
```

### Parameters:

- `<threads>`: Maximum number of concurrent threads (positive integer)
- `<timeout>`: Time interval between API calls in seconds (positive integer)
- `<services>`: Comma-separated list of services to query (NYTimes,CatFacts,Weather)
- `<format>`: Output format (json or csv)

### Example:

```bash
java -jar api-integration.jar 5 10 NYTimes,CatFacts json
```

This will:
- Use up to 5 concurrent threads
- Poll the APIs every 10 seconds
- Query the NYTimes and CatFacts services
- Output results in JSON format
- Automatically shut down after 3 cycles (as defined by the N constant)

## Architecture

- `Main`: Entry point that parses arguments and sets up the application
- `ApiClient`: Interface for all API service implementations
- `ApiTaskPooler`: Manages concurrent API task execution
- `DataProcessor`: Processes and formats API responses

## Configuration

The application will run for a fixed number of cycles (default: 3) before automatically shutting down. This can be configured by changing the `N` constant in the `Main` class.

## Output

Results are written to a file named "./result/output" with the appropriate extension (.json or .csv).