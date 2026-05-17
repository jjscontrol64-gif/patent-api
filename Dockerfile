FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar -x test --no-daemon \
    && find build/libs -name "*.jar" ! -name "*-plain.jar" -exec cp {} app.jar \;

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ENV PORT=10000
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:InitialRAMPercentage=15 -XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=160m -XX:+ExitOnOutOfMemoryError"
ENV APP_LOG_LEVEL=INFO
ENV SERVER_TOMCAT_THREADS_MAX=50
ENV SERVER_TOMCAT_THREADS_MIN_SPARE=5
ENV SERVER_TOMCAT_ACCEPT_COUNT=50

RUN groupadd --system app && useradd --system --gid app app

COPY --from=builder /workspace/app.jar app.jar

USER app

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
