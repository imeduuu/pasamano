# syntax=docker/dockerfile:1
# Imagen multi-etapa del backend (RNF-POR-03, < 300 MB): compila con JDK 21 y ejecuta con un
# JRE recortado con jlink sobre Alpine, con un usuario sin privilegios.

# ---------- Etapa 1: compilación ----------
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencias primero para aprovechar la caché de capas.
COPY pom.xml ./
COPY .mvn .mvn
COPY config config
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:resolve dependency:resolve-plugins || true

COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests -Dspotless.check.skip=true -Dcheckstyle.skip=true -Djacoco.skip=true \
 && cp target/trato-backend-*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------- Etapa 2: JRE mínimo con jlink ----------
FROM eclipse-temurin:21-jdk-alpine AS jre
RUN jlink \
      --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.jfr,jdk.localedata,jdk.management,jdk.naming.dns,jdk.net,jdk.unsupported,jdk.zipfs,jdk.charsets \
      --include-locales=en,es \
      --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
      --output /opt/jre

# ---------- Etapa 3: ejecución ----------
FROM alpine:3.24
ENV JAVA_HOME=/opt/java \
    PATH=/opt/java/bin:$PATH \
    LANG=C.UTF-8
RUN apk add --no-cache ca-certificates tzdata \
 && addgroup -S trato \
 && adduser -S -G trato -H -s /sbin/nologin trato
COPY --from=jre /opt/jre /opt/java
WORKDIR /app

COPY --from=build --chown=trato:trato /build/extracted/dependencies/ ./
COPY --from=build --chown=trato:trato /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=trato:trato /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=trato:trato /build/extracted/application/ ./

USER trato
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Duser.timezone=UTC"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
