package ru.oepak22.smallftpclient.data;

import java.util.List;

import ru.oepak22.smallftpclient.content.FtpServer;

// интерфейс для операции с базой данных
public interface RealmService {

   List<FtpServer> getFtpServers();

   boolean removeFtpServers(List<FtpServer> list);

   FtpServer findFtpServer(String name);

   void addFtpServer(FtpServer server);

   boolean editFtpServer(FtpServer server, String name);
}
