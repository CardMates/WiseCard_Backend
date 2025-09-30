# 멀티스테이지 빌드를 위한 Dockerfile
FROM eclipse-temurin:17-jdk AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 래퍼와 의존성 파일들을 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gRPC 모듈 복사
COPY gRPC gRPC

# 의존성 다운로드 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# gRPC 모듈 먼저 빌드
RUN ./gradlew :gRPC:build --no-daemon -x test

# 전체 애플리케이션 빌드
RUN ./gradlew build --no-daemon -x test

# 실행 단계
FROM eclipse-temurin:17-jre

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출 (Spring Boot 기본 포트 + gRPC 포트)
EXPOSE 8080 9091

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]