# binaflow

### Binary message exchanging via WebSocket.

This project is a spring boot starter that provides tools for extremely fast binary message exchanging via WebSocket.
For defining schema used protobuf.

## Getting Started

### Prerequisites

- Java 21+
- Spring Boot 3+

### How to use

1) Add the following dependency to your project:

```xml

<dependency>
    <groupId>com.github.binaflow</groupId>
    <artifactId>binaflow-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

2) Create a protobuf schema:

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "dev.toliyansky.citiespicker.dto"; // define package for generated classes

message GetCitiesRequest {
  string messageType = 1;
  string messageId = 2;
  double latitude = 3;
  double longitude = 4;
}

message GetCitiesResponse {
  string messageType = 1;
  string messageId = 2;
  repeated City cities = 3;
}

message City {
  string name = 1;
  double latitude = 2;
  double longitude = 3;
}
```

**Note:** any message that will use as DTO must contain `string messageType = 1;` and `string messageId = 2;` fields.
There is really important, because BinaFlow uses these fields for routing messages. But if message is not DTO, you can
skip these fields, for example `City` message.

3) Generate classes from protobuf schema:

```xml

<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.4.1</version>
    <executions>
        <execution>
            <id>generate-sources</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>protoc</executable>
                <arguments>
                    <argument>-I=./proto-schema</argument> <!-- path to your proto schema directory -->
                    <argument>--java_out=./src/main/java</argument>
                    <argument>my-schema.proto</argument>  <!-- path to your proto schema -->
                </arguments>
                <workingDirectory>${project.basedir}</workingDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

4) Setup application properties:

```yaml
binaflow:
  schema:
    directory: proto-schema # path to your proto schema directory
  http-path: /binaflow # path for WebSocket endpoint
```

5) Create a message controller:

```java
@Controller
public class CitiesController {

    @MessageMapping
    public GetCitiesResponse getCities(GetCitiesRequest request) {
        return GetCitiesResponse.newBuilder()
                .setMessageType("GetCitiesResponse")
                .setMessageId(request.getMessageId())
                .addAllCities(List.of(
                        City.newBuilder()
                                .setName("Moscow")
                                .setLatitude(55.7558)
                                .setLongitude(37.6176)
                                .build(),
                        City.newBuilder()
                                .setName("Saint Petersburg")
                                .setLatitude(59.9343)
                                .setLongitude(30.3351)
                                .build()
                ))
                .build();
    }
}
```

6) That's all for backend side. Now you can create a client for your application. For example, you can use <inset link to npm package> library.