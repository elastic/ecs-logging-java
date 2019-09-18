# Log4j2 ECS Layout

## Step 0: build the project
Check out the project and build with `mvn clean install`.
Once this project is available on maven central, this step is no longer needed.

## Step 1: add dependency

Add a dependency to your application
```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>log4j2-ecs-layout</artifactId>
    <version>${java-ecs-logging.version}</version>
</dependency>
```

## Step 2: use the `EcsLayout`

Instead of the usual `<PatternLayout/>`, use `<EcsLayout serviceName="my-app"/>`.

If you want to include [Markers](https://logging.apache.org/log4j/2.0/manual/markers.html) as tags,
set the `includeMarkers` attribute to `true` (default: `false`).

```
<EcsLayout serviceName="my-app" includeMarkers="true"/>
```

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

## Structured logging

By leveraging log4j2's `MapMessage` or even by implementing your own `MultiformatMessage` with JSON support,
you can add additional fields to the resulting JSON.

Example:

```java
logger.info(new StringMapMessage().with("message", "foo").with("foo", "bar"));
``` 

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
 