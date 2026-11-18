# Congratulator

Сервис для учёта дней рождения: хранит именинников и даты рождения, показывает ближайшие ДР
(в ближайшие 30 дней), позволяет прикреплять к именинникам фотографии. Работает как REST API.

## Возможности

- Создание, просмотр, изменение и удаление записей о днях рождения.
- Список всех ДР с пагинацией и сортировкой по дате рождения.
- Список «актуальных» ДР: сегодня и в ближайшие 30 дней (по календарю, год рождения не важен).
- Загрузка и скачивание фотографий именинников.
- Документация API: Swagger UI.

## Стек

| Область | Технологии |
|---|---|
| Язык и платформа | Java 21, Kotlin (плагин сборки), Spring Boot 3.2.2 |
| Web, данные | Spring Web, Spring Data JPA, Hibernate, Bean Validation |
| База данных | PostgreSQL 16 |
| API-first | OpenAPI 3.0 (`openapi.yaml`), генерация интерфейсов контроллеров и DTO (OpenAPI Generator 7.6.0) |
| Маппинг | ModelMapper, Lombok |
| Документация | springdoc-openapi (Swagger UI) |
| Сборка | Gradle 8.7 (Kotlin DSL) |
| Тесты | JUnit 5, Mockito, Spring MockMvc, Testcontainers, JaCoCo |
| Контейнеры | Docker, Docker Compose |

## Быстрый старт (Docker Compose)

Нужен только Docker.

```bash
docker compose up --build
```

После запуска:

- API: http://localhost:8081
- Swagger UI: http://localhost:8081/swagger-ui.html
- Описание API (JSON): http://localhost:8081/v3/api-docs

Пароль БД по умолчанию `postgres`; свой можно задать переменной окружения:

```bash
DB_PASSWORD=секрет docker compose up --build
```

Остановка: `docker compose down` (данные сохраняются в volume; `docker compose down -v` удалит и данные).

> Схема БД при первом запуске создаётся скриптом `docker/init.sql`.
> В репозитории также есть `src/main/resources/forMigration.sql`, но он расходится с кодом (см. «Известные проблемы»).

## Запуск без Docker

Требования: JDK 21, PostgreSQL.

1. Создайте базу и примените схему из `docker/init.sql`.
2. Задайте подключение к БД через переменные окружения (или поправьте `application.properties`):

   ```bash
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=ваш_пароль
   export APP_STORAGE_ROOT=./uploads
   ```

3. Запустите приложение:

   ```bash
   ./gradlew bootRun
   ```

При сборке интерфейсы контроллеров и DTO генерируются из `src/main/resources/openapi.yaml`
в `build/openapi` автоматически.

## Настройка

| Параметр | Переменная окружения | По умолчанию |
|---|---|---|
| Порт | `SERVER_PORT` | `8081` |
| URL базы данных | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/postgres` |
| Пользователь БД | `SPRING_DATASOURCE_USERNAME` | `postgres` |
| Пароль БД | `SPRING_DATASOURCE_PASSWORD` | задан в `application.properties` |
| Каталог для фото | `APP_STORAGE_ROOT` | `/data/uploads` |
| Максимальный размер файла | `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | `5MB` |

## API

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/birthdays/new` | Создать запись о ДР (вместе с именинником) |
| `GET` | `/` | ДР, наступающие в ближайшие 30 дней |
| `GET` | `/birthdays` | Все ДР. Параметры: `page` (от 0), `size` (от 1, по умолчанию 20), `sort` (`asc`/`desc`) |
| `GET` | `/birthdays/{uuid}` | Одна запись о ДР |
| `PUT` | `/birthdays/{uuid}` | Обновить запись |
| `DELETE` | `/birthdays/{uuid}` | Удалить запись |
| `POST` | `/files/upload` | Загрузить фото (`multipart/form-data`: `files`, `uuid` именинника) |
| `GET` | `/files/{id}` | Скачать фото именинника |

Статусы ДР: `OTHER`, `RECENT`, `TODAY`, `UPCOMING`.

### Пример запроса

```bash
curl -X POST http://localhost:8081/birthdays/new \
  -H "Content-Type: application/json" \
  -d '{
    "uuid": "00000000-0000-0000-0000-000000000001",
    "birthDate": "2000-01-01",
    "status": "UPCOMING",
    "сelebrant": {
      "uuid": "10000000-0000-0000-0000-000000000000",
      "firstName": "Иван",
      "lastName": "Иванович",
      "surname": "Иванов"
    }
  }'
