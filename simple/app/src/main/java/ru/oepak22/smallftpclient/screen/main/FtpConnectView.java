package ru.oepak22.smallftpclient.screen.main;

import org.apache.commons.net.ftp.FTPClient;

import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.general.LoadingView;

// интерфейс презентера установки соединения
public interface FtpConnectView extends LoadingView {
    void connectSuccess(FTPClient client, FtpServer server, boolean isDialog);          // открытие FTP фрагмента, соединение установлено
    void showMessage(String msg, boolean isError);                                      // сообщение (ошибка или предупреждение)
}
