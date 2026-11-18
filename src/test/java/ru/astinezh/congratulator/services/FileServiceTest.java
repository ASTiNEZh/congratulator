package ru.astinezh.congratulator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import ru.astinezh.congratulator.models.Celebrant;
import ru.astinezh.congratulator.support.TestData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService")
class FileServiceTest {

    private static final String STORED_NAME_PATTERN = "[0-9a-f\\-]{36}_%s";

    @TempDir
    Path storage;

    @Mock
    private CelebrantService celebrantService;

    private FileService fileService;
    private UUID celebrantId;
    private Celebrant celebrant;

    @BeforeEach
    void setUp() {
        fileService = new FileService(storage.toString(), celebrantService);
        celebrantId = UUID.randomUUID();
        celebrant = TestData.celebrant(celebrantId, "Иван", "Иванович", "Иванов");
    }

    private static MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("files", name, "image/png", content.getBytes(StandardCharsets.UTF_8));
    }

    private long filesInStorage() throws IOException {
        try (Stream<Path> s = Files.list(storage)) {
            return s.count();
        }
    }

    // ------------------------------------------------------------ конструктор

    @Test
    @DisplayName("Конструктор создаёт каталог хранилища, включая вложенные")
    void constructorCreatesDirectories() {
        Path nested = storage.resolve("a").resolve("b");

        new FileService(nested.toString(), celebrantService);

        assertTrue(Files.isDirectory(nested));
    }

    @Test
    @DisplayName("Конструктор: каталог создать нельзя -> RuntimeException")
    void constructorFailsWhenDirectoryCannotBeCreated() throws IOException {
        Path regularFile = Files.createFile(storage.resolve("blocker"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new FileService(regularFile.resolve("sub").toString(), celebrantService));

        assertTrue(ex.getMessage().startsWith("Cannot create storage folder"), ex.getMessage());
    }

    // ------------------------------------------------------------ saveFiles

    @Test
    @DisplayName("saveFiles: файл записывается на диск с UUID-префиксом")
    void saveFilesWritesFile() throws IOException {
        when(celebrantService.findOne(celebrantId)).thenReturn(celebrant);

        List<String> names = fileService.saveFiles(List.of(file("photo.png", "PNGDATA")), celebrantId);

        assertEquals(1, names.size());
        assertTrue(names.get(0).matches(String.format(STORED_NAME_PATTERN, "photo\\.png")), names.get(0));
        assertEquals("PNGDATA", Files.readString(storage.resolve(names.get(0))));
    }

    @Test
    @DisplayName("saveFiles: путь к фото сохраняется у именинника")
    void saveFilesUpdatesCelebrantPhotoPath() throws IOException {
        when(celebrantService.findOne(celebrantId)).thenReturn(celebrant);

        List<String> names = fileService.saveFiles(List.of(file("photo.png", "x")), celebrantId);

        assertEquals(storage.toAbsolutePath().resolve(names.get(0)).toString(), celebrant.getPhotoStoragePath());
        verify(celebrantService).update(eq(celebrantId), same(celebrant));
    }

    @Test
    @DisplayName("saveFiles: несколько файлов получают разные имена, фото = последний файл")
    void saveFilesMultiple() throws IOException {
        when(celebrantService.findOne(celebrantId)).thenReturn(celebrant);

        List<String> names = fileService.saveFiles(
                List.of(file("a.png", "AAA"), file("b.png", "BBB")), celebrantId);

        assertEquals(2, names.size());
        assertNotEquals(names.get(0), names.get(1));
        assertEquals(2, filesInStorage());
        assertEquals(storage.toAbsolutePath().resolve(names.get(1)).toString(), celebrant.getPhotoStoragePath());
    }

    @Test
    @DisplayName("saveFiles: файлы с одинаковым именем не перезаписывают друг друга")
    void saveFilesSameNameTwice() throws IOException {
        when(celebrantService.findOne(celebrantId)).thenReturn(celebrant);

        List<String> first = fileService.saveFiles(List.of(file("same.png", "1")), celebrantId);
        List<String> second = fileService.saveFiles(List.of(file("same.png", "2")), celebrantId);

        assertNotEquals(first.get(0), second.get(0));
        assertEquals(2, filesInStorage());
    }

    @Test
    @DisplayName("saveFiles: пустой список -> ничего не делает")
    void saveFilesEmptyList() throws IOException {
        List<String> names = fileService.saveFiles(List.of(), celebrantId);

        assertTrue(names.isEmpty());
        assertEquals(0, filesInStorage());
        verifyNoInteractions(celebrantService);
    }

    @Test
    @DisplayName("saveFiles: именинник не найден -> NullPointerException, файл не создаётся (текущее поведение)")
    void saveFilesUnknownCelebrant() throws IOException {
        when(celebrantService.findOne(celebrantId)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> fileService.saveFiles(List.of(file("photo.png", "x")), celebrantId));
        assertEquals(0, filesInStorage());
    }

    // ------------------------------------------------------------ getFileAsResource

    @Test
    @DisplayName("getFileAsResource: отдаёт содержимое сохранённого фото")
    void getFileAsResourceReturnsSavedContent() throws IOException {
        when(celebrantService.findOne(celebrantId)).thenReturn(celebrant);
        fileService.saveFiles(List.of(file("photo.png", "PNGDATA")), celebrantId);

        ByteArrayResource resource = fileService.getFileAsResource(celebrantId);

        assertEquals("PNGDATA", new String(resource.getByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getFileAsResource: относительное имя ищется в каталоге хранилища")
    void getFileAsResourceResolvesRelativeName() throws IOException {
        Files.writeString(storage.resolve("direct.txt"), "hello");
        celebrant.setPhotoStoragePath("direct.txt");
        when(celebrantService.findOne(celebrantId)).thenReturn(celebrant);

        assertEquals("hello", new String(fileService.getFileAsResource(celebrantId).getByteArray(),
                StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getFileAsResource: файла нет на диске -> RuntimeException «File not found»")
    void getFileAsResourceMissingFile() {
        celebrant.setPhotoStoragePath("missing.png");
        when(celebrantService.findOne(celebrantId)).thenReturn(celebrant);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> fileService.getFileAsResource(celebrantId));

        assertTrue(ex.getMessage().contains("File not found"), ex.getMessage());
        assertTrue(ex.getMessage().contains("missing.png"), ex.getMessage());
    }

    @Test
    @DisplayName("getFileAsResource: именинник не найден -> NullPointerException (текущее поведение)")
    void getFileAsResourceUnknownCelebrant() {
        when(celebrantService.findOne(celebrantId)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> fileService.getFileAsResource(celebrantId));
    }

    @Test
    @DisplayName("getFileAsResource: у именинника нет фото -> NullPointerException (текущее поведение)")
    void getFileAsResourceNoPhoto() {
        when(celebrantService.findOne(celebrantId)).thenReturn(celebrant);

        assertThrows(NullPointerException.class, () -> fileService.getFileAsResource(celebrantId));
    }
}
