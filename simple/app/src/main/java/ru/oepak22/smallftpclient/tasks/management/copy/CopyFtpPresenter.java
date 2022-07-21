package ru.oepak22.smallftpclient.tasks.management.copy;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.OperationFile;
import ru.oepak22.smallftpclient.tasks.management.CopyMoveDeleteView;

// класс презентера для копирования файлов FTP
public class CopyFtpPresenter {

    private CopyMoveDeleteView mView;                                                                   // интерфейс копирования
    private final FtpServer mServer;                                                                    // параметры сервера
    private final List<OperationFile> mStartCopyList;                                                   // начальный список копируемых файлов
    private List<OperationFile> mResultCopyList;                                                        // результирующий список копируемых файлов
    private List<String> mCreateFoldersList;                                                            // список папок для создания
    private long mTotalSize;                                                                            // суммарный объем копируемой информации
    private long mCopySize;                                                                             // суммарный объем уже скопированной информации
    private int mFileCounter;                                                                           // счетчик скопированных файлов

    // конструктор
    public CopyFtpPresenter(CopyMoveDeleteView view, FtpServer server, List<OperationFile> list) {
        mView = view;
        mServer = server;
        mStartCopyList = list;
    }

    // завершение работы презентера
    public void close() {
        mView = null;
        mStartCopyList.clear();
        mResultCopyList.clear();
        mCreateFoldersList.clear();
    }

