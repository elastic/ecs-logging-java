# Log4j2 ECS Layout

## Step 1: add dependency

Latest version: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/log4j2-ecs-layout.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:log4j2-ecs-layout)

Add a dependency to your application
```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>log4j2-ecs-layout</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```

If you are not using a dependency management tool, like maven, you have to add both,
`log4j2-ecs-layout` and `ecs-logging-core` jars manually to the classpath.
For example to the `$CATALINA_HOME/lib` directory.

## Step 2: use the `EcsLayout`

Instead of the usual `<PatternLayout/>`, use `<EcsLayout serviceName="my-app"/>`.

## Example
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="DEBUG">
    <Appenders>
        <Console name="LogToConsole" target="SYSTEM_OUT">
            <EcsLayout serviceName="my-app"/>
        </Console>
        <File name="LogToFile" fileName="logs/app.log">
            <EcsLayout serviceName="my-app"/>
        </File>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="LogToFile"/>
            <AppenderRef ref="LogToConsole"/>
        </Root>
    </Loggers>
</Configuration>
```

## Layout Parameters

|Parameter name   |Type   |Default|Description|
|-----------------|-------|-------|-----------|
|serviceName      |String |       |Sets the `service.name` field so you can filter your logs by a particular service |
|eventDataset     |String |`${serviceName}.log`|Sets the `event.dataset` field used by the machine learning job of the Logs app to look for anomalies in the log rate. |
|includeMarkers   |boolean|`false`|Log [Markers](https://logging.apache.org/log4j/2.0/manual/markers.html) as [`tags`](https://www.elastic.co/guide/en/ecs/current/ecs-base.html) |
|stackTraceAsArray|boolean|`false`|Serializes the [`error.stack_trace`](https://www.elastic.co/guide/en/ecs/current/ecs-error.html) as a JSON array where each element is in a new line to improve readability. Note that this requires a slightly more complex [Filebeat configuration](../README.md#when-stacktraceasarray-is-enabled).|
|includeOrigin    |boolean|`false`|If `true`, adds the [`log.origin.file.name`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html), [`log.origin.file.line`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html) and [`log.origin.function`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html) fields. Note that you also have to set `includeLocation="true"` on your loggers and appenders if you are using the async ones. |

To include any custom field in the output, use following syntax:

```xml
  <EcsLayout>
    <KeyValuePair key="additionalField1" value="constant value"/>
    <KeyValuePair key="additionalField2" value="$${ctx:key}"/>
  </EcsLayout>
```

Custom fields are included in the order they are declared. The values support [lookups](https://logging.apache.org/log4j/2.x/manual/lookups.html).

## Structured logging

By leveraging log4j2's `MapMessage` or even by implementing your own `MultiformatMessage` with JSON support,
you can add additional fields to the resulting JSON.

Example:

```java
logger.info(new StringMapMessage()
    .with("message", "Hello World!")
    .with("foo", "bar"));
``` 

If Jackson is on the classpath, you can also use an `ObjectMessage` to add a custom object the resulting JSON.

```java
logger.info(new ObjectMessage(myObject));
```

The `myObject` variable refers to a custom object which can be serialized by a Jackson `ObjectMapper`.

Using either will merge the object at the top-level (not nested under `message`) of the log event if it is a JSON object.
If it's a string, number boolean or an array, it will be converted into a string and added as the `message` property.
The conversion is done in order to avoid mapping conflicts as `message` is typed as a string in the Elasticsearch mapping.

### Tips
It's recommended to use existing [ECS fields](https://www.elastic.co/guide/en/ecs/current/ecs-field-reference.html).

If there is no appropriate ECS field,
consider prefixing your fields with `labels.`, as in `labels.foo`, for simple key/value pairs.
For nested structures consider prefixing with `custom.` to make sure you won't get conflicts if ECS later adds the same fields but with a different mapping.


### Gotchas

A common pitfall is how dots in field names are handled in Elasticsearch and how they affect the mapping.
In recent Elasticsearch versions, the following JSON structures would result in the same index mapping:

```json
{
  "foo.bar": "baz"
}
```

```json
{
  "foo": {
    "bar": "baz"
  }
}
```
The property `foo` would be mapped to the [Object datatype](https://www.elastic.co/guide/en/elasticsearch/reference/current/object.html).

This means that you can't index a document where `foo` would be a different datatype, as in shown in the following example:

```json
{
  "foo": "bar"
}
```

In that example, `foo` is a string.
Trying to index that document results in an error because the data type of `foo` can't be object and string at the same time.
 