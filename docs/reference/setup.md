---
mapped_pages:
  - https://www.elastic.co/guide/en/ecs-logging/java/current/setup.html
navigation_title: Get started
---

# Get started with ECS Logging Java [setup]


## Step 1: Configure application logging [setup-step-1]

If you are using the Elastic APM Java agent, the easiest way to transform your logs into ECS-compatible JSON format is through the [`log_ecs_reformatting`](apm-agent-java://docs/reference/config-logging.md#config-log-ecs-reformatting) configuration option. By only setting this option, the Java agent will automatically import the correct ECS-logging library and configure your logging framework to use it instead (`OVERRIDE`/`REPLACE`) or in addition to (`SHADE`) your current configuration. No other changes required! Make sure to check out other [Logging configuration options](apm-agent-java://docs/reference/config-logging.md) to unlock the full potential of this option.

Otherwise, follow the steps below to manually apply ECS-formatting through your logging framework configuration. The following logging frameworks are supported:

* Logback (default for Spring Boot)
* Log4j2
* Log4j
* `java.util.logging` (JUL)
* JBoss Log Manager


### Add the dependency [_add_the_dependency]

:::::::{tab-set}

::::::{tab-item} Logback
The minimum required logback version is 1.1.

Download the latest version of Elastic logging: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/logback-ecs-encoder.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:logback-ecs-encoder)

Add a dependency to your application:

```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>logback-ecs-encoder</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```

::::{note}
If you are not using a dependency management tool, like maven, you have to manually add both `logback-ecs-encoder` and `ecs-logging-core` jars to the classpath. For example to the `$CATALINA_HOME/lib` directory. Other than that, there are no required dependencies.
::::
::::::

::::::{tab-item} Log4j2
The minimum required log4j2 version is 2.6.

Download the latest version of Elastic logging: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/log4j2-ecs-layout.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:log4j2-ecs-layout:)

Add a dependency to your application:

```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>log4j2-ecs-layout</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```

::::{note}
If you are not using a dependency management tool, like maven, you have to manually add both `log4j2-ecs-layout` and `ecs-logging-core` jars to the classpath. For example, to the `$CATALINA_HOME/lib` directory. Other than that, there are no required dependencies.
::::
::::::

::::::{tab-item} Log4j
The minimum required log4j version is 1.2.4.

Download the latest version of Elastic logging: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/log4j-ecs-layout.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:log4j-ecs-layout)

Add a dependency to your application:

```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>log4j-ecs-layout</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```

::::{note}
If you are not using a dependency management tool, like maven, you have to manually add both `log4j-ecs-layout` and `ecs-logging-core` jars to the classpath. For example, to the `$CATALINA_HOME/lib` directory. Other than that, there are no required dependencies.
::::
::::::

::::::{tab-item} JUL
A formatter for JUL (`java.util.logging`) which produces ECS-compatible records. Useful for applications that use JUL as primary logging framework, like Apache Tomcat.

Download the latest version of Elastic logging: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/jul-ecs-formatter.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:jul-ecs-formatter)

Add a dependency to your application:

```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>jul-ecs-formatter</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```

::::{note}
If you are not using a dependency management tool, like maven, you have to manually add both `jul-ecs-formatter` and `ecs-logging-core` jars to the classpath. For example, to the `$CATALINA_HOME/lib` directory. Other than that, there are no required dependencies.
::::
::::::

::::::{tab-item} JBoss
A formatter for JBoss Log Manager which produces ECS-compatible records. Useful for applications that use JBoss Log Manager as their primary logging framework, like WildFly.

