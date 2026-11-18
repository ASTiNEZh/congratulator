# syntax=docker/dockerfile:1

# ---------- Этап 1: сборка ----------
# JDK 21 нужен, потому что в build.gradle.kts задан toolchain Java 21
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle-обёртка и файлы сборки копируем первыми — так слой с зависимостями кэшируется
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
# если gradlew сохранён с Windows-переводами строк (CRLF), в Linux он не запустится
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src ./src

# Тесты в образе не гоняем: интеграционным тестам нужен Docker (Testcontainers).
# Генерация API из openapi.yaml выполняется автоматически при сборке.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar -x test --no-daemon \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n 1)" /workspace/app.jar

# ---------- Этап 2: запуск ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# отдельный пользователь без root-прав и каталог для загружаемых фото
RUN groupadd --system app && useradd --system --gid app app \
    && mkdir -p /data/uploads && chown -R app:app /data /app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app
EXPOSE 8081
VOLUME /data/uploads

# Параметры БД и каталог хранения переопределяются переменными окружения
# (SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, APP_STORAGE_ROOT)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
