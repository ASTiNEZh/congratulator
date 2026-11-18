package ru.astinezh.congratulator.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.astinezh.congratulator.models.Birthday;
import ru.astinezh.congratulator.models.BirthdayStatus;
import ru.astinezh.congratulator.repositories.BirthdayRepository;
import ru.astinezh.congratulator.support.PostgresContainerConfig;
import ru.astinezh.congratulator.support.TestData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** BirthdayService вместе с настоящим репозиторием и PostgreSQL. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PostgresContainerConfig.class, BirthdayService.class})
@Tag("docker")
@DisplayName("BirthdayService + PostgreSQL")
class BirthdayServiceDbTest {

    @Autowired
    private BirthdayService service;
    @Autowired
    private BirthdayRepository repository;
    @Autowired
    private TestEntityManager em;

    private Birthday save(LocalDate date) {
        return repository.saveAndFlush(TestData.birthday(UUID.randomUUID(), null, date, BirthdayStatus.OTHER));
    }

    @Test
    @DisplayName("findAll: сортировка desc и ограничение размера страницы")
    void findAllDescendingWithPaging() {
        save(LocalDate.of(1995, 5, 5));
        save(LocalDate.of(1990, 1, 1));
        save(LocalDate.of(2000, 12, 31));

        List<Birthday> page = service.findAll(0, 2, "desc");

        assertEquals(List.of(LocalDate.of(2000, 12, 31), LocalDate.of(1995, 5, 5)),
                page.stream().map(Birthday::getBirthDate).toList());
    }

    @Test
    @DisplayName("findAll: сортировка asc, вторая страница")
    void findAllAscendingSecondPage() {
        save(LocalDate.of(1995, 5, 5));
        save(LocalDate.of(1990, 1, 1));
        save(LocalDate.of(2000, 12, 31));

        List<Birthday> page = service.findAll(1, 2, "asc");

        assertEquals(List.of(LocalDate.of(2000, 12, 31)),
                page.stream().map(Birthday::getBirthDate).toList());
    }

    @Test
    @DisplayName("findActual: из базы выбираются только ДР в ближайшие 30 дней")
    void findActualFromDatabase() {
        LocalDate today = LocalDate.now();
        // 20 лет назад — кратно 4, чтобы 29 февраля не «съезжало» на 28-е
        Birthday soon = save(today.plusDays(5).minusYears(20));
        Birthday todayBirthday = save(today.minusYears(20));
        save(today.plusDays(100).minusYears(20));
        save(today.minusDays(3).minusYears(20));

        List<UUID> actual = service.findActual().stream().map(Birthday::getUuid).toList();

        assertEquals(2, actual.size());
        assertTrue(actual.contains(soon.getUuid()));
        assertTrue(actual.contains(todayBirthday.getUuid()));
    }

    @Test
    @DisplayName("update: меняет существующую запись, а не создаёт новую")
    void updateExistingRecord() {
        UUID id = UUID.randomUUID();
        repository.saveAndFlush(TestData.birthday(id, null, LocalDate.of(2000, 1, 1), BirthdayStatus.OTHER));
        em.clear();
        Birthday changes = TestData.birthday(UUID.randomUUID(), null, LocalDate.of(2000, 1, 1), BirthdayStatus.TODAY);

        service.update(id, changes);
        em.flush();
        em.clear();

        assertEquals(1, repository.count());
        assertEquals(BirthdayStatus.TODAY, repository.findById(id).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("findOne: неизвестный uuid -> null")
    void findOneUnknown() {
        assertNull(service.findOne(UUID.randomUUID()));
    }

    @Test
    @DisplayName("delete: запись исчезает из базы")
    void deleteRemovesRecord() {
        Birthday b = save(LocalDate.of(2000, 1, 1));

        service.delete(b.getUuid());
        repository.flush();

        assertNull(service.findOne(b.getUuid()));
    }
}
