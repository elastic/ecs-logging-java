# Log4j ECS Layout
🚧

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
