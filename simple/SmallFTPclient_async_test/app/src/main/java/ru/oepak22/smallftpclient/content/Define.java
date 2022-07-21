package ru.oepak22.smallftpclient.content;

// константы для идентификаторов
public interface Define {

    // широковещательные сообщения
    String SERVICE_DOWNLOAD = "ru.oepak22.smallftpclient.download_service";
    String SERVICE_UPLOAD = "ru.oepak22.smallftpclient.upload_service";
    String SERVICE_OTHER = "ru.oepak22.smallftpclient.other_service";

    // асинхронные операции
    int SD_ROOT = 1;
    int UPLOAD_CANCEL = 2;
    int DOWNLOAD_CANCEL = 3;
    int OTHER_CANCEL = 4;
    int DOWNLOAD_TASK = 5;
    int UPLOAD_TASK = 6;
    int COPY_TASK = 7;
    int MOVE_TASK = 8;
    int DELETE_TASK = 10;

    // синхронные операции
    int LOCAL_FILE_RENAME = 10;
    int LOCAL_DIR_CREATE = 11;
    int FTP_FILE_RENAME = 12;
    int FTP_DIR_CREATE = 13;
}
