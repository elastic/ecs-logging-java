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

Logging in ECS-compatible JSON has the advantage that you don't need to set up a logstash/ingest node pipeline to parse logs using grok.
Another benefit is that you are automatically using the field names the
[Logs UI](https://www.elastic.co/guide/en/kibana/7.3/xpack-logs.html) expects,
which means that getting started with it is straightforward.

## APM Log correlation

If you are using the [Elastic APM Java agent](https://www.elastic.co/guide/en/apm/agent/java/current/index.html),
you can leverage the [log correlation feature](https://www.elastic.co/guide/en/apm/agent/java/current/config-logging.html#config-enable-log-correlation) without any additional configuration.

This lets you jump from the [Span timeline in the APM UI](https://www.elastic.co/guide/en/kibana/master/spans.html) to the 
[Logs UI](https://www.elastic.co/guide/en/kibana/7.3/xpack-logs.html),
showing only the logs which belong to the corresponding request.
Vice versa, you can also jump from a log line in the Logs UI to the Span Timeline of the APM UI.

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

