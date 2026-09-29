FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn --batch-mode dependency:go-offline
COPY src src
RUN mvn --batch-mode package

FROM eclipse-temurin:25-jre
RUN useradd --system vapor
USER vapor
WORKDIR /app
COPY --from=build /build/target/vapor.jar vapor.jar
EXPOSE 8080
CMD ["java", "-jar", "vapor.jar"]
