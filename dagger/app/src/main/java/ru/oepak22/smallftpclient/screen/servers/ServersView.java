package ru.oepak22.smallftpclient.screen.servers;

import androidx.annotation.NonNull;

import java.util.List;

import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.general.LoadingView;

// интерфейс для презентера списка серверов
public interface ServersView extends LoadingView {
    void showServers(@NonNull List<FtpServer> servers);             // вывод списка серверов из базы
    void showFoundedServer(FtpServer server);                       // вывод параметров найденного в базе сервера
    void connectResult(String stat);                                // обработка результатов проверки соединения
    void disconnectResult(boolean stat);                            // обработка успешности завершения соединения
    void showEmpty();                                               // вывод пустого пространства на экран
    void showMessage(String msg, boolean isError);                  // вывод сообщения
    void taskComplete(String msg);                                  // сигнал о выполнении опреации
}
