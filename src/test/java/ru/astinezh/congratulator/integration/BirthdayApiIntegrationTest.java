package ru.astinezh.congratulator.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.astinezh.congratulator.repositories.BirthdayRepository;
import ru.astinezh.congratulator.repositories.CelebrantRepository;
import ru.astinezh.congratulator.support.PostgresContainerConfig;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Сквозные тесты API: HTTP -> контроллер -> сервис -> JPA -> PostgreSQL (Testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresContainerConfig.class)
@Tag("docker")
@DisplayName("API ДР: сквозные тесты с PostgreSQL")
class BirthdayApiIntegrationTest {

    /** В JSON свойство написано с кириллической «с» (U+0441) — так в openapi.yaml. */
    private static final String CELEBRANT_PROPERTY = "\u0441elebrant";
    /** То же свойство в синтаксисе JsonPath. */
    private static final String CELEBRANT_PATH = "$['" + CELEBRANT_PROPERTY + "']";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BirthdayRepository birthdayRepository;
    @Autowired
    private CelebrantRepository celebrantRepository;

    @AfterEach
    void cleanUp() {
        birthdayRepository.deleteAll();
        celebrantRepository.deleteAll();
    }

    private String body(UUID birthdayId, UUID celebrantId, String firstName, LocalDate date, String status)
            throws Exception {
        ObjectNode celebrant = objectMapper.createObjectNode()
                .put("uuid", celebrantId.toString())
                .put("firstName", firstName)
                .put("lastName", "Иванович")
                .put("surname", "Иванов");
        ObjectNode body = objectMapper.createObjectNode()
                .put("uuid", birthdayId.toString())
                .put("birthDate", date.toString())
                .put("status", status);
        body.set(CELEBRANT_PROPERTY, celebrant);
        return objectMapper.writeValueAsString(body);
    }

