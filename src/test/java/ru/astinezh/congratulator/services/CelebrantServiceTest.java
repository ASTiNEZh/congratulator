package ru.astinezh.congratulator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.astinezh.congratulator.models.Celebrant;
import ru.astinezh.congratulator.repositories.CelebrantRepository;
import ru.astinezh.congratulator.support.TestData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CelebrantService")
class CelebrantServiceTest {

    @Mock
    private CelebrantRepository repository;

    private CelebrantService service;

    @BeforeEach
    void setUp() {
        service = new CelebrantService(repository);
    }

    private static Celebrant celebrant() {
        return TestData.celebrant(UUID.randomUUID(), "Иван", "Иванович", "Иванов");
    }

    @Test
    @DisplayName("findAll: возвращает всех именинников")
    void findAll() {
        List<Celebrant> all = List.of(celebrant(), celebrant());
        when(repository.findAll()).thenReturn(all);

        assertEquals(all, service.findAll());
    }

    @Test
    @DisplayName("findOne: найден")
    void findOneFound() {
        Celebrant c = celebrant();
        when(repository.findById(c.getUuid())).thenReturn(Optional.of(c));

        assertSame(c, service.findOne(c.getUuid()));
    }

    @Test
    @DisplayName("findOne: не найден -> null")
    void findOneNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertNull(service.findOne(id));
    }

    @Test
    @DisplayName("remember: сохраняет именинника")
    void remember() {
        Celebrant c = celebrant();

        service.remember(c);

        verify(repository).save(c);
    }

    @Test
    @DisplayName("update: подставляет uuid и сохраняет")
    void update() {
        UUID target = UUID.randomUUID();
        Celebrant c = celebrant();

        service.update(target, c);

        assertEquals(target, c.getUuid());
        verify(repository).save(c);
    }

    @Test
    @DisplayName("delete: удаляет по uuid")
    void delete() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(repository).deleteById(id);
    }
}
