package ru.oepak22.smallftpclient.data;

import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import ru.oepak22.smallftpclient.content.FtpServer;
import rx.Observable;

// класс операций, исполняемых на сервере FTP
public class FtpOperations implements FtpService {

    // соединение с сервером
    @Override
    public Observable<String> connect(@NonNull FTPClient client, FtpServer server,
                                                                    String reconnect_path) {

        client.setConnectTimeout(7000);

        return Observable.fromCallable(() -> {

            // подключение по адресу и порту
            client.connect(server.getAddress(), server.getPort());
            Log.v("FTP", "Trying to connect...");
            if (FTPReply.isPositiveCompletion(client.getReplyCode())) {

                // авторизация
                boolean status;
                Log.v("FTP", "Trying to login...");
                if (server.isAnon())
                    status = client.login("anonymous", "nobody");
                else
                    status = client.login(server.getUsername(), server.getPassword());

                if (status) {

                    // установка рабочей директории
                    String workPath;
                    if (reconnect_path != null) workPath = reconnect_path;
                    else workPath = server.getPath();

                    if (workPath.equals("/")) {

                        // установка пассивного режима
                        client.enterLocalPassiveMode();
                        return "Success";
                    }
                    else {

                        Log.v("FTP", "Trying to set root directory to "
                                                                    + workPath + "...");
                        if (client.changeWorkingDirectory(workPath)) {

                            // установка пассивного режима
                            client.enterLocalPassiveMode();
                            Log.v("FTP", "Connected successfully");
                            return "Success";
                        }
                        else {
                            if (client.isConnected()) {
                                client.logout();
                                client.disconnect();
                                Log.v("FTP", "Root directory failed. Disconnect...");
                            }
                            return "Specified working directory failed!";
                        }
                    }
                }
                else {
                    if (client.isConnected()) {
                        client.disconnect();
                        Log.v("FTP", "Login failed. Disconnect...");
                    }
                    return "Login failed!";
                }
            }
            else {
                Log.v("FTP", "Connection failed");
                return "Connection failed!";
            }
        }).onErrorResumeNext(throwable -> {
            Log.v("FTP error:", throwable.getMessage());
            return Observable.error(throwable);
        });
    }

    // отключение от сервера
    @Override
    public Observable<Boolean> disconnect(FTPClient client) {

        return Observable.fromCallable(() -> {
            if (client != null && client.isConnected()) {
                Log.v("FTP", "Trying to disconnect...");
                client.logout();
                client.disconnect();
                return true;
            }
            Log.v("FTP", "Already disconnected");
            return false;
        }).onErrorResumeNext(throwable -> {
            Log.v("FTP error:", throwable.getMessage());
            return Observable.error(throwable);
        });
    }

    // получение текущей рабочей директории
    @Override
    public Observable<String> getWorkingDirectory(FTPClient client) {

        return Observable.fromCallable(() -> {
            String dir = client.printWorkingDirectory();
            Log.v("FTP", "Working directory is " + dir);
            return dir;
        }).onErrorResumeNext(throwable -> {
            Log.v("FTP error:", throwable.getMessage());
            return Observable.error(throwable);
        });
    }

    // получение списка всех файлов текущей рабочей директории
    @Override
    public Observable<List<FTPFile>> getFiles(FTPClient client) {

        return Observable.fromCallable((Callable<List<FTPFile>>) () -> {
            Log.v("FTP", "Getting file list...");
            return new ArrayList<>(Arrays.asList(client.listFiles()));
        }).onErrorResumeNext(throwable -> {
            Log.v("FTP error:", throwable.getMessage());
            return Observable.error(throwable);
        });
    }

    // переход в указанную директорию
    @Override
    public Observable<Integer> changeDirectory(FTPClient client, String dir) {

        return Observable.fromCallable(() -> {
            int reply = client.cwd(dir);
            Log.v("FTP", "Trying to change directory to " + dir + ". Reply is " + reply);
            return reply;
        }).onErrorResumeNext(throwable -> {
            Log.v("FTP error:", throwable.getMessage());
            return Observable.error(throwable);
        });
    }

    // установка рабочей директории
    public Observable<Boolean> setWorkingDirectory(FTPClient client, String dir) {

        return Observable.fromCallable(() -> {
            boolean status = client.changeWorkingDirectory(dir);
            if (status) Log.v("FTP", "Set working directory to " + dir);
            else Log.v("FTP", "Can't set working directory to " + dir);
            return status;
        }).onErrorResumeNext(throwable -> {
            Log.v("FTP error:", throwable.getMessage());
            return Observable.error(throwable);
        });
    }

    // переход в родительскую директорию
    @Override
    public Observable<Boolean> parentDirectory(FTPClient client) {

        return Observable.fromCallable(() -> {
            boolean status = client.changeToParentDirectory();
            if (status) Log.v("FTP", "Come to parent directory");
            else Log.v("FTP", "Can't come to parent directory");
            return status;
        }).onErrorResumeNext(throwable -> {
            Log.v("FTP error:", throwable.getMessage());
            return Observable.error(throwable);
        });
    }

    // создание каталога
    @Override
    public Observable<Boolean> createDirectory(FTPClient client, String path) {

        return Observable.fromCallable(() -> {
            boolean status = client.makeDirectory(path);
            if (status) Log.v("FTP", "Create directory " + path);
            else Log.v("FTP", "Can't create directory " + path);
            return status;
        }).onErrorResumeNext(throwable -> {
            Log.v("FTP error:", throwable.getMessage());
            return Observable.error(throwable);
        });
    }

    // переименование файла
    @Override
    public Observable<Boolean> renameFile(FTPClient client, String oldFile, String newFile) {

        return Observable.fromCallable(() -> {
            boolean status = client.rename(oldFile, newFile);
            if (status) Log.v("FTP", "Rename file " + oldFile + " to " + newFile);
            else Log.v("FTP", "Can't rename file " + oldFile + " to " + newFile);
            return status;
        }).onErrorResumeNext(throwable -> {
            Log.v("FTP error:", throwable.getMessage());
            return Observable.error(throwable);
        });
    }

    // проверка активности клиента
    @Override
    public boolean isActive(FTPClient client) {
        return client != null && client.isConnected();
    }
}