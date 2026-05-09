# Build context MUST be this folder (backend/) — parent Maven reactor + modules.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -pl app -am package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --create-home --shell /usr/sbin/nologin appuser
COPY --from=build /build/app/target/*-exec.war /app/app.war
RUN chown appuser:appuser /app/app.war
USER appuser
ENV PORT=8080
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.war"]
