FROM maven:3-jdk-8-alpine as builder

COPY . /src
WORKDIR /src
RUN mvn package

FROM openjdk:8-jre-alpine

USER nobody

CMD ["java", "${JAVA_OPTS}", "-jar", "zeebe-worker-sleep.jar"]

COPY --from=builder /src/target/zeebe-worker-sleep-*.jar /zeebe-worker-sleep.jar
