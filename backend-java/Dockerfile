# Build and run image for the SilentVoix backend (used by Railway; works with any Docker host).
# Build context is this directory: docker build -t silentvoix-backend backend-java

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependencies first, so a source-only change reuses this layer.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run in CI (./mvnw verify); the image build only packages.
RUN ./mvnw -B -q -DskipTests package \
    && cp target/backend-*.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

# Do not run as root.
RUN useradd --system --no-create-home silentvoix
USER silentvoix

COPY --from=build /app/app.jar app.jar

# The platform sets PORT; 8081 is the default when it does not (see application.properties).
EXPOSE 8081

# Size the heap from the container's memory limit rather than the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "app.jar"]
