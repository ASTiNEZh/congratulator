-- Схема для интеграционных тестов (PostgreSQL в Testcontainers).
-- Повторяет структуру из forMigration.sql, но с исправлениями BUG-10/BUG-11:
-- значения enum совпадают с BirthdayStatus, таблица CELEBRANTS создаётся.
DROP TABLE IF EXISTS birthdays;
DROP TABLE IF EXISTS celebrants;
DROP TYPE IF EXISTS birthday_status;

CREATE TYPE birthday_status AS ENUM ('OTHER', 'RECENT', 'TODAY', 'UPCOMING');

CREATE TABLE celebrants (
    uuid               UUID PRIMARY KEY,
    first_name         VARCHAR(255),
    last_name          VARCHAR(255),
    surname            VARCHAR(255),
    photo_storage_path VARCHAR(1024)
);

CREATE TABLE birthdays (
    uuid           UUID PRIMARY KEY,
    celebrant_uuid UUID UNIQUE,
    birth_date     DATE NOT NULL,
    status         birthday_status NOT NULL,
    CONSTRAINT fk_celebrant FOREIGN KEY (celebrant_uuid) REFERENCES celebrants (uuid) ON DELETE CASCADE
);
