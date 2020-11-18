# Log4j ECS Layout
🚧

The minimum required logback version is 1.2.4.

## Step 1: add dependency
Latest version: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/log4j-ecs-layout.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:log4j-ecs-layout)

Add a dependency to your application
```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>log4j-ecs-layout</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```

If you are not using a dependency management tool, like maven, you have to add both,
`log4j-ecs-layout` and `ecs-logging-core` jars manually to the classpath.
For example to the `$CATALINA_HOME/lib` directory.

## Step 2: use the `EcsLayout`

Instead of the usual layout class `"org.apache.log4j.PatternLayout"`, use `"co.elastic.logging.log4j.EcsLayout"`.

## Example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">
    <appender name="LogToConsole" class="org.apache.log4j.ConsoleAppender">
        <param name="Target" value="System.out"/>
            <layout class="co.elastic.logging.log4j.EcsLayout">
                <param name="serviceName" value="my-app"/>
            </layout>
    </appender>
    <appender name="LogToFile" class="org.apache.log4j.RollingFileAppender">
        <param name="File" value="logs/app.log"/>
            <layout class="co.elastic.logging.log4j.EcsLayout">
                <param name="serviceName" value="my-app"/>
            </layout>
    </appender>
    <root>
        <priority value="INFO"/>
        <appender-ref ref="LogToFile"/>
        <appender-ref ref="LogToConsole"/>
    </root>
</log4j:configuration>
```
