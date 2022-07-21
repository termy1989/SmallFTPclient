package ru.oepak22.smallftpclient.data;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

import io.realm.Realm;
import io.realm.RealmResults;
import ru.oepak22.smallftpclient.content.FtpServer;

// класс операций с базой данных
public class RealmOperations implements RealmService {

    // получение настроек всех сохраненных серверов FTP
    @Override
    public List<FtpServer> getFtpServers() {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        RealmResults<FtpServer> results = realm.where(FtpServer.class).findAll();
        List<FtpServer> servers = realm.copyFromRealm(results);
        realm.commitTransaction();
        realm.close();
        if (servers.isEmpty()) return null;
        else return servers;
    }

    // удаление списка серверов FTP из базы данных
    @Override
    public boolean removeFtpServers(@NonNull List<FtpServer> list) {

        Realm realm = Realm.getDefaultInstance();
        boolean status = false;

        for (FtpServer server : list) {

            // поиск сервера в базе
            realm.beginTransaction();
            RealmResults<FtpServer> results = realm.where(FtpServer.class)
                    .equalTo("mName", server.getName())
                    .findAll();

            // удаление сервера из базы
            if (results.size() == 1) {
                Objects.requireNonNull(results.get(0)).deleteFromRealm();
                status = true;
            }
            else status = false;
            realm.commitTransaction();
        }

        realm.close();
        return status;
    }

    // поиск сервера FTP в базе по заданному имени
    @Override
    public FtpServer findFtpServer(String name) {

        FtpServer result = null;
        Realm realm = Realm.getDefaultInstance();

        // поиск редактируемого элемента в базе данных
        RealmResults<FtpServer> servers = realm.where(FtpServer.class)
                .equalTo("mName", name)
                .findAll();

        // элемент найден - заполнение полей значениями элемента
        List<FtpServer> list = realm.copyFromRealm(servers);
        if (list.size() == 1) result = list.get(0);

        realm.close();
        return result;
    }

    // добавление сервера FTP в базу данных
    @Override
    public void addFtpServer(FtpServer server) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.insert(server);
        realm.commitTransaction();
        realm.close();
    }

    // редактирование сервера FTP, указанного по имени
    @Override
    public boolean editFtpServer(FtpServer server, String name) {

        Realm realm = Realm.getDefaultInstance();

        // поиск редактируемого элемента в базе данных
        realm.beginTransaction();
        RealmResults<FtpServer> servers = realm.where(FtpServer.class)
                .equalTo("mName", name)
                .findAll();

        // элемент найден - замена на отредактированный
        if (servers.size() == 1) {
            Objects.requireNonNull(servers.get(0)).deleteFromRealm();
            realm.insert(server);
            realm.commitTransaction();
            realm.close();
            return true;
        }

        // элемент не найден - ошибка в базе данных
        else {
            realm.close();
            return false;
        }
    }
}
