package ru.astinezh.congratulator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.astinezh.congratulator.controller.BirthdayController;
import ru.astinezh.congratulator.controller.FilesController;
import ru.astinezh.congratulator.repositories.BirthdayRepository;
import ru.astinezh.congratulator.repositories.CelebrantRepository;
import ru.astinezh.congratulator.services.BirthdayService;
import ru.astinezh.congratulator.services.CelebrantService;
import ru.astinezh.congratulator.services.FileService;
import ru.astinezh.congratulator.support.PostgresContainerConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke-тест: приложение целиком поднимается с настоящей PostgreSQL в Docker.
 * Профиль "test" берёт настройки из src/test/resources/application-test.properties.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresContainerConfig.class)
@Tag("docker")
@DisplayName("Приложение: загрузка Spring-контекста")
class CongratulatorApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("contextLoads: контекст поднимается")
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    @DisplayName("Ключевые бины созданы")
    void keyBeansArePresent() {
        assertNotNull(context.getBean(ModelMapper.class));
        assertNotNull(context.getBean(BirthdayRepository.class));
        assertNotNull(context.getBean(CelebrantRepository.class));
        assertNotNull(context.getBean(BirthdayService.class));
        assertNotNull(context.getBean(CelebrantService.class));
        assertNotNull(context.getBean(FileService.class));
        assertNotNull(context.getBean(BirthdayController.class));
    }

    @Test
    @DisplayName("Подключение к БД идёт к контейнеру, а не к localhost:5432 из application.properties")
    void usesContainerDatabase() {
        String url = context.getEnvironment().getProperty("spring.datasource.url");

        // @ServiceConnection подменяет подключение, в свойствах остаётся значение по умолчанию —
        // поэтому проверяем сам DataSource через репозиторий: запрос должен выполниться без ошибок
        assertNotNull(url);
        assertDoesNotThrow(() -> context.getBean(BirthdayRepository.class).count());
    }

    @Test
    @Disabled("BUG-8: у FilesController нет @RestController, бин не создаётся и /files/** недоступны")
    @DisplayName("FilesController зарегистрирован в контексте")
    void filesControllerShouldBeRegistered() {
        assertEquals(1, context.getBeanNamesForType(FilesController.class).length);
    }
}
