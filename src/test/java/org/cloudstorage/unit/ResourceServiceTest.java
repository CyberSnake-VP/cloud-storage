package org.cloudstorage.unit;


import org.cloudstorage.dto.ResourceResponse;
import org.cloudstorage.exception.NotFoundException;
import org.cloudstorage.exception.ResourceAlreadyExists;
import org.cloudstorage.service.ResourceServiceImpl;
import org.cloudstorage.service.minio.StorageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceServiceTest {

    @Mock
    private StorageServiceImpl storageService;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private static final Long USER_ID = 1L;

    // GET_RESOURCE
    @Test
    @DisplayName("getResource - файл существует, возвращает информацию с размером")
    void getResource_FileExists_ShouldReturnWithSize() {
        when(storageService.exists(USER_ID, "docs/test.txt"))
                .thenReturn(true);
        when(storageService.getFileSize(USER_ID, "docs/test.txt"))
                .thenReturn(100L);

        ResourceResponse response = resourceService.getResource(USER_ID, "docs/test.txt");

        assertThat(response.name()).isEqualTo("test.txt");
        assertThat(response.path()).isEqualTo("docs/");
        assertThat(response.size()).isEqualTo(100L);
        assertThat(response.type()).isEqualTo("FILE");
    }

    @Test
    @DisplayName("getResource - папка существует, возвращает информацию без размера")
    void getResource_FolderExists_ShouldReturnWithoutSize() {
        when(storageService.exists(USER_ID, "parent/docs/"))
                .thenReturn(true);

        ResourceResponse response = resourceService.getResource(USER_ID, "parent/docs/");

        assertThat(response.path()).isEqualTo("parent/");
        assertThat(response.name()).isEqualTo("docs");
        assertThat(response.type()).isEqualTo("DIRECTORY");
    }

    @Test
    @DisplayName("getResource - пустой путь")
    void getResource_EmptyPath_ShouldThrowException() {
        assertThatThrownBy(() -> resourceService.getResource(USER_ID, ""))
                .isInstanceOf(IllegalArgumentException.class);

        // проверяем что в сервис не заходим, сразу выбрасываем исключение
        verifyNoInteractions(storageService);
    }

    // DELETE_RESOURCE
    @Test
    @DisplayName("deleteResource - успешное удаление")
    void deleteResource_Success() {
        when(storageService.exists(USER_ID, "test.txt"))
                .thenReturn(true);

        resourceService.deleteResource(USER_ID, "test.txt");

        verify(storageService, times(1))
                .deleteObject(USER_ID, "test.txt");
    }

    @Test
    @DisplayName("deleteResource - не найден")
    void deleteResource_NotFound_ShouldThrowException() {
        when(storageService.exists(USER_ID, "test.txt"))
                .thenReturn(false);

        assertThatThrownBy(() -> resourceService.deleteResource(USER_ID, "test.txt"))
                .isInstanceOf(NotFoundException.class);

        // не лезем в storageService, выходим из метода
        verify(storageService, never()).deleteObject(anyLong(), anyString());
    }

    // SEARCH_RESOURCE
    @Test
    @DisplayName("searchResource - находит файлы по имени")
    void searchResources_ShouldFindFileByName() {
        // получаем все файлы в хранилище для пользователя
        when(storageService.listAllFiles(USER_ID)).thenReturn(
                List.of(
                        "docs/",
                        "docs/report.txt",
                        "photos/",
                        "photos/image.png",
                        "readmy.md"
                )
        );

        // ищем "report"
        List<ResourceResponse> results =
                resourceService.searchResources(USER_ID, "report");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("report.txt");
        assertThat(results.get(0).path()).isEqualTo("docs/");
    }

    @Test
    @DisplayName("searchResource - находит папки по имени")
    void searchResources_ShouldFindDirectoryByName() {
        when(storageService.listAllFiles(USER_ID)).thenReturn(
                List.of(
                        "photos/",
                        "docs/report.txt",
                        "file.txt"
                )
        );

        List<ResourceResponse> results =
                resourceService.searchResources(USER_ID, "photo");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("photos");
        assertThat(results.get(0).type()).isEqualTo("DIRECTORY");
    }

    @Test
    @DisplayName("searchResource - ничего не найдено")
    void searchResources_NothingFound_ShouldReturnEmptyList() {
        when(storageService.listAllFiles(USER_ID)).thenReturn(
                List.of(
                        "docs/",
                        "photos/"
                )
        );

        List<ResourceResponse> results =
                resourceService.searchResources(USER_ID, "nothing");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("searchResource - пустой запрос")
    void searchResources_EmptyQuery_ShouldThrowException() {
        assertThatThrownBy(() -> resourceService.searchResources(USER_ID, ""))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(storageService);
    }

    // CREATE_DIRECTORY
    @Test
    @DisplayName("createDirectory - успешное создание")
    void createDirectory_ShouldReturnWithoutSize() {

        // Родительская папка существует
        when(storageService.exists(USER_ID, "docs/"))
                .thenReturn(true);

        // Новая папка отсутствует, можно создать
        when(storageService.exists(USER_ID, "docs/new/"))
                .thenReturn(false);

        ResourceResponse response = resourceService.createDirectory(USER_ID, "docs/new/");

        assertThat(response.name()).isEqualTo("new");
        assertThat(response.path()).isEqualTo("docs/");
        assertThat(response.type()).isEqualTo("DIRECTORY");

        verify(storageService, times(1)).createFolder(USER_ID, "docs/new/");
    }

    @Test
    @DisplayName("createDirectory - родительская папка не существует")
    void createDirectory_ParentNotFound_ShouldThrowException() {
        when(storageService.exists(USER_ID, "notFound/"))
                .thenReturn(false);

        assertThatThrownBy(() -> resourceService.createDirectory(USER_ID, "notFound/new/"))
                .isInstanceOf(NotFoundException.class);

        verify(storageService, never()).createFolder(anyLong(), anyString());
    }

    @Test
    @DisplayName("createDirectory - папка уже существует")
    void createDirectory_AlreadyExists_ShouldThrowException() {
        when(storageService.exists(USER_ID, "docs/"))
                .thenReturn(true);
        when(storageService.exists(USER_ID, "docs/exists/"))
                .thenReturn(true);

        assertThatThrownBy(() -> resourceService.createDirectory(USER_ID, "docs/exists/"))
                .isInstanceOf(ResourceAlreadyExists.class);
    }
}
