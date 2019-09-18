# ECS Logback Encoder

## Step 0: build the project
Check out the project and build with `mvn clean install`.
Once this project is available on maven central, this step is no longer needed.

## Step 1: add dependency

Add a dependency to your application
```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>logback-ecs-encoder</artifactId>
    <version>${java-ecs-logging.version}</version>
</dependency>
```

## Step 2: use the `EcsEncoder`

All you have to do is to use the `co.elastic.logging.logback.EcsEncoder` instead of the default pattern encoder
```xml
<encoder class="co.elastic.logging.logback.EcsEncoder">
    <serviceName>my-application</serviceName>
</encoder>
```

## Example `logback.xml` for Spring Boot applications
 
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/file-appender.xml"/>
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="co.elastic.logging.logback.EcsEncoder">
            <serviceName>my-application</serviceName>
        </encoder>
    </appender>
    <appender name="json-file" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder class="co.elastic.logging.logback.EcsEncoder">
            <serviceName>my-application</serviceName>
        </encoder>
        <file>${LOG_FILE}.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE}.json.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxFileSize>${LOG_FILE_MAX_SIZE:-10MB}</maxFileSize>
            <maxHistory>${LOG_FILE_MAX_HISTORY:-0}</maxHistory>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="console"/>
        <appender-ref ref="json-file"/>
        <!-- uncomment this if you also want to log in plain text        
        <appender-ref ref="FILE"/>
        -->
    </root>
</configuration>
```

## Encoder Parameters

|Parameter name   |Type   |Default|Description|
|-----------------|-------|-------|-----------|
|serviceName      |String |       |Sets the `service.name` field so you can filter your logs by a particular service |
|includeMarkers   |boolean|`false`|Log [Markers](https://www.slf4j.org/api/org/slf4j/Marker.html) as `tags` |
|stackTraceAsArray|boolean|`false`|Serializes the `error.stack_trace` as a JSON array where each element is in a new line to improve readability. Note that this requires a slightly more complex [Filebeat configuration](../README.md#when-stacktraceasarray-is-enabled).|
