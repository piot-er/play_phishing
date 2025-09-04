FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1 as builder
WORKDIR /app
COPY build.sbt ./
COPY project ./project
RUN sbt update
COPY src ./src
RUN sbt clean assembly

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/scala-2.13/consent-gate-assembly-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