    private UUID create(LocalDate date, String status) throws Exception {
        UUID birthdayId = UUID.randomUUID();
        mockMvc.perform(post("/birthdays/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(birthdayId, UUID.randomUUID(), "Иван", date, status)))
                .andExpect(status().isOk());
        return birthdayId;
    }

    @Test
    @DisplayName("POST /birthdays/new сохраняет ДР и именинника в БД")
    void createPersistsData() throws Exception {
        UUID birthdayId = UUID.randomUUID();
        UUID celebrantId = UUID.randomUUID();

        mockMvc.perform(post("/birthdays/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(birthdayId, celebrantId, "Иван", LocalDate.of(2000, 1, 1), "UPCOMING")))
                .andExpect(status().isOk());

        assertTrue(birthdayRepository.existsById(birthdayId));
        assertTrue(celebrantRepository.existsById(celebrantId));
    }

    @Test
    @DisplayName("POST затем GET /birthdays/{uuid}: данные возвращаются без искажений")
    void createThenGet() throws Exception {
        UUID birthdayId = UUID.randomUUID();
        UUID celebrantId = UUID.randomUUID();
        mockMvc.perform(post("/birthdays/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(birthdayId, celebrantId, "Иван", LocalDate.of(2000, 1, 1), "RECENT")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/birthdays/{uuid}", birthdayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(birthdayId.toString()))
                .andExpect(jsonPath("$.birthDate").value("2000-01-01"))
                .andExpect(jsonPath("$.status").value("RECENT"))
                .andExpect(jsonPath(CELEBRANT_PATH + ".uuid").value(celebrantId.toString()))
                .andExpect(jsonPath(CELEBRANT_PATH + ".firstName").value("Иван"))
                .andExpect(jsonPath(CELEBRANT_PATH + ".surname").value("Иванов"));
    }

    @Test
    @DisplayName("GET /birthdays?sort=asc|desc: порядок по дате рождения")
    void listSorted() throws Exception {
        create(LocalDate.of(1995, 5, 5), "OTHER");
        create(LocalDate.of(1990, 1, 1), "OTHER");
        create(LocalDate.of(2000, 12, 31), "OTHER");

        mockMvc.perform(get("/birthdays").param("sort", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$[2].birthDate").value("2000-12-31"));

        mockMvc.perform(get("/birthdays").param("sort", "desc"))
                .andExpect(jsonPath("$[0].birthDate").value("2000-12-31"))
                .andExpect(jsonPath("$[2].birthDate").value("1990-01-01"));
    }

    @Test
    @DisplayName("GET /birthdays?page&size: пагинация")
    void listPaged() throws Exception {
        create(LocalDate.of(1995, 5, 5), "OTHER");
        create(LocalDate.of(1990, 1, 1), "OTHER");
        create(LocalDate.of(2000, 12, 31), "OTHER");

        mockMvc.perform(get("/birthdays").param("page", "1").param("size", "2").param("sort", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].birthDate").value("2000-12-31"));
    }

    @Test
    @DisplayName("PUT /birthdays/{uuid}: обновляет ДР и данные именинника")
    void update() throws Exception {
        UUID birthdayId = UUID.randomUUID();
        UUID celebrantId = UUID.randomUUID();
        mockMvc.perform(post("/birthdays/new").contentType(MediaType.APPLICATION_JSON)
                        .content(body(birthdayId, celebrantId, "Иван", LocalDate.of(2000, 1, 1), "OTHER")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/birthdays/{uuid}", birthdayId).contentType(MediaType.APPLICATION_JSON)
                        .content(body(birthdayId, celebrantId, "Иоанн", LocalDate.of(2001, 2, 2), "TODAY")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/birthdays/{uuid}", birthdayId))
                .andExpect(jsonPath("$.status").value("TODAY"))
                .andExpect(jsonPath("$.birthDate").value("2001-02-02"))
                .andExpect(jsonPath(CELEBRANT_PATH + ".firstName").value("Иоанн"));
        assertEquals(1, birthdayRepository.count());
        assertEquals(1, celebrantRepository.count());
    }

    @Test
    @DisplayName("DELETE /birthdays/{uuid}: ДР удаляется, именинник остаётся")
    void delete() throws Exception {
        UUID birthdayId = UUID.randomUUID();
        UUID celebrantId = UUID.randomUUID();
        mockMvc.perform(post("/birthdays/new").contentType(MediaType.APPLICATION_JSON)
                        .content(body(birthdayId, celebrantId, "Иван", LocalDate.of(2000, 1, 1), "OTHER")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/birthdays/{uuid}", birthdayId)).andExpect(status().isOk());

        assertFalse(birthdayRepository.existsById(birthdayId));
        assertTrue(celebrantRepository.existsById(celebrantId));
    }

    @Test
    @DisplayName("GET /: только ДР в ближайшие 30 дней")
    void actualBirthdays() throws Exception {
        LocalDate today = LocalDate.now();
        // 20 лет назад — кратно 4, чтобы 29 февраля не «съезжало» на 28-е
        UUID soon = create(today.plusDays(5).minusYears(20), "UPCOMING");
        UUID todayId = create(today.minusYears(20), "TODAY");
        create(today.plusDays(100).minusYears(20), "OTHER");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.uuid=='" + soon + "')]").exists())
                .andExpect(jsonPath("$[?(@.uuid=='" + todayId + "')]").exists());
    }

    @Test
    @DisplayName("Валидация работает в настоящем контексте: пустое тело -> 400")
    void validationRejectsEmptyBody() throws Exception {
        mockMvc.perform(post("/birthdays/new").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertEquals(0, birthdayRepository.count());
    }

    @Test
    @DisplayName("Некорректный uuid в пути -> 400")
    void invalidUuid() throws Exception {
        mockMvc.perform(get("/birthdays/{uuid}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Swagger: /v3/api-docs доступен и описывает /birthdays")
    void apiDocsAreAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/birthdays']").exists());
    }

    // ------------------------------------------------------------ известные дефекты

    @Test
    @Disabled("BUG-1: GET /birthdays без параметра sort падает с NullPointerException (sortBy.equals(...))")
    @DisplayName("GET /birthdays без sort -> 200")
    void listWithoutSortShouldWork() throws Exception {
        create(LocalDate.of(1990, 1, 1), "OTHER");

        mockMvc.perform(get("/birthdays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Disabled("BUG-7: для несуществующего uuid контроллер отвечает 500 вместо 404")
    @DisplayName("GET /birthdays/{uuid} для несуществующей записи -> 404")
    void unknownBirthdayShouldBe404() throws Exception {
        mockMvc.perform(get("/birthdays/{uuid}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Disabled("BUG-8: FilesController не зарегистрирован (нет @RestController), поэтому /files/upload отвечает 404")
    @DisplayName("Загрузка и скачивание фото именинника")
    void uploadAndDownloadPhoto() throws Exception {
        UUID birthdayId = UUID.randomUUID();
        UUID celebrantId = UUID.randomUUID();
        mockMvc.perform(post("/birthdays/new").contentType(MediaType.APPLICATION_JSON)
                        .content(body(birthdayId, celebrantId, "Иван", LocalDate.of(2000, 1, 1), "OTHER")))
                .andExpect(status().isOk());
        byte[] photo = "PNGDATA".getBytes();

        mockMvc.perform(multipart("/files/upload")
                        .file(new MockMultipartFile("files", "photo.png", "image/png", photo))
                        .param("uuid", celebrantId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/files/{id}", celebrantId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(photo));
    }
}
