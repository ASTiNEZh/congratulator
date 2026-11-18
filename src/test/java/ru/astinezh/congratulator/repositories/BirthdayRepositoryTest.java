package ru.astinezh.congratulator.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import ru.astinezh.congratulator.models.Birthday;
import ru.astinezh.congratulator.models.BirthdayStatus;
import ru.astinezh.congratulator.models.Celebrant;
import ru.astinezh.congratulator.support.PostgresContainerConfig;
import ru.astinezh.congratulator.support.TestData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(PostgresContainerConfig.class)
@Tag("docker")
@DisplayName("BirthdayRepository (настоящий PostgreSQL в Testcontainers)")
class BirthdayRepositoryTest {

    @Autowired
    private BirthdayRepository birthdayRepository;
    @Autowired
    private CelebrantRepository celebrantRepository;
    @Autowired
    private TestEntityManager em;

    private Celebrant savedCelebrant() {
        return celebrantRepository.saveAndFlush(
                TestData.celebrant(UUID.randomUUID(), "Иван", "Иванович", "Иванов"));
    }

    private Birthday savedBirthday(Celebrant celebrant, LocalDate date, BirthdayStatus status) {
        return birthdayRepository.saveAndFlush(TestData.birthday(UUID.randomUUID(), celebrant, date, status));
    }

    private static List<LocalDate> dates(Page<Birthday> page) {
        return page.getContent().stream().map(Birthday::getBirthDate).toList();
    }

    @Test
    @DisplayName("Сохранение и чтение: все поля и связанный именинник")
    void saveAndLoadRoundTrip() {
        Celebrant celebrant = savedCelebrant();
        Birthday saved = savedBirthday(celebrant, LocalDate.of(2000, 1, 1), BirthdayStatus.TODAY);
        em.flush();
        em.clear();

        Birthday loaded = birthdayRepository.findById(saved.getUuid()).orElseThrow();

        assertEquals(LocalDate.of(2000, 1, 1), loaded.getBirthDate());
        assertEquals(BirthdayStatus.TODAY, loaded.getStatus());
        Celebrant loadedCelebrant = TestData.celebrantOf(loaded);
        assertEquals(celebrant.getUuid(), loadedCelebrant.getUuid());
        assertEquals("Иван", loadedCelebrant.getFirstName());
        assertEquals("Иванович", loadedCelebrant.getLastName());
        assertEquals("Иванов", loadedCelebrant.getSurname());
    }

