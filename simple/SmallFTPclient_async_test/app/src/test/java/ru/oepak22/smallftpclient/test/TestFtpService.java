package ru.oepak22.smallftpclient.test;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.util.List;

import ru.oepak22.smallftpclient.data.FtpService;
import ru.oepak22.smallftpclient.content.FtpServer;
import rx.Observable;

public class TestFtpService implements FtpService {

    @Override
    public Observable<String> connect(FTPClient client, FtpServer server, String reconnect_path) {
        return Observable.empty();
    }

    @Override
    public Observable<Boolean> disconnect(FTPClient client) {
        return Observable.empty();
    }

    @Override
    public Observable<String> getWorkingDirectory(FTPClient client) {
        return Observable.empty();
    }

    @Override
    public Observable<List<FTPFile>> getFiles(FTPClient client) {
        return Observable.empty();
    }

    @Override
    public Observable<Integer> changeDirectory(FTPClient client, String dir) {
        return Observable.empty();
    }

    @Override
    public Observable<Boolean> setWorkingDirectory(FTPClient client, String dir) {
        return Observable.empty();
    }

    @Override
    public Observable<Boolean> parentDirectory(FTPClient client) {
        return Observable.empty();
    }

    @Override
    public Observable<Boolean> createDirectory(FTPClient client, String path) {
        return Observable.empty();
    }

    @Override
    public Observable<Boolean> renameFile(FTPClient client, String oldFile, String newFile) {
        return Observable.empty();
    }

    @Override
    public boolean isActive(FTPClient client) {
        return false;
    }
}
