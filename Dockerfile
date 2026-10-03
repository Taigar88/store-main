# ---- build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle* build.gradle* gradle.properties* ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain' | head -n 1)" /workspace/app.jar

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER app
EXPOSE 8080
# Database settings come from the environment, e.g.
#   SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/store
#   SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
