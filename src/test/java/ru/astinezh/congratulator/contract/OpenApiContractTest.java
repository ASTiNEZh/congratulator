package ru.astinezh.congratulator.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import ru.astinezh.congratulator.models.BirthdayStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверки согласованности артефактов проекта: openapi.yaml, SQL-миграции и Java-кода.
 * Запускаются без Spring-контекста и без базы данных.
 */
@DisplayName("Контракт: openapi.yaml, SQL-миграция и модель")
class OpenApiContractTest {

    private static final Set<String> ALLOWED_MEDIA_TYPES =
            Set.of("application/json", "multipart/form-data", "application/octet-stream");

    private static JsonNode spec;

    @BeforeAll
    static void loadSpec() throws IOException {
        try (InputStream in = new ClassPathResource("openapi.yaml").getInputStream()) {
            spec = new ObjectMapper(new YAMLFactory()).readTree(in);
        }
    }

    private static String readResource(String name) throws IOException {
        try (InputStream in = new ClassPathResource(name).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void collect(JsonNode node, String key, List<String> out) {
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                if (e.getKey().equals(key) && e.getValue().isTextual()) {
                    out.add(e.getValue().asText());
                }
                collect(e.getValue(), key, out);
            });
        } else if (node.isArray()) {
            node.forEach(n -> collect(n, key, out));
        }
    }

    // ------------------------------------------------------------ структура

    @Test
    @DisplayName("Спецификация OpenAPI 3.0.x и содержит заголовок и сервер")
    void specHeader() {
        assertTrue(spec.path("openapi").asText().startsWith("3.0"));
        assertEquals("congratulator", spec.path("info").path("title").asText());
        assertEquals("http://localhost:8081", spec.path("servers").path(0).path("url").asText());
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "post,   /birthdays/new",
            "get,    /",
            "get,    /birthdays",
            "get,    /birthdays/{uuid}",
            "put,    /birthdays/{uuid}",
            "delete, /birthdays/{uuid}",
            "post,   /files/upload",
            "get,    /files/{id}"
    })
    @DisplayName("Все ожидаемые эндпоинты описаны в спецификации")
    void endpointsAreDescribed(String method, String path) {
        assertFalse(spec.path("paths").path(path).path(method).isMissingNode(),
                method + " " + path + " отсутствует в openapi.yaml");
    }

    @Test
    @DisplayName("operationId уникальны")
    void operationIdsAreUnique() {
        List<String> ids = new ArrayList<>();
        collect(spec.path("paths"), "operationId", ids);

        assertFalse(ids.isEmpty());
        assertEquals(ids.size(), new HashSet<>(ids).size(), "повторяющиеся operationId: " + ids);
    }

    @Test
    @DisplayName("Все $ref ведут на существующие элементы спецификации")
    void allReferencesResolve() {
        List<String> refs = new ArrayList<>();
        collect(spec, "$ref", refs);

        assertFalse(refs.isEmpty());
        for (String ref : refs) {
            assertTrue(ref.startsWith("#/"), "внешние ссылки не ожидаются: " + ref);
            assertFalse(spec.at(ref.substring(1)).isMissingNode(), "битая ссылка: " + ref);
        }
    }

    // ------------------------------------------------------------ согласованность с кодом

    @Test
    @DisplayName("Статусы ДР в спецификации совпадают с enum BirthdayStatus")
    void statusEnumMatchesJavaEnum() {
        Set<String> inSpec = new TreeSet<>();
        spec.at("/components/schemas/BirthdayDto/properties/status/enum").forEach(n -> inSpec.add(n.asText()));

        Set<String> inJava = Stream.of(BirthdayStatus.values()).map(Enum::name).collect(Collectors.toCollection(TreeSet::new));

        assertEquals(inJava, inSpec);
    }

    @Test
    @DisplayName("Параметр sort допускает только asc и desc")
    void sortParameterEnum() {
        List<String> values = new ArrayList<>();
        spec.at("/components/parameters/sort/schema/enum").forEach(n -> values.add(n.asText()));

        assertEquals(List.of("asc", "desc"), values);
    }

    @Test
    @DisplayName("Параметры пагинации: page >= 0 (по умолчанию 0), size >= 1 (по умолчанию 20)")
    void paginationParameters() {
        JsonNode page = spec.at("/components/parameters/page/schema");
        JsonNode size = spec.at("/components/parameters/size/schema");

        assertEquals(0, page.path("minimum").asInt());
        assertEquals(0, page.path("default").asInt());
        assertEquals(1, size.path("minimum").asInt());
        assertEquals(20, size.path("default").asInt());
    }

    @Test
    @DisplayName("uuid и status — обязательные поля BirthdayDto")
    void requiredFields() {
        List<String> required = new ArrayList<>();
        spec.at("/components/schemas/BirthdayDto/required").forEach(n -> required.add(n.asText()));

        assertTrue(required.containsAll(List.of("uuid", "status")));
    }

    // ------------------------------------------------------------ известные дефекты

    @Test
    @Disabled("BUG-9: опечатка в openapi.yaml — media type «mapplication/json» в ответе GET /")
    @DisplayName("Все media type в спецификации допустимы")
    void mediaTypesAreValid() {
        List<String> unexpected = new ArrayList<>();
        collectMediaTypes(spec.path("paths"), unexpected);

        assertTrue(unexpected.isEmpty(), "неизвестные media type: " + unexpected);
    }

    private static void collectMediaTypes(JsonNode node, List<String> unexpected) {
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                if (e.getKey().equals("content") && e.getValue().isObject()) {
                    e.getValue().fieldNames().forEachRemaining(mt -> {
                        if (!ALLOWED_MEDIA_TYPES.contains(mt)) {
                            unexpected.add(mt);
                        }
                    });
                }
                collectMediaTypes(e.getValue(), unexpected);
            });
        } else if (node.isArray()) {
            node.forEach(n -> collectMediaTypes(n, unexpected));
        }
    }

    @Test
    @Disabled("BUG-10: в forMigration.sql enum birthday_status = PLANNED/CELEBRATED/MISSED, а в коде и API — OTHER/RECENT/TODAY/UPCOMING")
    @DisplayName("SQL-миграция: значения birthday_status совпадают с BirthdayStatus")
    void sqlEnumMatchesJavaEnum() throws IOException {
        Matcher m = Pattern.compile("CREATE TYPE birthday_status AS ENUM \\(([^)]*)\\)", Pattern.CASE_INSENSITIVE)
                .matcher(readResource("forMigration.sql"));
        assertTrue(m.find(), "не найден CREATE TYPE birthday_status");

        Set<String> inSql = Stream.of(m.group(1).split(","))
                .map(s -> s.trim().replace("'", ""))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> inJava = Stream.of(BirthdayStatus.values()).map(Enum::name).collect(Collectors.toCollection(TreeSet::new));

        assertEquals(inJava, inSql);
    }

    @Test
    @Disabled("BUG-11: forMigration.sql ссылается на таблицу CELEBRANTS, но не создаёт её")
    @DisplayName("SQL-миграция создаёт таблицу CELEBRANTS, на которую ссылается внешний ключ")
    void sqlCreatesCelebrantsTable() throws IOException {
        String sql = readResource("forMigration.sql").toUpperCase(Locale.ROOT);

        assertTrue(sql.contains("CREATE TABLE CELEBRANTS"));
    }
}
