-- Схема для запуска в Docker. Значения enum совпадают с BirthdayStatus в коде и API,
-- таблица CELEBRANTS создаётся (в forMigration.sql это не так — BUG-10 и BUG-11).
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
