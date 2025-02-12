FROM --platform=linux/amd64 maven:3.6.0-jdk-13 AS build

WORKDIR /app

# Copy the pom.xml and the source code
COPY pom.xml .
COPY src ./src
COPY libs ./libs
COPY src/main/resources /app/src/main/resources

# Package the application
RUN mvn clean package -DskipTests

# Use a smaller image to run the application
FROM --platform=linux/amd64 openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