    // копирование файлов - построение структуры
    public void copy() {

        mFileCounter = 0;
        mCopySize = 0;
        mTotalSize = 0;
        mResultCopyList = new ArrayList<>();
        mCreateFoldersList = new ArrayList<>();

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

                    // формирование нового списка элементов для копирования
                    for (OperationFile item : mStartCopyList) {

                        // список каталогов для создания
                        if (item.isDirectory() && !((AsyncTask) mView).isCancelled()) {

                            mCreateFoldersList.add(item.getDestinationPath());
                            createDirectoryTree(client, item.getDestinationPath(),
                                                                item.getSourcePath());
                        }

                        // список файлов для копирования
                        else if (!((AsyncTask) mView).isCancelled()) {
                            mTotalSize += item.getSize();
                            mResultCopyList.add(item);
                        }
                    }

                    // отключение от сервера
                    client.disconnect();

                    // переход к созданию папок для копирования
                    createCopyFolders();
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

    // формирование списка файлов и создание директорий
    private void createDirectoryTree(@NonNull FTPClient ftpClient,
                                     String destinationPath, String sourcePath) throws IOException {

        // получение содержимого очередной директории
        FTPFile[] remoteFiles = ftpClient.listFiles(sourcePath);

        // обработка списка файлов и каталогов очередной директории
        if (remoteFiles != null && remoteFiles.length > 0 && !((AsyncTask) mView).isCancelled()) {
            for (FTPFile remoteFile : remoteFiles) {
                if (!remoteFile.getName().equals(".")
                        && !remoteFile.getName().equals("..")
                        && !((AsyncTask) mView).isCancelled()) {

                    // формирование новых путей
                    String destinationFilePath = destinationPath + "/" + remoteFile.getName();
                    String sourceFilePath = sourcePath + "/" + remoteFile.getName();

                    // добавление каталога в список на создание, переход в следующий каталог
                    if (remoteFile.isDirectory()) {
                        mCreateFoldersList.add(destinationFilePath);
                        mView.showMessage("prepare (" + mCreateFoldersList.size()
                                                        + " folders, " + mResultCopyList.size()
                                                                                    + " files)");
                        createDirectoryTree(ftpClient, destinationFilePath, sourceFilePath);
                    }

                    // добавление файла в список на копирование
                    else {
                        OperationFile copyFile = new OperationFile();
                        copyFile.setName(remoteFile.getName());
                        copyFile.setDestinationPath(destinationFilePath);
                        copyFile.setSourcePath(sourceFilePath);
                        copyFile.setSize(remoteFile.getSize());
                        mResultCopyList.add(copyFile);
                        mView.showMessage("prepare (" + mCreateFoldersList.size()
                                                        + " folders, " + mResultCopyList.size()
                                                                                    + " files)");
                        mTotalSize += copyFile.getSize();
                    }
                }
            }
        }
    }

    // создание папок для копирования, в соответствии с инфраструктурой
    private void createCopyFolders() {

        boolean status = true;

        // создание папок по списку (если нужно создать)
        if (mCreateFoldersList != null && !mCreateFoldersList.isEmpty()) {

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

                        // обработка списка каталогов, которые необходимо создать
                        for (String path : mCreateFoldersList) {

                            client.changeWorkingDirectory(path);
                            int returnCode = client.getReplyCode();
                            if (returnCode == 550 && !((AsyncTask) mView).isCancelled()) {
                                boolean created = client.makeDirectory(path);
                                if (created) {
                                    Log.d("CopyTask", "Create folder: " + path);
                                    mView.showMessage("Create folder: " + path);
                                }
                                else {
                                    Log.d("CopyTask", "Can't create folder: " + path);
                                    mView.showMessage("Can't create folder: " + path);
                                }
                            }
                        }

                        // отключение от сервера
                        client.disconnect();
                    }

                    // ошибка авторизации
                    else {
                        if (client.isConnected()) {
                            status = false;
                            client.disconnect();
                        }
                        mView.showError("Login failed");
                    }
                }
                else {
                    status = false;
                    mView.showError("Connection error");
                }
            } catch (IOException e) {
                status = false;
                Log.v("CopyTask", "Error: " + e.getMessage());
                mView.showError(e.getMessage());
            }
        }

        // копирование всех файлов
        if (status) copyAllFiles();
    }

    // копирование файлов по списку
    private void copyAllFiles() {

        boolean status = true;

        for (OperationFile file : mResultCopyList) {

            if (!((AsyncTask) mView).isCancelled()) {
                String uploading = copySingleFile(file);
                if (!uploading.equals("Success")) {
                    mView.showError(uploading);
                    status = false;
                    break;
                }
            }
        }

        // завершающее уведомление
        if (status) mView.showCompleted(mFileCounter, mResultCopyList.size());
    }

    // копирование одного FTP-файла
    private String copySingleFile(OperationFile file) {

        // инициализация клиентов FTP (чтение и запись)
        FTPClient ftpClientInput = new FTPClient();
        FTPClient ftpClientOutput = new FTPClient();

        // установка тайм-аутов
        ftpClientInput.setDataTimeout(10000);
        ftpClientOutput.setDataTimeout(10000);
        ftpClientInput.setConnectTimeout(10000);
        ftpClientOutput.setConnectTimeout(10000);
        ftpClientInput.setDefaultTimeout(10000);
        ftpClientOutput.setDefaultTimeout(10000);

        try {

            // соединение с сервером FTP
            Log.d("CopyTask", "Connect to FTP server...");
            ftpClientInput.connect(mServer.getAddress(), mServer.getPort());
            ftpClientOutput.connect(mServer.getAddress(), mServer.getPort());

            // авторизация на сервере FTP
            Log.d("CopyTask", "Login on FTP server...");
            if (ftpClientInput.login(mServer.getUsername(), mServer.getPassword())
                    && ftpClientOutput.login(mServer.getUsername(), mServer.getPassword())) {

                // установка пассивного режима
                ftpClientInput.enterLocalPassiveMode();
                ftpClientOutput.enterLocalPassiveMode();
                Log.d("CopyTask", "Enter FTP passive mode");
                if (ftpClientInput.setFileType(FTP.BINARY_FILE_TYPE)
                        && ftpClientOutput.setFileType(FTP.BINARY_FILE_TYPE))
                    Log.d("CopyTask", "Enter FTP binary file type");

                // инициализация потока записи файла на сервер
                OutputStream outputStream = ftpClientOutput.storeFileStream(file.getDestinationPath());
                Log.d("CopyTask", "Open output stream for file - " + file.getDestinationPath());

                if (outputStream != null) {

                    // инициализация потока чтения исходного файла
                    InputStream inputStream = ftpClientInput.retrieveFileStream(file.getSourcePath());
                    Log.d("CopyTask", "Open input stream for file - " + file.getName());

                    if (inputStream != null && ftpClientInput.getReplyCode() != 550) {

                        // чтение/запись из потока в поток
                        byte[] data = new byte[8192];
                        long singleDownload = 0;
                        int count;
                        while ((count = inputStream.read(data)) != -1
                                    && !((AsyncTask) mView).isCancelled()) {

                            outputStream.write(data, 0, count);
                            singleDownload += count;
                            mCopySize += count;

                            int progressCommon = (int) ((double) (mCopySize * 100)
                                                                / (double) mTotalSize);
                            int progressSingle = (int) ((double) (singleDownload * 100)
                                                                / (double) file.getSize());

                            mView.showCopyMoveProgress(mFileCounter, mResultCopyList.size(),
                                                        progressCommon, progressSingle, mCopySize,
                                                                        mTotalSize, file.getName());
                        }

                        // закрытие потоков
                        outputStream.close();
                        Log.d("CopyTask", "Close outputStream for file - " + file.getName());
                        inputStream.close();
                        Log.d("CopyTask", "Close inputStream for file - " + file.getName());

                        // увеличение числа скопированных файлов
                        mFileCounter++;
                    }
                }
            }

            // ошибка авторизации
            else {
                Log.d("CopyTask", "Login incorrect");
                return "Login incorrect";
            }

            // отключение от сервера
            ftpClientInput.disconnect();
            ftpClientOutput.disconnect();
            Log.d("CopyTask", "Disconnect");
        }
        catch (IOException ex) {
            Log.d("CopyTask", ex.getMessage());
            return ex.getMessage();
        }

        return "Success";
    }
}
