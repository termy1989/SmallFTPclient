package ru.oepak22.smallftpclient.tasks.management.move;

import android.os.AsyncTask;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.List;

import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.OperationFile;
import ru.oepak22.smallftpclient.tasks.management.CopyMoveDeleteView;

// класс презентера для перемещения файлов FTP
public class MoveFtpPresenter {

    private CopyMoveDeleteView mView;                                                                   // интерфейс перемещения
    private final FtpServer mServer;                                                                    // параметры сервера
    private final List<OperationFile> mStartCopyList;                                                   // начальный список удаляемых файлов

    // конструктор
    public MoveFtpPresenter(CopyMoveDeleteView view, FtpServer server, List<OperationFile> list) {
        mView = view;
        mServer = server;
        mStartCopyList = list;
    }

    // завершение работы презентера
    public void close() {
        mView = null;
        mStartCopyList.clear();
    }

    // перемещение файлов
    public void move() {

        // счетчик перемещенных файлов
        int counter = 0;

        // инициализация клиента FTP
        FTPClient client = new FTPClient();

        try {

            // соединение с сервером
            client.connect(mServer.getAddress(), mServer.getPort());
            if (FTPReply.isPositiveCompletion(client.getReplyCode())) {

                // авторизация на сервере
                if (client.login(mServer.getUsername(), mServer.getPassword())) {

                    // установка пассивного режима
                    client.enterLocalPassiveMode();

                    // перемещение элементов по списку
                    for (OperationFile item : mStartCopyList) {

                        if (!((AsyncTask) mView).isCancelled()) {

                            // перемещение путем переименования
                            boolean rename = client.rename(item.getSourcePath(),
                                                            item.getDestinationPath());
                            if (rename) counter++;

                            // уведомление о прогрессе
                            int progress = (int) ((double) (counter * 100)
                                    / (double) mStartCopyList.size());
                            mView.showDeleteMoveProgress(counter, progress, item.getName(),
                                                                        mStartCopyList.size());
                        }
                    }

                    // отключение от сервера
                    client.disconnect();

                    // завершающее уведомление
                    mView.showCompleted(counter, mStartCopyList.size());
                }

                // ошибка авторизации
                else {
                    if (client.isConnected())
                        client.disconnect();
                    mView.showError("Login failed!");
                }
            }

            // ошибка соединения
            else mView.showError("Connection failed!");
        }
        catch (IOException e) {
            mView.showError(e.getMessage());
        }
    }
}