Download the latest version of Elastic logging: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.logging/jboss-logmanager-ecs-formatter.svg)](https://search.maven.org/search?q=g:co.elastic.logging%20AND%20a:jboss-logmanager-ecs-formatter)

Add a dependency to your application:

```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>jboss-logmanager-ecs-formatter</artifactId>
    <version>${ecs-logging-java.version}</version>
</dependency>
```

::::{note}
If you are not using a dependency management tool, like maven, you have to manually add both `jboss-logmanager-ecs-formatter` and `ecs-logging-core` jars to the classpath. Other than that, there are no required dependencies.
::::
::::::

:::::::

### Use the ECS encoder/formatter/layout [_use_the_ecs_encoderformatterlayout]

:::::::{tab-set}

::::::{tab-item} Logback
**Spring Boot applications**

In `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_FILE" value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}/spring.log}"/>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml" />
    <include resource="org/springframework/boot/logging/logback/file-appender.xml" />
    <include resource="co/elastic/logging/logback/boot/ecs-console-appender.xml" />
    <include resource="co/elastic/logging/logback/boot/ecs-file-appender.xml" />
    <root level="INFO">
        <appender-ref ref="ECS_JSON_CONSOLE"/>
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

**Other applications**

All you have to do is to use the `co.elastic.logging.logback.EcsEncoder` instead of the default pattern encoder in `logback.xml`

```xml
<encoder class="co.elastic.logging.logback.EcsEncoder">
    <serviceName>my-application</serviceName>
    <serviceVersion>my-application-version</serviceVersion>
    <serviceEnvironment>my-application-environment</serviceEnvironment>
    <serviceNodeName>my-application-cluster-node</serviceNodeName>
</encoder>
```

**Encoder Parameters**

| Parameter name | Type | Default | Description |
| --- | --- | --- | --- |
| `serviceName` | String |  | Sets the `service.name` field so you can filter your logs by a particular service name |
| `serviceVersion` | String |  | Sets the `service.version` field so you can filter your logs by a particular service version |
| `serviceEnvironment` | String |  | Sets the `service.environment` field so you can filter your logs by a particular service environment |
| `serviceNodeName` | String |  | Sets the `service.node.name` field so you can filter your logs by a particular node of your clustered service |
| `eventDataset` | String | `${serviceName}` | Sets the `event.dataset` field used by the machine learning job of the Logs app to look for anomalies in the log rate. |
| `includeMarkers` | boolean | `false` | Log [Markers](https://logging.apache.org/log4j/2.0/manual/markers.md) as [`tags`](ecs://docs/reference/ecs-base.md) |
| `stackTraceAsArray` | boolean | `false` | Serializes the [`error.stack_trace`](ecs://docs/reference/ecs-error.md) as a JSON array where each element is in a new line to improve readability.Note that this requires a slightly more complex [Filebeat configuration](#setup-stack-trace-as-array). |
| `includeOrigin` | boolean | `false` | If `true`, adds the [`log.origin.file.name`](ecs://docs/reference/ecs-log.md), [`log.origin.file.line`](ecs://docs/reference/ecs-log.md) and [`log.origin.function`](ecs://docs/reference/ecs-log.md) fields. Note that you also have to set `<includeCallerData>true</includeCallerData>` on your appenders if you are using the async ones. |

To include any custom field in the output, use following syntax:

```xml
<additionalField>
    <key>key1</key>
    <value>value1</value>
</additionalField>
<additionalField>
    <key>key2</key>
    <value>value2</value>
</additionalField>
```
::::::

::::::{tab-item} Log4j2
Instead of the usual `<PatternLayout/>`, use `<EcsLayout serviceName="my-app"/>`. For example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="DEBUG">
    <Appenders>
        <Console name="LogToConsole" target="SYSTEM_OUT">
            <EcsLayout serviceName="my-app" serviceVersion="my-app-version" serviceEnvironment="my-app-environment" serviceNodeName="my-app-cluster-node"/>
        </Console>
        <File name="LogToFile" fileName="logs/app.log">
            <EcsLayout serviceName="my-app" serviceVersion="my-app-version" serviceEnvironment="my-app-environment" serviceNodeName="my-app-cluster-node"/>
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

**Layout Parameters**

| Parameter name | Type | Default | Description |
| --- | --- | --- | --- |
| `serviceName` | String |  | Sets the `service.name` field so you can filter your logs by a particular service name |
| `serviceVersion` | String |  | Sets the `service.version` field so you can filter your logs by a particular service version |
| `serviceEnvironment` | String |  | Sets the `service.environment` field so you can filter your logs by a particular service environment |
| `serviceNodeName` | String |  | Sets the `service.node.name` field so you can filter your logs by a particular node of your clustered service |
| `eventDataset` | String | `${serviceName}` | Sets the `event.dataset` field used by the machine learning job of the Logs app to look for anomalies in the log rate. |
| `includeMarkers` | boolean | `false` | Log [Markers](https://logging.apache.org/log4j/2.0/manual/markers.md) as [`tags`](ecs://docs/reference/ecs-base.md) |
| `stackTraceAsArray` | boolean | `false` | Serializes the [`error.stack_trace`](ecs://docs/reference/ecs-error.md) as a JSON array where each element is in a new line to improve readability. Note that this requires a slightly more complex [Filebeat configuration](#setup-stack-trace-as-array). |
| `includeOrigin` | boolean | `false` | If `true`, adds the [`log.origin.file.name`](ecs://docs/reference/ecs-log.md) fields. Note that you also have to set `includeLocation="true"` on your loggers and appenders if you are using the async ones. |

To include any custom field in the output, use following syntax:

```xml
  <EcsLayout>
    <KeyValuePair key="key1" value="constant value"/>
    <KeyValuePair key="key2" value="$${ctx:key}"/>
  </EcsLayout>
```

Custom fields are included in the order they are declared. The values support [lookups](https://logging.apache.org/log4j/2.x/manual/lookups.md). This means that the `KeyValuePair` setting can be utilized to dynamically set predefined fields as well:

```xml
<EcsLayout serviceName="myService">
  <KeyValuePair key="service.version" value="$${spring:project.version}"/>
  <KeyValuePair key="service.node.name" value="${env:HOSTNAME}"/>
</EcsLayout>
```

::::{note}
The log4j2 `EcsLayout` does not allocate any memory (unless the log event contains an `Exception`) to reduce GC pressure. This is achieved by manually serializing JSON so that no intermediate JSON or map representation of a log event is needed.
::::
::::::

::::::{tab-item} Log4j
Instead of the usual layout class `"org.apache.log4j.PatternLayout"`, use `"co.elastic.logging.log4j.EcsLayout"`. For example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">
    <appender name="LogToConsole" class="org.apache.log4j.ConsoleAppender">
        <param name="Target" value="System.out"/>
        <layout class="co.elastic.logging.log4j.EcsLayout">
            <param name="serviceName" value="my-app"/>
            <param name="serviceNodeName" value="my-app-cluster-node"/>
        </layout>
    </appender>
    <appender name="LogToFile" class="org.apache.log4j.RollingFileAppender">
        <param name="File" value="logs/app.log"/>
        <layout class="co.elastic.logging.log4j.EcsLayout">
            <param name="serviceName" value="my-app"/>
            <param name="serviceNodeName" value="my-app-cluster-node"/>
        </layout>
    </appender>
    <root>
        <priority value="INFO"/>
        <appender-ref ref="LogToFile"/>
        <appender-ref ref="LogToConsole"/>
    </root>
</log4j:configuration>
```

**Layout Parameters**

| Parameter name | Type | Default | Description |
| --- | --- | --- | --- |
| `serviceName` | String |  | Sets the `service.name` field so you can filter your logs by a particular service name |
| `serviceVersion` | String |  | Sets the `service.version` field so you can filter your logs by a particular service version |
| `serviceEnvironment` | String |  | Sets the `service.environment` field so you can filter your logs by a particular service environment |
| `serviceNodeName` | String |  | Sets the `service.node.name` field so you can filter your logs by a particular node of your clustered service |
| `eventDataset` | String | `${serviceName}` | Sets the `event.dataset` field used by the machine learning job of the Logs app to look for anomalies in the log rate. |
| `stackTraceAsArray` | boolean | `false` | Serializes the [`error.stack_trace`](ecs://docs/reference/ecs-error.md) as a JSON array where each element is in a new line to improve readability.Note that this requires a slightly more complex [Filebeat configuration](#setup-stack-trace-as-array). |
| `includeOrigin` | boolean | `false` | If `true`, adds the [`log.origin.file.name`](ecs://docs/reference/ecs-log.md) fields.Note that you also have to set `<param name="LocationInfo" value="true"/>` if you are using `AsyncAppender`. |

To include any custom field in the output, use following syntax:

```xml
<layout class="co.elastic.logging.log4j.EcsLayout">
   <param name="additionalField" value="key1=value1"/>
   <param name="additionalField" value="key2=value2"/>
</layout>
```

Custom fields are included in the order they are declared.
::::::

::::::{tab-item} JUL
Specify `co.elastic.logging.jul.EcsFormatter` as `formatter` for the required log handler.

For example, in `$CATALINA_HOME/conf/logging.properties`:

```properties
java.util.logging.ConsoleHandler.level = FINE
java.util.logging.ConsoleHandler.formatter = co.elastic.logging.jul.EcsFormatter
co.elastic.logging.jul.EcsFormatter.serviceName=my-app
co.elastic.logging.jul.EcsFormatter.serviceVersion=my-app-version
co.elastic.logging.jul.EcsFormatter.serviceEnvironment=my-app-environment
co.elastic.logging.jul.EcsFormatter.serviceNodeName=my-app-cluster-node
```

**Layout Parameters**

| Parameter name | Type | Default | Description |
| --- | --- | --- | --- |
| `serviceName` | String |  | Sets the `service.name` field so you can filter your logs by a particular service name |
| `serviceVersion` | String |  | Sets the `service.version` field so you can filter your logs by a particular service version |
| `serviceEnvironment` | String |  | Sets the `service.environment` field so you can filter your logs by a particular service environment |
| `serviceNodeName` | String |  | Sets the `service.node.name` field so you can filter your logs by a particular node of your clustered service |
| `eventDataset` | String | `${serviceName}` | Sets the `event.dataset` field used by the machine learning job of the Logs app to look for anomalies in the log rate. |
| `stackTraceAsArray` | boolean | `false` | Serializes the [`error.stack_trace`](ecs://docs/reference/ecs-error.md) as a JSON array where each element is in a new line to improve readability. Note that this requires a slightly more complex Filebeat configuration. |
| `includeOrigin` | boolean | `false` | If `true`, adds the [`log.origin.file.name`](ecs://docs/reference/ecs-log.md) fields. Note that JUL does not stores line number and `log.origin.file.line` will have *1* value. |
| `additionalFields` | String |  | Adds additional static fields to all log events. The fields are specified as comma-separated key-value pairs. Example: `co.elastic.logging.jul.EcsFormatter.additionalFields=key1=value1,key2=value2`. |
::::::

::::::{tab-item} JBoss
Specify `co.elastic.logging.jboss.logmanager.EcsFormatter` as `formatter` for the required log handler.

For example, with Wildfly, create a `jboss-logmanager-ecs-formatter` module:

```bash
$WILDFLY_HOME/bin/jboss-cli.sh -c 'module add --name=co.elastic.logging.jboss-logmanager-ecs-formatter --resources=jboss-logmanager-ecs-formatter-${ecs-logging-java.version}.jar:/tmp/ecs-logging-core-${ecs-logging-java.version}.jar --dependencies=org.jboss.logmanager'
```

Add the formatter to a handler in the logging subsystem:

```bash
$WILDFLY_HOME/bin/jboss-cli.sh -c '/subsystem=logging/custom-formatter=ECS:add(module=co.elastic.logging.jboss-logmanager-ecs-formatter,
class=co.elastic.logging.jboss.logmanager.EcsFormatter, properties={serviceName=my-app,serviceVersion=my-app-version,serviceEnvironment=my-app-environment,serviceNodeName=my-app-cluster-node}),\
                                   /subsystem=logging/console-handler=CONSOLE:write-attribute(name=named-formatter,value=ECS)'
```

**Layout Parameters**

| Parameter name | Type | Default | Description |
| --- | --- | --- | --- |
| `serviceName` | String |  | Sets the `service.name` field so you can filter your logs by a particular service name |
| `serviceVersion` | String |  | Sets the `service.version` field so you can filter your logs by a particular service version |
| `serviceEnvironment` | String |  | Sets the `service.environment` field so you can filter your logs by a particular service environment |
| `serviceNodeName` | String |  | Sets the `service.node.name` field so you can filter your logs by a particular node of your clustered service |
| `eventDataset` | String | `${serviceName}` | Sets the `event.dataset` field used by the machine learning job of the Logs app to look for anomalies in the log rate. |
| `stackTraceAsArray` | boolean | `false` | Serializes the [`error.stack_trace`](ecs://docs/reference/ecs-error.md) as a JSON array where each element is in a new line to improve readability. Note that this requires a slightly more complex [Filebeat configuration](#setup-stack-trace-as-array). |
| `includeOrigin` | boolean | `false` | If `true`, adds the [`log.origin.file.name`](ecs://docs/reference/ecs-log.md) fields. |
| `additionalFields` | String |  | Adds additional static fields to all log events. The fields are specified as comma-separated key-value pairs. Example: `additionalFields=key1=value1,key2=value2`. |
::::::

:::::::
::::{note}
If you’re using the Elastic APM Java agent, log correlation is enabled by default starting in version 1.30.0. In previous versions, log correlation is off by default, but can be enabled by setting the `enable_log_correlation` config to `true`.
::::



## Step 2: Configure Filebeat [setup-step-2]

:::::::{tab-set}

::::::{tab-item} Log file
1. Follow the [Filebeat quick start](beats://docs/reference/filebeat/filebeat-installation-configuration.md)
2. Add the following configuration to your `filebeat.yaml` file.

For Filebeat 7.16+

```yaml
filebeat.inputs:
- type: filestream <1>
  paths: /path/to/logs.json
  parsers:
    - ndjson:
      overwrite_keys: true <2>
      add_error_key: true <3>
      expand_keys: true <4>

processors: <5>
  - add_host_metadata: ~
  - add_cloud_metadata: ~
  - add_docker_metadata: ~
  - add_kubernetes_metadata: ~
```

1. Use the filestream input to read lines from active log files.
2. Values from the decoded JSON object overwrite the fields that {{filebeat}} normally adds (type, source, offset, etc.) in case of conflicts.
3. {{filebeat}} adds an "error.message" and "error.type: json" key in case of JSON unmarshalling errors.
4. {{filebeat}} will recursively de-dot keys in the decoded JSON, and expand them into a hierarchical object structure.
5. Processors enhance your data. See [processors](beats://docs/reference/filebeat/filtering-enhancing-data.md) to learn more.


For Filebeat < 7.16

```yaml
filebeat.inputs:
- type: log
  paths: /path/to/logs.json
  json.keys_under_root: true
  json.overwrite_keys: true
  json.add_error_key: true
  json.expand_keys: true

processors:
- add_host_metadata: ~
- add_cloud_metadata: ~
- add_docker_metadata: ~
- add_kubernetes_metadata: ~
```
::::::

::::::{tab-item} Kubernetes
1. Make sure your application logs to stdout/stderr.
2. Follow the [Run Filebeat on Kubernetes](beats://docs/reference/filebeat/running-on-kubernetes.md) guide.
3. Enable [hints-based autodiscover](beats://docs/reference/filebeat/configuration-autodiscover-hints.md) (uncomment the corresponding section in `filebeat-kubernetes.yaml`).
4. Add these annotations to your pods that log using ECS loggers. This will make sure the logs are parsed appropriately.

```yaml
annotations:
  co.elastic.logs/json.overwrite_keys: true <1>
  co.elastic.logs/json.add_error_key: true <2>
  co.elastic.logs/json.expand_keys: true <3>
```

1. Values from the decoded JSON object overwrite the fields that {{filebeat}} normally adds (type, source, offset, etc.) in case of conflicts.
2. {{filebeat}} adds an "error.message" and "error.type: json" key in case of JSON unmarshalling errors.
3. {{filebeat}} will recursively de-dot keys in the decoded JSON, and expand them into a hierarchical object structure.
::::::

::::::{tab-item} Docker
1. Make sure your application logs to stdout/stderr.
2. Follow the [Run Filebeat on Docker](beats://docs/reference/filebeat/running-on-docker.md) guide.
3. Enable [hints-based autodiscover](beats://docs/reference/filebeat/configuration-autodiscover-hints.md).
4. Add these labels to your containers that log using ECS loggers. This will make sure the logs are parsed appropriately.

```yaml
labels:
  co.elastic.logs/json.overwrite_keys: true <1>
  co.elastic.logs/json.add_error_key: true <2>
  co.elastic.logs/json.expand_keys: true <3>
```

1. Values from the decoded JSON object overwrite the fields that {{filebeat}} normally adds (type, source, offset, etc.) in case of conflicts.
2. {{filebeat}} adds an "error.message" and "error.type: json" key in case of JSON unmarshalling errors.
3. {{filebeat}} will recursively de-dot keys in the decoded JSON, and expand them into a hierarchical object structure.
::::::

:::::::
For more information, see the [Filebeat reference](beats://docs/reference/filebeat/configuring-howto-filebeat.md).


### When `stackTraceAsArray` is enabled [setup-stack-trace-as-array]

Filebeat can normally only decode JSON if there is one JSON object per line. When `stackTraceAsArray` is enabled, there will be a new line for each stack trace element which improves readability. But when combining the multiline settings with a `decode_json_fields` we can also handle multi-line JSON:

```yaml
filebeat.inputs:
  - type: log
    paths: /path/to/logs.json
    multiline.pattern: '^{'
    multiline.negate: true
    multiline.match: after
processors:
  - decode_json_fields:
      fields: message
      target: ""
      overwrite_keys: true
  # flattens the array to a single string
  - script:
      when:
        has_fields: ['error.stack_trace']
      lang: javascript
      id: my_filter
      source: >
        function process(event) {
            event.Put("error.stack_trace", event.Get("error.stack_trace").join("\n"));
        }
```
