# ECS formatter for JBoss Log Manager

Formatter for JBoss Log Manager which produce ECS-compatible records. May be useful for applications which use JBoss Log Manager as primary logging framework (e.g. WildFly).

## Step 1: add dependency

Latest version: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/jboss-logmanager-ecs-formatter.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:jboss-logmanager-ecs-formatter)

Add a dependency to your application
```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>jboss-logmanager-ecs-formatter</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```
If you are not using a dependency management tool, like maven, you have to add both, `jboss-logmanager-ecs-formatter` and `ecs-logging-core` jars manually to the classpath.

## Step 2: use the `EcsFormatter`

Specify `co.elastic.logging.jboss.logmanager.EcsFormatter` as `formatter` for the required log handler. 

## Example (WildFly)

Create a jboss-logmanager-ecs-formatter` module

```shell script
$WILDFLY_HOME/bin/jboss-cli.sh -c 'module add --name=co.elastic.logging.jboss-logmanager-ecs-formatter --resources=jboss-logmanager-ecs-formatter-${ecs-logging-java.version}.jar:/tmp/ecs-logging-core-${ecs-logging-java.version}.jar --dependencies=org.jboss.logmanager'
```

Add the formatter to a handler in the logging subsystem

```shell script
$WILDFLY_HOME/bin/jboss-cli.sh -c '/subsystem=logging/custom-formatter=ECS:add(module=co.elastic.logging.jboss-logmanager-ecs-formatter, class=co.elastic.logging.jboss.logmanager.EcsFormatter, properties={serviceName=my-app}),\
                                   /subsystem=logging/console-handler=CONSOLE:write-attribute(name=named-formatter,value=ECS)'
```

## Layout Parameters

|Parameter name   |Type   |Default|Description|
|-----------------|-------|-------|-----------|
|serviceName      |String |       |Sets the `service.name` field so you can filter your logs by a particular service |
|eventDataset     |String |`${serviceName}.log`|Sets the `event.dataset` field used by the machine learning job of the Logs app to look for anomalies in the log rate. |
|stackTraceAsArray|boolean|`false`|Serializes the [`error.stack_trace`](https://www.elastic.co/guide/en/ecs/current/ecs-error.html) as a JSON array where each element is in a new line to improve readability. Note that this requires a slightly more complex [Filebeat configuration](../README.md#when-stacktraceasarray-is-enabled).|
|includeOrigin    |boolean|`false`|If `true`, adds the [`log.origin.file.name`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html), [`log.origin.file.line`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html) and [`log.origin.function`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html) fields. |

 
