package ru.astinezh.congratulator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.astinezh.congratulator.models.Birthday;
import ru.astinezh.congratulator.models.BirthdayStatus;
import ru.astinezh.congratulator.models.Celebrant;
import ru.astinezh.congratulator.services.BirthdayService;
import ru.astinezh.congratulator.services.CelebrantService;
import ru.astinezh.congratulator.support.TestData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-уровень без базы данных: контроллер + настоящий ModelMapper + замоканные сервисы.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BirthdayController (REST API)")
class BirthdayControllerTest {

    private static final UUID BIRTHDAY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CELEBRANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000000");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private BirthdayService birthdayService;
    @Mock
    private CelebrantService celebrantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BirthdayController controller = new BirthdayController(birthdayService, celebrantService, new ModelMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    /**
     * Свойство «celebrant» в JSON написано с кириллической «с» (U+0441) — так в openapi.yaml.
     * Тело запроса собирается как JSON-дерево, чтобы тесты не зависели от сгенерированных DTO-классов.
     */
    private static final String CELEBRANT_PROPERTY = "\u0441elebrant";

    private ObjectNode validBody() {
        ObjectNode celebrant = objectMapper.createObjectNode()
                .put("uuid", CELEBRANT_ID.toString())
                .put("firstName", "Иван")
                .put("lastName", "Иванович")
                .put("surname", "Иванов");
        ObjectNode body = objectMapper.createObjectNode()
                .put("uuid", BIRTHDAY_ID.toString())
                .put("birthDate", "2000-01-01")
                .put("status", "UPCOMING");
        body.set(CELEBRANT_PROPERTY, celebrant);
        return body;
    }

    private Birthday entity(UUID id, BirthdayStatus status) {
        Celebrant celebrant = TestData.celebrant(CELEBRANT_ID, "Иван", "Иванович", "Иванов");
        return TestData.birthday(id, celebrant, LocalDate.of(2000, 1, 1), status);
    }

    // ------------------------------------------------------------ POST /birthdays/new

    @Test
    @DisplayName("POST /birthdays/new: 200 и сохранение именинника и ДР")
    void rememberBirthdayOk() throws Exception {
        mockMvc.perform(post("/birthdays/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk());

        ArgumentCaptor<Celebrant> celebrantCaptor = ArgumentCaptor.forClass(Celebrant.class);
        verify(celebrantService).remember(celebrantCaptor.capture());
        assertEquals(CELEBRANT_ID, celebrantCaptor.getValue().getUuid());
        assertEquals("Иван", celebrantCaptor.getValue().getFirstName());
        assertEquals("Иванов", celebrantCaptor.getValue().getSurname());

        ArgumentCaptor<Birthday> birthdayCaptor = ArgumentCaptor.forClass(Birthday.class);
        verify(birthdayService).remember(birthdayCaptor.capture());
        assertEquals(BIRTHDAY_ID, birthdayCaptor.getValue().getUuid());
        assertEquals(LocalDate.of(2000, 1, 1), birthdayCaptor.getValue().getBirthDate());
        assertEquals(BirthdayStatus.UPCOMING, birthdayCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("POST /birthdays/new: пустое тело {} -> 400 (uuid и status обязательны)")
    void rememberBirthdayMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/birthdays/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(birthdayService, celebrantService);
    }

    @Test
    @DisplayName("POST /birthdays/new: неизвестный статус -> 400")
    void rememberBirthdayUnknownStatus() throws Exception {
        String body = "{\"uuid\":\"" + BIRTHDAY_ID + "\",\"status\":\"WHATEVER\"}";

        mockMvc.perform(post("/birthdays/new").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(birthdayService, celebrantService);
    }

    @Test
    @DisplayName("POST /birthdays/new: невалидный JSON -> 400")
    void rememberBirthdayMalformedJson() throws Exception {
        mockMvc.perform(post("/birthdays/new").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /birthdays/new: невалидная дата рождения -> 400")
    void rememberBirthdayBadDate() throws Exception {
        String body = "{\"uuid\":\"" + BIRTHDAY_ID + "\",\"status\":\"TODAY\",\"birthDate\":\"31-12-2000\"}";

        mockMvc.perform(post("/birthdays/new").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /birthdays/new: не JSON (text/plain) -> 415")
    void rememberBirthdayWrongContentType() throws Exception {
        mockMvc.perform(post("/birthdays/new").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("/birthdays/new: PUT по адресу создания не приводит к успешному ответу")
    void rememberBirthdayWrongMethod() throws Exception {
        int code = mockMvc.perform(put("/birthdays/new")).andReturn().getResponse().getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(200, code);
    }

    @Test
    @Disabled("BUG-4: поле сelebrant в схеме необязательное, но без него контроллер падает (modelMapper.map(null) -> 500)")
    @DisplayName("POST /birthdays/new без именинника -> 200")
    void rememberBirthdayWithoutCelebrantShouldWork() throws Exception {
        String body = "{\"uuid\":\"" + BIRTHDAY_ID + "\",\"status\":\"OTHER\"}";

        mockMvc.perform(post("/birthdays/new").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ GET /birthdays

    @Test
    @DisplayName("GET /birthdays: значения по умолчанию page=0, size=20, sort не передаётся")
    void getListDefaults() throws Exception {
        when(birthdayService.findAll(eq(0), eq(20), isNull()))
                .thenReturn(List.of(entity(BIRTHDAY_ID, BirthdayStatus.TODAY)));

        mockMvc.perform(get("/birthdays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].uuid").value(BIRTHDAY_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("TODAY"))
                .andExpect(jsonPath("$[0].birthDate").value("2000-01-01"));
    }

    @Test
    @DisplayName("GET /birthdays?page&size&sort: параметры доходят до сервиса")
    void getListPassesParameters() throws Exception {
        when(birthdayService.findAll(1, 5, "desc")).thenReturn(List.of());

        mockMvc.perform(get("/birthdays").param("page", "1").param("size", "5").param("sort", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(birthdayService).findAll(1, 5, "desc");
    }

    @Test
    @DisplayName("GET /birthdays: нечисловой page -> 400")
    void getListInvalidPage() throws Exception {
        mockMvc.perform(get("/birthdays").param("page", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(birthdayService);
    }

    // ------------------------------------------------------------ GET /birthdays/{uuid}

    @Test
    @DisplayName("GET /birthdays/{uuid}: найденная запись возвращается в JSON")
    void getBirthdayFound() throws Exception {
        when(birthdayService.findOne(BIRTHDAY_ID)).thenReturn(entity(BIRTHDAY_ID, BirthdayStatus.RECENT));

        mockMvc.perform(get("/birthdays/{uuid}", BIRTHDAY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(BIRTHDAY_ID.toString()))
                .andExpect(jsonPath("$.status").value("RECENT"));
    }

    @Test
    @DisplayName("GET /birthdays/{uuid}: некорректный uuid -> 400")
    void getBirthdayInvalidUuid() throws Exception {
        mockMvc.perform(get("/birthdays/{uuid}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(birthdayService);
    }

    @Test
    @Disabled("BUG-7: для несуществующего uuid сервис возвращает null, modelMapper.map(null) даёт 500 вместо 404")
    @DisplayName("GET /birthdays/{uuid}: запись не найдена -> 404")
    void getBirthdayNotFoundShouldBe404() throws Exception {
        when(birthdayService.findOne(BIRTHDAY_ID)).thenReturn(null);

        mockMvc.perform(get("/birthdays/{uuid}", BIRTHDAY_ID))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ PUT /birthdays/{uuid}

    @Test
    @DisplayName("PUT /birthdays/{uuid}: обновляет именинника и ДР")
    void updateBirthday() throws Exception {
        UUID pathId = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

        mockMvc.perform(put("/birthdays/{uuid}", pathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk());

        ArgumentCaptor<Celebrant> celebrantCaptor = ArgumentCaptor.forClass(Celebrant.class);
        verify(celebrantService).update(eq(CELEBRANT_ID), celebrantCaptor.capture());
        assertEquals("Иван", celebrantCaptor.getValue().getFirstName());

        ArgumentCaptor<Birthday> birthdayCaptor = ArgumentCaptor.forClass(Birthday.class);
        verify(birthdayService).update(eq(pathId), birthdayCaptor.capture());
        assertEquals(BirthdayStatus.UPCOMING, birthdayCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("PUT /birthdays/{uuid}: пустое тело -> 400")
    void updateBirthdayInvalidBody() throws Exception {
        mockMvc.perform(put("/birthdays/{uuid}", BIRTHDAY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(birthdayService, celebrantService);
    }

    // ------------------------------------------------------------ DELETE /birthdays/{uuid}

    @Test
    @DisplayName("DELETE /birthdays/{uuid}: 200 и удаление")
    void deleteBirthday() throws Exception {
        mockMvc.perform(delete("/birthdays/{uuid}", BIRTHDAY_ID))
                .andExpect(status().isOk());

        verify(birthdayService).delete(BIRTHDAY_ID);
    }

    @Test
    @DisplayName("DELETE /birthdays/{uuid}: некорректный uuid -> 400")
    void deleteBirthdayInvalidUuid() throws Exception {
        mockMvc.perform(delete("/birthdays/{uuid}", "123"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(birthdayService);
    }

    // ------------------------------------------------------------ GET /

    @Test
    @DisplayName("GET /: возвращает актуальные ДР")
    void getActualBirthdays() throws Exception {
        when(birthdayService.findActual()).thenReturn(List.of(
                entity(BIRTHDAY_ID, BirthdayStatus.UPCOMING),
                entity(UUID.fromString("00000000-0000-0000-0000-000000000002"), BirthdayStatus.TODAY)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("UPCOMING"))
                .andExpect(jsonPath("$[1].status").value("TODAY"));
    }

    @Test
    @DisplayName("GET /: нет актуальных ДР -> пустой массив")
    void getActualBirthdaysEmpty() throws Exception {
        when(birthdayService.findActual()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
