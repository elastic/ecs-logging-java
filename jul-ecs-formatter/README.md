# ECS formatter for JUL

Formatter for JUL (java.util.logging) which produce ECS-compatible records. May be useful for applications which use JUL as primary logging framework (e.g. Apache Tomcat).

## Step 1: add dependency

Latest version: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/jul-ecs-formatter.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:jul-ecs-formatter)

Add a dependency to your application
```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>jul-ecs-formatter</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```
If you are not using a dependency management tool, like maven, you have to add both, `jul-ecs-formatter` and `ecs-logging-core` jars manually to the classpath. For example to the `$CATALINA_HOME/lib` directory.

## Step 2: use the `EcsFormatter`

Specify `co.elastic.logging.jul.EcsFormatter` as `formatter` for the required log handler. 

## Example
For example `$CATALINA_HOME/conf/logging.properties`

```properties
java.util.logging.ConsoleHandler.level = FINE
java.util.logging.ConsoleHandler.formatter = co.elastic.logging.jul.EcsFormatter
co.elastic.logging.jul.EcsFormatter.serviceName=my-app
```

## Layout Parameters

|Parameter name   |Type   |Default|Description|
|-----------------|-------|-------|-----------|
|serviceName      |String |       |Sets the `service.name` field so you can filter your logs by a particular service |
|eventDataset     |String |`${serviceName}.log`|Sets the `event.dataset` field used by the machine learning job of the Logs app to look for anomalies in the log rate. |
|stackTraceAsArray|boolean|`false`|Serializes the [`error.stack_trace`](https://www.elastic.co/guide/en/ecs/current/ecs-error.html) as a JSON array where each element is in a new line to improve readability. Note that this requires a slightly more complex [Filebeat configuration](../README.md#when-stacktraceasarray-is-enabled).|
|includeOrigin    |boolean|`false`|If `true`, adds the [`log.origin.file.name`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html), [`log.origin.file.line`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html) and [`log.origin.function`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html) fields. Note that JUL does not stores line number and `log.origin.file.line` will have '1' value. |

 
