# ============ 构建阶段 ============
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build
COPY pom.xml ./
COPY repo ./repo
COPY src ./src

RUN mvn clean package -DskipTests -q

# ============ 运行阶段 ============
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --no-create-home --ingroup appgroup appuser
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

COPY --from=builder /build/target/*.jar app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
