package ru.oepak22.smallftpclient.data;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.util.List;

import ru.oepak22.smallftpclient.content.FtpServer;
import rx.Observable;

// интерфейс для FTP операций
public interface FtpService {

    Observable<String> connect(FTPClient client, FtpServer server, String reconnect_path);

    Observable<Boolean> disconnect(FTPClient client);

    Observable<String> getWorkingDirectory(FTPClient client);

    Observable<List<FTPFile>> getFiles(FTPClient client);

    Observable<Integer> changeDirectory(FTPClient client, String dir);

    Observable<Boolean> setWorkingDirectory(FTPClient client, String dir);

    Observable<Boolean> parentDirectory(FTPClient client);

    Observable<Boolean> createDirectory(FTPClient client, String path);

    Observable<Boolean> renameFile(FTPClient client, String oldFile, String newFile);

    boolean isActive(FTPClient client);
}
