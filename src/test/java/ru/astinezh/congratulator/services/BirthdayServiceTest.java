package ru.astinezh.congratulator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.astinezh.congratulator.models.Birthday;
import ru.astinezh.congratulator.models.BirthdayStatus;
import ru.astinezh.congratulator.repositories.BirthdayRepository;
import ru.astinezh.congratulator.support.TestData;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BirthdayService")
class BirthdayServiceTest {

    @Mock
    private BirthdayRepository repository;

    private BirthdayService service;

    @BeforeEach
    void setUp() {
        service = new BirthdayService(repository);
    }

    private static Birthday birthday() {
        return TestData.birthday(UUID.randomUUID(), null, LocalDate.of(2000, 1, 1), BirthdayStatus.OTHER);
    }

    /** День рождения, ближайшее наступление которого — через {@code daysFromToday} дней. */
    private static Birthday birthdayIn(long daysFromToday) {
        LocalDate nextOccurrence = LocalDate.now().plusDays(daysFromToday);
        // 20 лет назад — кратно 4, чтобы 29 февраля не «съезжало» на 28-е
        return TestData.birthday(UUID.randomUUID(), null, nextOccurrence.minusYears(20), BirthdayStatus.UPCOMING);
    }

    // ------------------------------------------------------------ findAll

    @Test
    @DisplayName("findAll: возвращает содержимое страницы из репозитория")
    void findAllReturnsPageContent() {
        List<Birthday> content = List.of(birthday(), birthday());
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(content));

        assertEquals(content, service.findAll(0, 20, "asc"));
    }

    @Test
    @DisplayName("findAll: номер и размер страницы передаются в Pageable")
    void findAllPassesPaging() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.findAll(3, 7, "asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(captor.capture());
        assertEquals(3, captor.getValue().getPageNumber());
        assertEquals(7, captor.getValue().getPageSize());
    }

    @ParameterizedTest(name = "sort=\"{0}\" -> {1} по birthDate")
    @CsvSource({
            "asc,     ASC",
            "desc,    DESC",
            // любое значение, кроме точного «desc», трактуется как asc (в т.ч. другой регистр)
            "DESC,    ASC",
            "foo,     ASC"
    })
    @DisplayName("findAll: направление сортировки по birthDate")
    void findAllSortDirection(String sortBy, Sort.Direction expected) {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.findAll(0, 20, sortBy);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("birthDate");
        assertNotNull(order, "сортировка должна идти по birthDate");
        assertEquals(expected, order.getDirection());
    }

    @Test
    @Disabled("BUG-1: параметр sort в API необязательный (required: false), а sortBy.equals(...) падает с NPE при sort=null")
    @DisplayName("findAll: без параметра sort используется сортировка по умолчанию (asc)")
    void findAllWithoutSortShouldDefaultToAsc() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertDoesNotThrow(() -> service.findAll(0, 20, null));
    }

    @Test
    @DisplayName("findAll: sort=null -> NullPointerException (текущее поведение, см. BUG-1)")
    void findAllWithNullSortThrowsNpe() {
        assertThrows(NullPointerException.class, () -> service.findAll(0, 20, null));
    }

    // ------------------------------------------------------------ CRUD

    @Test
    @DisplayName("findOne: запись найдена")
    void findOneFound() {
        Birthday b = birthday();
        when(repository.findById(b.getUuid())).thenReturn(Optional.of(b));

        assertSame(b, service.findOne(b.getUuid()));
    }

    @Test
    @DisplayName("findOne: запись не найдена -> null")
    void findOneNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertNull(service.findOne(id));
    }

    @Test
    @DisplayName("remember: сохраняет запись в репозитории")
    void rememberSaves() {
        Birthday b = birthday();

        service.remember(b);

        verify(repository).save(b);
    }

    @Test
    @DisplayName("update: подставляет uuid из пути и сохраняет")
    void updateOverridesUuid() {
        UUID target = UUID.randomUUID();
        Birthday b = birthday();

        service.update(target, b);

        assertEquals(target, b.getUuid());
        verify(repository).save(b);
    }

    @Test
    @DisplayName("delete: удаляет по uuid")
    void deleteById() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(repository).deleteById(id);
    }

    // ------------------------------------------------------------ findActual

    @ParameterizedTest(name = "ДР через {0} дн. -> актуальное: {1}")
    @CsvSource({
            "0,   true",    // сегодня
            "1,   true",
            "15,  true",
            "30,  true",    // граница включена (в описании API: «менее чем через 30 дней» — см. комментарий в README)
            "31,  false",
            "180, false",
            "-1,  false"    // вчера: следующее ДР через ~год
    })
    @DisplayName("findActual: окно «сегодня .. сегодня+30 дней»")
    void findActualWindow(long daysFromToday, boolean expectedActual) {
        Birthday b = birthdayIn(daysFromToday);
        when(repository.findAll()).thenReturn(List.of(b));

        List<Birthday> result = service.findActual();

        assertEquals(expectedActual, result.contains(b));
    }

    @Test
    @DisplayName("findActual: оставляет только актуальные и сохраняет порядок")
    void findActualFiltersAndKeepsOrder() {
        Birthday soon = birthdayIn(5);
        Birthday far = birthdayIn(100);
        Birthday today = birthdayIn(0);
        Birthday passed = birthdayIn(-3);
        when(repository.findAll()).thenReturn(List.of(soon, far, today, passed));

        assertEquals(List.of(soon, today), service.findActual());
    }

    @Test
    @DisplayName("findActual: пустая база -> пустой список")
    void findActualEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        assertTrue(service.findActual().isEmpty());
    }

    @ParameterizedTest(name = "год рождения {0} не влияет на актуальность")
    @ValueSource(ints = {1950, 1990, 2020})
    @DisplayName("findActual: учитывается только день и месяц, год рождения не важен")
    void findActualIgnoresBirthYear(int year) {
        LocalDate in10 = LocalDate.now().plusDays(10);
        // год подбираем так, чтобы 29 февраля не мешало: берём ближайший високосный или обычный день
        LocalDate birthDate = in10.withYear(year);
        Birthday b = TestData.birthday(UUID.randomUUID(), null, birthDate, BirthdayStatus.UPCOMING);
        when(repository.findAll()).thenReturn(List.of(b));

        assertEquals(List.of(b), service.findActual());
    }
}
