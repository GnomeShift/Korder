# Stage 1: Build
FROM --platform=$BUILDPLATFORM gradle:8.14-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN --mount=type=cache,target=/home/gradle/.gradle/caches gradle dependencies --no-daemon
COPY src ./src
RUN --mount=type=cache,target=/home/gradle/.gradle/caches gradle buildFatJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S korder && adduser -S korder -G korder
COPY --from=builder --chown=korder:korder /app/build/libs/*-all.jar app.jar
RUN mkdir -p /app/docs && chown korder:korder /app/docs
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+ExitOnOutOfMemoryError"
USER korder
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
