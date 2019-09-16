[![Build Status](https://apm-ci.elastic.co/buildStatus/icon?job=apm-agent-java%2Fjava-ecs-logging-mbp%2Fmaster)](https://apm-ci.elastic.co/job/apm-agent-java/job/java-ecs-logging-mbp/job/master/)

# ECS-based logging for Java applications

Centralized logging for Java applications with the Elastic stack made easy

<img width="1829" alt="logs-ui" src="https://user-images.githubusercontent.com/2163464/62682932-9cac3600-b9bd-11e9-9cc3-39e907280f8e.png">

## What is ECS?

Elastic Common Schema (ECS) defines a common set of fields for ingesting data into Elasticsearch.
For more information about ECS, visit the [ECS Reference Documentation](https://www.elastic.co/guide/en/ecs/current/ecs-reference.html).

## What is ECS logging?

This library helps to log ECS-compatible JSON into a file

Example:
```
{"@timestamp":"2019-08-06T12:09:12.375Z", "log.level": "INFO", "message":"Tomcat started on port(s): 8080 (http) with context path ''", "service.name":"spring-petclinic","process.thread.name":"restartedMain","log.logger":"org.springframework.boot.web.embedded.tomcat.TomcatWebServer"}
{"@timestamp":"2019-08-06T12:09:12.379Z", "log.level": "INFO", "message":"Started PetClinicApplication in 7.095 seconds (JVM running for 9.082)", "service.name":"spring-petclinic","process.thread.name":"restartedMain","log.logger":"org.springframework.samples.petclinic.PetClinicApplication"}
{"@timestamp":"2019-08-06T14:08:40.199Z", "log.level":"DEBUG", "message":"init find form", "service.name":"spring-petclinic","process.thread.name":"http-nio-8080-exec-8","log.logger":"org.springframework.samples.petclinic.owner.OwnerController","transaction.id":"28b7fb8d5aba51f1","trace.id":"2869b25b5469590610fea49ac04af7da"}
```

## Why ECS logging?

* No parsing of the log file required \
  Logging in ECS-compatible JSON has the advantage that you don't need to set up a logstash/ingest node pipeline to parse logs using grok.
* No external dependencies
* Highly efficient by manually serializing JSON
* Low/Zero allocations (reduces GC pauses) \
  The log4j2 `EcsLayout` does not allocate any memory (unless the log event contains an `Exception`)
* Decently human-readable JSON structure \
  The first three fields are always `@timestamp`, `log.level` and `message`.
* Use the Kibana [Logs UI](https://www.elastic.co/guide/en/kibana/7.3/xpack-logs.html) without additional configuration \
  As this library adheres to [ECS](https://www.elastic.co/guide/en/ecs/current/ecs-reference.html), the Logs UI knows which fields to show
* Out-of-the-box correlation of logs and traces via the Logs UI and APM UI when using the [Elastic APM Java agent](https://www.elastic.co/guide/en/apm/agent/java/current/index.html)
* Using a common schema across different services and teams makes it possible create reusable dashboards and avoids [mapping explosions](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html#mapping-limit-settings).

### APM Log correlation

If you are using the [Elastic APM Java agent](https://www.elastic.co/guide/en/apm/agent/java/current/index.html),
you can leverage the [log correlation feature](https://www.elastic.co/guide/en/apm/agent/java/current/config-logging.html#config-enable-log-correlation) without any additional configuration.

This lets you jump from the [Span timeline in the APM UI](https://www.elastic.co/guide/en/kibana/master/spans.html) to the
[Logs UI](https://www.elastic.co/guide/en/kibana/7.3/xpack-logs.html),
showing only the logs which belong to the corresponding request.
Vice versa, you can also jump from a log line in the Logs UI to the Span Timeline of the APM UI.

### Additional advantages when using in combination with Filebeat

We recommend using this library to log into a JSON log file and let Filebeat send the logs to Elasticsearch
* Resilient in case of outages \
  [Guaranteed at-least-once delivery](https://www.elastic.co/guide/en/beats/filebeat/current/how-filebeat-works.html#at-least-once-delivery)
  without buffering within the application, thus no risk of `OutOfMemoryError`s or lost events.
  There's also the option to use either the JSON logs or plain-text logs as a fallback.
* Loose coupling \
  The application does not need to know the details of the logging backend (URI, credentials, etc.).
  You can also leverage alternative [Filebeat outputs](https://www.elastic.co/guide/en/beats/filebeat/current/configuring-output.html),
  like Logstash, Kafka or Redis.
* Index Lifecycle management \
  Leverage Filebeat's default [index lifemanagement settings](https://www.elastic.co/guide/en/beats/filebeat/master/ilm.html).
  This is much more efficient than using daily indices.
* Efficient Elasticsearch mappings \
  Leverage Filebeat's default ECS-compatible [index template](https://www.elastic.co/guide/en/beats/filebeat/master/configuration-template.html)

## Mapping

|ECS field | Log4j2 API  |
|----------|-------------|
|[`@timestamp`](https://www.elastic.co/guide/en/ecs/current/ecs-base.html) | [`LogEvent#getTimeMillis()`](https://logging.apache.org/log4j/log4j-2.3/log4j-core/apidocs/org/apache/logging/log4j/core/LogEvent.html#getTimeMillis()) |
|[`log.level`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html) | [`LogEvent#getLevel()`](https://logging.apache.org/log4j/log4j-2.3/log4j-core/apidocs/org/apache/logging/log4j/core/LogEvent.html#getLevel()) |
|[`log.logger`](https://www.elastic.co/guide/en/ecs/current/ecs-log.html)|[`LogEvent#getLoggerName()`](https://logging.apache.org/log4j/log4j-2.3/log4j-core/apidocs/org/apache/logging/log4j/core/LogEvent.html#getLoggerName())|
|[`message`](https://www.elastic.co/guide/en/ecs/current/ecs-base.html)|[`LogEvent#getMessage()`](https://logging.apache.org/log4j/log4j-2.3/log4j-core/apidocs/org/apache/logging/log4j/core/LogEvent.html#getMessage())|
|[`error.code`](https://www.elastic.co/guide/en/ecs/current/ecs-error.html)|[`Throwable#getClass()`](https://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#getClass())|
|[`error.message`](https://www.elastic.co/guide/en/ecs/current/ecs-error.html)|[`Throwable#getStackTrace()`](https://docs.oracle.com/javase/7/docs/api/java/lang/Throwable.html#getMessage())|
|[`error.stack_trace`](https://www.elastic.co/guide/en/ecs/current/ecs-error.html)|[`Throwable#getStackTrace()`](https://docs.oracle.com/javase/7/docs/api/java/lang/Throwable.html#getStackTrace())|
|[`process.thread.name`](https://www.elastic.co/guide/en/ecs/current/ecs-process.html)|[`LogEvent#getThreadName()`](https://logging.apache.org/log4j/log4j-2.3/log4j-core/apidocs/org/apache/logging/log4j/core/LogEvent.html#getThreadName()) |
|[`labels`](https://www.elastic.co/guide/en/ecs/current/ecs-base.html)|[`LogEvent#getContextMap()`](https://logging.apache.org/log4j/log4j-2.3/log4j-core/apidocs/org/apache/logging/log4j/core/LogEvent.html#getContextMap())|
|[`tags`](https://www.elastic.co/guide/en/ecs/current/ecs-base.html)|[`LogEvent#getContextStack()`](https://logging.apache.org/log4j/log4j-2.3/log4j-core/apidocs/org/apache/logging/log4j/core/LogEvent.html#getContextStack())|

## Getting Started

### Logging configuration

- [Logback](logback-ecs-encoder/README.md)
- [Log4j2](log4j2-ecs-layout/README.md)
- [Log4j](log4j-ecs-layout/README.md)

### Filebeat configuration

#### With `filebeat.yml` configuration file

```yaml
filebeat.inputs:
- type: log
  paths: /path/to/logs.json
  json.keys_under_root: true

# no further processing required, logs can directly be sent to Elasticsearch  
output.elasticsearch:
  hosts: ["https://localhost:9200"]

# Or to Elastic cloud
# Example:
#cloud.id: "staging:dXMtZWFzdC0xLmF3cy5mb3VuZC5pbyRjZWM2ZjI2MWE3NGJmMjRjZTMzYmI4ODExYjg0Mjk0ZiRjNmMyY2E2ZDA0MjI0OWFmMGNjN2Q3YTllOTYyNTc0Mw=="
#cloud.auth: "elastic:YOUR_PASSWORD"

```

For more information, check the [Filebeat documentation](https://www.elastic.co/guide/en/beats/filebeat/master/configuring-howto-filebeat.html)

#### With Beats Central Management

- Enroll the beat \
  In Kibana, go to `Management` > `Beats` > `Central Management` > `Enroll Beats` and follow the instructions.
- Add a `Filebeat input` configuration block
  - Configure the path of the log file(s)
  - Set `Other config`
    ```yaml
    type: log
    json.keys_under_root: true
    ```
- Add an `Output` configuration block
  - Set `Output type` to `Elasticsearch`
  - Configure the `hosts`
  - For secured Elasticsearch deployments (like Elastic cloud) set `Username` and `Password`
