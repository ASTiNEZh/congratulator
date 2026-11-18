package ru.astinezh.congratulator.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.astinezh.congratulator.models.Celebrant;
import ru.astinezh.congratulator.support.PostgresContainerConfig;
import ru.astinezh.congratulator.support.TestData;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(PostgresContainerConfig.class)
@Tag("docker")
@DisplayName("CelebrantRepository (настоящий PostgreSQL в Testcontainers)")
class CelebrantRepositoryTest {

    @Autowired
    private CelebrantRepository repository;
    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("Сохранение и чтение всех полей, включая путь к фото")
    void roundTrip() {
        UUID id = UUID.randomUUID();
        Celebrant c = TestData.celebrant(id, "Пётр", "Петрович", "Петров");
        c.setPhotoStoragePath("/data/uploads/photo.png");
        repository.saveAndFlush(c);
        em.clear();

        Celebrant loaded = repository.findById(id).orElseThrow();

        assertEquals("Пётр", loaded.getFirstName());
        assertEquals("Петрович", loaded.getLastName());
        assertEquals("Петров", loaded.getSurname());
        assertEquals("/data/uploads/photo.png", loaded.getPhotoStoragePath());
    }

    @Test
    @DisplayName("Повторное сохранение с тем же uuid обновляет запись, а не дублирует")
    void saveTwiceUpdates() {
        UUID id = UUID.randomUUID();
        repository.saveAndFlush(TestData.celebrant(id, "Иван", "Иванович", "Иванов"));
        em.clear();

        repository.saveAndFlush(TestData.celebrant(id, "Иоанн", "Иванович", "Иванов"));
        em.clear();

        assertEquals(1, repository.count());
        assertEquals("Иоанн", repository.findById(id).orElseThrow().getFirstName());
    }

    @Test
    @DisplayName("Удаление по uuid")
    void deleteById() {
        UUID id = UUID.randomUUID();
        repository.saveAndFlush(TestData.celebrant(id, "Иван", "Иванович", "Иванов"));

        repository.deleteById(id);
        repository.flush();

        assertFalse(repository.existsById(id));
    }

    @Test
    @DisplayName("findAll возвращает всех сохранённых")
    void findAll() {
        repository.saveAndFlush(TestData.celebrant(UUID.randomUUID(), "А", "А", "А"));
        repository.saveAndFlush(TestData.celebrant(UUID.randomUUID(), "Б", "Б", "Б"));

        assertEquals(2, repository.findAll().size());
    }
}
