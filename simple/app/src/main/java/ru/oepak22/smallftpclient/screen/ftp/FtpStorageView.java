package ru.oepak22.smallftpclient.screen.ftp;

import java.util.List;

import ru.oepak22.smallftpclient.content.RemoteFile;
import ru.oepak22.smallftpclient.general.LoadingView;

// интерфейс для презентера сервера FTP
public interface FtpStorageView extends LoadingView {

    void showFiles(List<RemoteFile> list);                  // вывод содержимого на экран
    void showError(String msg);                          // вывод сообщения об ошибке
    void showEmpty();                                       // вывод пустого пространства на экран
}
