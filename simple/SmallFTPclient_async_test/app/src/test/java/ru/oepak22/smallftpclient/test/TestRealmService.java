package ru.oepak22.smallftpclient.test;

import java.util.List;

import ru.oepak22.smallftpclient.data.RealmService;
import ru.oepak22.smallftpclient.content.FtpServer;

public class TestRealmService implements RealmService {

    @Override
    public List<FtpServer> getFtpServers() {
        return null;
    }

    @Override
    public boolean removeFtpServers(List<FtpServer> list) {
        return false;
    }

    @Override
    public FtpServer findFtpServer(String name) {
        return null;
    }

    @Override
    public void addFtpServer(FtpServer server) {}

    @Override
    public boolean editFtpServer(FtpServer server, String name) {
        return false;
    }
}
