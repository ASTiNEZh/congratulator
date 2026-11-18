package ru.astinezh.congratulator.support;

import ru.astinezh.congratulator.models.Birthday;
import ru.astinezh.congratulator.models.BirthdayStatus;
import ru.astinezh.congratulator.models.Celebrant;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Фабрики тестовых данных.
 *
 * ВАЖНО: в проекте поле «celebrant» в Birthday написано с КИРИЛЛИЧЕСКОЙ буквой «с»
 * (код U+0441), поэтому и сгенерированные методы содержат кириллицу. Чтобы тесты не зависели от
 * раскладки клавиатуры, такие имена записаны через unicode-эскейпы. Если поле будет переименовано
 * в латиницу (рекомендуется, см. BUG-6), достаточно поправить только этот класс.
 */
public final class TestData {

    private TestData() {
    }

    public static Celebrant celebrant(UUID id, String firstName, String lastName, String surname) {
        Celebrant c = new Celebrant();
        c.setUuid(id);
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setSurname(surname);
        return c;
    }

    public static Birthday birthday(UUID id, Celebrant celebrant, LocalDate birthDate, BirthdayStatus status) {
        Birthday b = new Birthday();
        b.setUuid(id);
        b.set\u0421elebrant(celebrant);
        b.setBirthDate(birthDate);
        b.setStatus(status);
        return b;
    }

    public static Celebrant celebrantOf(Birthday birthday) {
        return birthday.get\u0421elebrant();
    }
}
