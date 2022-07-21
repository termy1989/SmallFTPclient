package ru.oepak22.smallftpclient.data;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

// провайдер локального хранилища
public final class FilesProvider {

    // экземпляр интерфейса
    private static FilesService sFilesService;

    // конструктор
    private FilesProvider() {}

    // инициализатор
    @MainThread
    public static void init() { sFilesService = new FilesOperations(); }

    // проверка на то, что операционный класс проинициализирован
    @NonNull
    public static FilesService provideFilesService() {
        if (sFilesService == null) {
            sFilesService = new FilesOperations();
        }
        return sFilesService;
    }

    // установка переопределенного операционного класса (для тестирования)
    public static void setFilesService(FilesService filesService) {
        sFilesService = filesService;
    }
}