```

> ⚠️ Поле именинника в JSON называется `сelebrant`, и первая буква — **кириллическая «с»**
> (U+0441), а не латинская «c». Копируйте имя поля из Swagger UI или из примера выше.
> Это особенность текущей версии `openapi.yaml`.

## Модель данных

- **Celebrant** (именинник): `uuid`, `firstName`, `lastName` (отчество), `surname` (фамилия), `photoStoragePath`.
- **Birthday** (день рождения): `uuid`, `birthDate`, `status`, связь «один к одному» с именинником.

Дата рождения хранится в записи о ДР, а не у именинника — это сознательное решение автора
(см. описание поля в `openapi.yaml`).

## Тестирование

В проекте 111 автотестов (юнит, REST-слой, работа с БД, сквозные).

| Уровень | Что проверяется | Нужен Docker |
|---|---|---|
| Сервисы (Mockito) | `BirthdayService`, `CelebrantService`, `FileService` (на временном каталоге) | нет |
| REST-слой (MockMvc) | `BirthdayController`, `FilesController`: коды ответов, валидация, JSON | нет |
| Контракт | `openapi.yaml`, enum статусов, SQL-схема | нет |
| Репозитории и сервис с БД | PostgreSQL в Testcontainers: пагинация, ограничения, каскадное удаление | да |
| Сквозные (API + БД) | HTTP → контроллер → JPA → PostgreSQL, Swagger | да |
| Контекст | `contextLoads`: приложение поднимается целиком | да |

```bash
./gradlew test        # все тесты, включая Docker-тесты
./gradlew unitTest    # только тесты без Docker
```

Отчёт о покрытии (JaCoCo) после `./gradlew test`:
`build/reports/jacoco/test/html/index.html`.

Особенности:

- Для интеграционных тестов нужен запущенный Docker. Testcontainers использует версию 1.21.4:
  более старые версии не подключаются к Docker Engine 29+ (ошибка `client version 1.32 is too old`).
- Тесты на известные дефекты помечены `@Disabled("BUG-N: ...")`. Они описывают правильное поведение
  и сейчас упали бы; после исправления кода достаточно убрать `@Disabled`.
- Тесты окна «30 дней» зависят от текущей даты и рассчитаны относительно «сегодня».

## Известные проблемы

Найдены при написании автотестов; для каждой есть отключённый тест.

| № | Проблема |
|---|---|
| BUG-1 | `GET /birthdays` без параметра `sort` падает с `NullPointerException`, хотя параметр необязательный |
| BUG-4 | `POST /birthdays/new` без именинника возвращает 500, хотя поле необязательное |
| BUG-7 | `GET /birthdays/{uuid}` для несуществующей записи возвращает 500 вместо 404 |
| BUG-8 | У `FilesController` нет `@RestController`, поэтому `/files/**` не работают |
| BUG-9 | Опечатка в `openapi.yaml`: media type `mapplication/json` в ответе `GET /` |
| BUG-10 | В `forMigration.sql` значения enum (`PLANNED/CELEBRATED/MISSED`) не совпадают с кодом и API (`OTHER/RECENT/TODAY/UPCOMING`) |
| BUG-11 | `forMigration.sql` ссылается на таблицу `CELEBRANTS`, но не создаёт её |

Прочее:

- Поле `сelebrant` написано с кириллической «с» — это усложняет клиентский код и тесты.
- Пароль к БД лежит в `application.properties` — лучше вынести в переменные окружения.
- В описании `GET /` сказано «менее чем через 30 дней», а граница в коде включительная (30 дней входят).

## Структура проекта

```
.
├── Dockerfile
├── docker-compose.yml
├── docker/init.sql                  # схема БД для Docker
├── build.gradle.kts
├── src/main
│   ├── java/ru/astinezh/congratulator
│   │   ├── controller/              # REST-контроллеры (реализуют сгенерированные интерфейсы)
│   │   ├── services/                # бизнес-логика, работа с файлами
│   │   ├── repositories/            # Spring Data JPA
│   │   └── models/                  # сущности и enum статусов
│   └── resources
│       ├── openapi.yaml             # контракт API (источник генерации)
│       ├── forMigration.sql         # исходная схема БД (устарела, см. BUG-10/11)
│       └── application.properties
└── src/test                         # тесты (см. раздел «Тестирование»)
```
