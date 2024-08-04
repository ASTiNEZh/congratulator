CREATE DATABASE congratulator;
COMMENT ON DATABASE congratulator IS 'БД для "Поздравлятора"';

CREATE SCHEMA congratulator;
COMMENT ON SCHEMA congratulator IS 'main-схема для БД';

CREATE TABLE IF NOT EXISTS congratulator.birthdays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    birthdays_date TIMESTAMP WITH TIME ZONE NOT NULL
);
COMMENT ON TABLE congratulator.birthdays IS 'Таблица для хранения дат ДР';
COMMENT ON COLUMN congratulator.birthdays.id IS 'Первичный ключ';
COMMENT ON COLUMN congratulator.birthdays.birthdays_date IS 'Дата дня рождения';

CREATE TABLE IF NOT EXISTS congratulator.persons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    birsdays_id UUID REFERENCES congratulator.birthdays(id),
    lastname TEXT NOT NULL,
    firstname TEXT NOT NULL,
    midlename TEXT NOT NULL,
    avatar TEXT NOT NULL,
    is_archived BOOLEAN DEFAULT false
);
COMMENT ON TABLE congratulator.persons IS 'Таблица для хранения данных людей';
COMMENT ON COLUMN congratulator.persons.id IS 'Первичный ключ';
COMMENT ON COLUMN congratulator.persons.birsdays_id IS 'Ссылка на дату рождения';
COMMENT ON COLUMN congratulator.persons.lastname IS 'Фамилия';
COMMENT ON COLUMN congratulator.persons.firstname IS 'Имя';
COMMENT ON COLUMN congratulator.persons.midlename IS 'Отчество';
COMMENT ON COLUMN congratulator.persons.avatar IS 'Расположение фотографии';
COMMENT ON COLUMN congratulator.persons.is_archived IS 'Удалён ли человек';