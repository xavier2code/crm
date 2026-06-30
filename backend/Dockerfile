# Backend Dockerfile
# 多阶段构建：Build Stage + Runtime Stage

# ===== Build Stage =====
FROM eclipse-temurin:21-jdk-alpine AS builder

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

WORKDIR /app

# 先复制依赖文件，利用 Docker 缓存
COPY gradle/ gradle/
COPY gradlew .
COPY settings.gradle build.gradle ./

# 下载依赖
RUN ./gradlew dependencies --no-daemon || true

# 复制源码并构建
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ===== Runtime Stage =====
FROM eclipse-temurin:21-jre-alpine

ENV TZ=Asia/Shanghai
ENV APP_NAME=crm-cy
ENV LOG_PATH=/data/crm/logs
ENV JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

RUN apk add --no-cache tzdata wget && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    addgroup -S spring && \
    adduser -S spring -G spring && \
    mkdir -p /data/crm/logs /data/crm/files && \
    chown -R spring:spring /data/crm

WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /app/build/libs/*.jar app.jar

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
