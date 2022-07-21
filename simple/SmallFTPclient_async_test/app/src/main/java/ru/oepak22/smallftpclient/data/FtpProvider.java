package ru.oepak22.smallftpclient.data;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

// провайдер сервера FTP
public final class FtpProvider {

    // экземпляр интерфейса
    private static FtpService sFtpService;

    // конструктор
    private FtpProvider() {}

    // инициализатор
    @MainThread
    public static void init() {
        sFtpService = new FtpOperations();
    }

    // проверка на то, что операционный класс проинициализирован
    @NonNull
    public static FtpService provideServiceFTP() {
        if (sFtpService == null) {
            sFtpService = new FtpOperations();
        }
        return sFtpService;
    }

    // установка переопределенного операционного класса (для тестирования)
    public static void setServiceFTP(FtpService ftpService) {
        sFtpService = ftpService;
    }
}
