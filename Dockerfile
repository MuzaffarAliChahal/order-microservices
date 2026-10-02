# One Dockerfile for every service: docker build --build-arg MODULE=order-service -t order-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
ARG MODULE
RUN mvn -q -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
RUN useradd --system --uid 1001 spring
USER spring
COPY --from=build /app/${MODULE}/target/${MODULE}-*.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
