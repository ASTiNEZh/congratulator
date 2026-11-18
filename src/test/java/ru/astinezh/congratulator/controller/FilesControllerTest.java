package ru.astinezh.congratulator.controller;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.astinezh.congratulator.services.FileService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilesController (REST API)")
class FilesControllerTest {

    private static final UUID ID = UUID.fromString("10000000-0000-0000-0000-000000000000");

    @Mock
    private FileService fileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FilesController(fileService)).build();
    }

    @Test
    @DisplayName("GET /files/{id}: возвращает байты файла как application/octet-stream")
    void downloadFile() throws Exception {
        byte[] bytes = "PNGDATA".getBytes(StandardCharsets.UTF_8);
        when(fileService.getFileAsResource(ID)).thenReturn(new ByteArrayResource(bytes));

        mockMvc.perform(get("/files/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @DisplayName("GET /files/{id}: некорректный uuid -> 400")
    void downloadFileInvalidUuid() throws Exception {
        mockMvc.perform(get("/files/{id}", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("GET /files/{id}: ошибка чтения файла (IOException) -> исключение пробрасывается")
    void downloadFileIoError() throws Exception {
        when(fileService.getFileAsResource(ID)).thenThrow(new IOException("disk error"));

        ServletException ex = assertThrows(ServletException.class,
                () -> mockMvc.perform(get("/files/{id}", ID)));

        assertInstanceOf(RuntimeException.class, ex.getCause());
    }

    @Test
    @DisplayName("POST /files/upload: два файла передаются в сервис вместе с uuid")
    void uploadFiles() throws Exception {
        MockMultipartFile first = new MockMultipartFile("files", "a.png", "image/png", "AAA".getBytes());
        MockMultipartFile second = new MockMultipartFile("files", "b.png", "image/png", "BBB".getBytes());

        mockMvc.perform(multipart("/files/upload").file(first).file(second).param("uuid", ID.toString()))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MultipartFile>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileService).saveFiles(captor.capture(), eq(ID));
        assertEquals(2, captor.getValue().size());
        assertEquals("a.png", captor.getValue().get(0).getOriginalFilename());
        assertEquals("b.png", captor.getValue().get(1).getOriginalFilename());
    }

    @Test
    @DisplayName("POST /files/upload: некорректный uuid -> 400")
    void uploadFilesInvalidUuid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a.png", "image/png", "AAA".getBytes());

        mockMvc.perform(multipart("/files/upload").file(file).param("uuid", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("POST /files/upload: ошибка записи (IOException) -> исключение пробрасывается")
    void uploadFilesIoError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a.png", "image/png", "AAA".getBytes());
        when(fileService.saveFiles(any(), eq(ID))).thenThrow(new IOException("disk full"));

        assertThrows(ServletException.class,
                () -> mockMvc.perform(multipart("/files/upload").file(file).param("uuid", ID.toString())));
    }

    @Test
    @Disabled("BUG-8: у FilesController нет @RestController, поэтому Spring не регистрирует /files/** в реальном приложении")
    @DisplayName("FilesController зарегистрирован как Spring-контроллер")
    void controllerShouldBeAnnotated() {
        assertTrue(FilesController.class.isAnnotationPresent(RestController.class)
                || FilesController.class.isAnnotationPresent(org.springframework.stereotype.Controller.class));
    }
}