    @ParameterizedTest(name = "статус {0} сохраняется и читается из PostgreSQL")
    @EnumSource(BirthdayStatus.class)
    @DisplayName("Каждый статус ДР корректно хранится в enum-колонке")
    void everyStatusRoundTrips(BirthdayStatus status) {
        Birthday saved = savedBirthday(null, LocalDate.of(1999, 9, 9), status);
        em.flush();
        em.clear();

        assertEquals(status, birthdayRepository.findById(saved.getUuid()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("ДР без именинника допустим (внешний ключ необязателен)")
    void birthdayWithoutCelebrant() {
        Birthday saved = savedBirthday(null, LocalDate.of(1990, 3, 3), BirthdayStatus.OTHER);
        em.flush();
        em.clear();

        assertNull(TestData.celebrantOf(birthdayRepository.findById(saved.getUuid()).orElseThrow()));
    }

    @Test
    @DisplayName("findById: неизвестный uuid -> пусто")
    void findByIdUnknown() {
        assertTrue(birthdayRepository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("Пагинация и сортировка по birthDate (по возрастанию)")
    void pagingAscending() {
        savedBirthday(null, LocalDate.of(1995, 5, 5), BirthdayStatus.OTHER);
        savedBirthday(null, LocalDate.of(1990, 1, 1), BirthdayStatus.OTHER);
        savedBirthday(null, LocalDate.of(2000, 12, 31), BirthdayStatus.OTHER);
        Sort asc = Sort.by(Sort.Direction.ASC, "birthDate");

        Page<Birthday> first = birthdayRepository.findAll(PageRequest.of(0, 2, asc));
        Page<Birthday> second = birthdayRepository.findAll(PageRequest.of(1, 2, asc));

        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(List.of(LocalDate.of(1990, 1, 1), LocalDate.of(1995, 5, 5)), dates(first));
        assertEquals(List.of(LocalDate.of(2000, 12, 31)), dates(second));
    }

    @Test
    @DisplayName("Сортировка по birthDate по убыванию")
    void pagingDescending() {
        savedBirthday(null, LocalDate.of(1995, 5, 5), BirthdayStatus.OTHER);
        savedBirthday(null, LocalDate.of(1990, 1, 1), BirthdayStatus.OTHER);
        savedBirthday(null, LocalDate.of(2000, 12, 31), BirthdayStatus.OTHER);

        Page<Birthday> page = birthdayRepository.findAll(
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "birthDate")));

        assertEquals(List.of(LocalDate.of(2000, 12, 31), LocalDate.of(1995, 5, 5), LocalDate.of(1990, 1, 1)),
                dates(page));
    }

    @Test
    @DisplayName("Страница за пределами данных пуста, но totalElements корректен")
    void pageBeyondData() {
        savedBirthday(null, LocalDate.of(1995, 5, 5), BirthdayStatus.OTHER);

        Page<Birthday> page = birthdayRepository.findAll(PageRequest.of(5, 10));

        assertTrue(page.getContent().isEmpty());
        assertEquals(1, page.getTotalElements());
    }

    @Test
    @DisplayName("Один именинник — одно ДР: повтор нарушает UNIQUE")
    void celebrantMustBeUnique() {
        Celebrant celebrant = savedCelebrant();
        savedBirthday(celebrant, LocalDate.of(2000, 1, 1), BirthdayStatus.OTHER);

        assertThrows(DataIntegrityViolationException.class,
                () -> savedBirthday(celebrant, LocalDate.of(2001, 2, 2), BirthdayStatus.OTHER));
    }

    @Test
    @DisplayName("Дата рождения обязательна (NOT NULL)")
    void birthDateIsRequired() {
        Birthday b = TestData.birthday(UUID.randomUUID(), null, null, BirthdayStatus.OTHER);

        assertThrows(DataIntegrityViolationException.class, () -> birthdayRepository.saveAndFlush(b));
    }

    @Test
    @DisplayName("Статус обязателен (NOT NULL)")
    void statusIsRequired() {
        Birthday b = TestData.birthday(UUID.randomUUID(), null, LocalDate.of(2000, 1, 1), null);

        assertThrows(DataIntegrityViolationException.class, () -> birthdayRepository.saveAndFlush(b));
    }

    @Test
    @DisplayName("Удаление ДР не удаляет именинника")
    void deletingBirthdayKeepsCelebrant() {
        Celebrant celebrant = savedCelebrant();
        Birthday saved = savedBirthday(celebrant, LocalDate.of(2000, 1, 1), BirthdayStatus.OTHER);
        em.flush();
        em.clear();

        birthdayRepository.deleteById(saved.getUuid());
        birthdayRepository.flush();

        assertFalse(birthdayRepository.existsById(saved.getUuid()));
        assertTrue(celebrantRepository.existsById(celebrant.getUuid()));
    }

    @Test
    @DisplayName("Удаление именинника каскадно удаляет его ДР (ON DELETE CASCADE)")
    void deletingCelebrantCascadesToBirthday() {
        Celebrant celebrant = savedCelebrant();
        Birthday saved = savedBirthday(celebrant, LocalDate.of(2000, 1, 1), BirthdayStatus.OTHER);
        em.flush();
        em.clear();

        celebrantRepository.deleteById(celebrant.getUuid());
        celebrantRepository.flush();
        em.clear();

        assertTrue(birthdayRepository.findById(saved.getUuid()).isEmpty());
    }
}
