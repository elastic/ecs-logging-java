# ECS Logback Encoder

## Step 1: add dependency

Latest version: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/logback-ecs-encoder.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:logback-ecs-encoder)

Add a dependency to your application
```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>logback-ecs-encoder</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```

## Step 2: use the `EcsEncoder`

## Spring Boot applications

`src/main/resources/logback-spring.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_FILE" value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}/spring.log}"/>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml" />
    <include resource="org/springframework/boot/logging/logback/file-appender.xml" />
    <include resource="co/elastic/logging/logback/boot/ecs-file-appender.xml" />
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ECS_JSON_FILE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

You also need to configure the following properties to your `application.properties`:

```properties
spring.application.name=my-application
# for Spring Boot 2.2.x+
logging.file.name=/path/to/my-application.log
# for older Spring Boot versions
logging.file=/path/to/my-application.log
```

## Other applications 

All you have to do is to use the `co.elastic.logging.logback.EcsEncoder` instead of the default pattern encoder in `logback.xml`

```xml
<encoder class="co.elastic.logging.logback.EcsEncoder">
    <serviceName>my-application</serviceName>
</encoder>
```

## Encoder Parameters

|Parameter name   |Type   |Default|Description|
|-----------------|-------|-------|-----------|
|serviceName      |String |       |Sets the `service.name` field so you can filter your logs by a particular service |
|eventDataset     |String |`${serviceName}.log`|Sets the `event.dataset` field used by the machine learning job of the Logs app to look for anomalies in the log rate. |
|includeMarkers   |boolean|`false`|Log [Markers](https://logging.apache.org/log4j/2.0/manual/markers.html) as [`tags`](https://www.elastic.co/guide/en/ecs/current/ecs-base.html) |
|stackTraceAsArray|boolean|`false`|Serializes the [`error.stack_trace`](https://www.elastic.co/guide/en/ecs/current/ecs-error.html) as a JSON array where each element is in a new line to improve readability. Note that this requires a slightly more complex [Filebeat configuration](../README.md#when-stacktraceasarray-is-enabled).|
|includeOrigin    |boolean|`false`|If `true`, adds the [`log.origin.file.name`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html), [`log.origin.file.line`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html) and [`log.origin.function`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html) fields. Note that you also have to set `includeLocation="true"` on your loggers and appenders if you are using the async ones. |

To include any custom field in the output, use following syntax:

```xml
<additionalField>
    <key>foo</key>
    <value>bar</value>
</additionalField>
<additionalField>
    <key>baz</key>
    <value>qux</value>
</additionalField>
```
