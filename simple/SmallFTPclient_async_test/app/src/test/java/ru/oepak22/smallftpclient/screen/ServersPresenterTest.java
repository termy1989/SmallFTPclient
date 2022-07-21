package ru.oepak22.smallftpclient.screen;

import static org.junit.Assert.assertNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.data.FtpProvider;
import ru.oepak22.smallftpclient.data.RealmProvider;
import ru.oepak22.smallftpclient.screen.servers.ServersPresenter;
import ru.oepak22.smallftpclient.screen.servers.ServersView;
import ru.oepak22.smallftpclient.test.TestFtpService;
import ru.oepak22.smallftpclient.test.TestRealmService;
import rx.Observable;
import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;
import rx.schedulers.Schedulers;

// тестирование презентера работы с серверами
@RunWith(JUnit4.class)
public class ServersPresenterTest {

    private ServersPresenter mPresenter;
    private ServersView mServersView;

    // инициализация объектов
    @Before
    public void setUp() throws Exception {

        RxAndroidPlugins.getInstance().registerSchedulersHook(new RxAndroidSchedulersHook() {
            @Override
            public Scheduler getMainThreadScheduler() {
                return Schedulers.immediate();
            }
        });

        mServersView = Mockito.mock(ServersView.class);
        mPresenter = new ServersPresenter(mServersView);
    }

    // проверка на то, что презентер корректно создается и инициализируется
    @Test
    public void testCreated() throws Exception {
        assertNotNull(mPresenter);
    }

    // проверка на отсутствие каких-либо действий
    @Test
    public void testNoActionsWithView() throws Exception {
        Mockito.verifyNoMoreInteractions(mServersView);
    }

    // список серверов не пуст
    @Test
    public void testRefresh() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        mPresenter.refresh();

        Mockito.verify(mServersView).showServers(servers);
    }

    // список серверов пуст
    @Test
    public void testRefreshNull() throws Exception {

        TestRealmProvider realmTest = new TestRealmProvider(null);
        RealmProvider.setServiceRealm(realmTest);

        mPresenter.refresh();

        Mockito.verify(mServersView).showEmpty();
    }

    // успешное удаление сервера
    @Test
    public void testRemoveSuccess() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        List<FtpServer> removeList = new ArrayList<>();
        removeList.add(server1);

        mPresenter.remove(removeList);

        Mockito.verify(mServersView).taskComplete("Successfully deleted");

        mPresenter.refresh();
    }

    // удаление сервера с ошибкой
    @Test
    public void testRemoveError() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        FtpServer server3 = new FtpServer();
        server3.setName("name3");
        server3.setAddress("127.0.0.1");
        server3.setPort(21);
        server3.setPath("/");
        server3.setUsername("username");
        server3.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        List<FtpServer> removeList = new ArrayList<>();
        removeList.add(server3);

        mPresenter.remove(removeList);

        Mockito.verify(mServersView).showMessage("Database error!", true);

        mPresenter.refresh();
    }

    // успешный поиск сервера
    @Test
    public void testFindSuccess() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        mPresenter.find("name1");

        Mockito.verify(mServersView).showFoundedServer(realmTest.findFtpServer("name1"));
    }

    // поиск сервера с ошибкой
    @Test
    public void testFindError() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        mPresenter.find("name3");

        Mockito.verify(mServersView)
                .showMessage("Item is not found. Database is corrupted!", true);
    }

    // успешное добавление сервера
    @Test
    public void testAddSuccess() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        FtpServer server3 = new FtpServer();
        server3.setName("name3");
        server3.setAddress("127.0.0.1");
        server3.setPort(21);
        server3.setPath("/");
        server3.setUsername("username");
        server3.setPassword("passwd");

        mPresenter.add(server3);

        mPresenter.connect(server3);
    }

    // добавление сервера с ошибкой
    @Test
    public void testAddError() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        FtpServer server3 = new FtpServer();
        server3.setName("name1");
        server3.setAddress("127.0.0.1");
        server3.setPort(21);
        server3.setPath("/");
        server3.setUsername("username");
        server3.setPassword("passwd");

        mPresenter.add(server3);

        Mockito.verify(mServersView)
                .showMessage("Server with specified name is already exists!", true);
    }

    // редактирование сервера, вариант 1
    @Test
    public void testEdit1() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        FtpServer server3 = new FtpServer();
        server3.setName("name3");
        server3.setAddress("127.0.0.1");
        server3.setPort(21);
        server3.setPath("/");
        server3.setUsername("username");
        server3.setPassword("passwd");

        mPresenter.edit(server3, "name4");
        mPresenter.connect(server3);
    }

    // редактирование сервера, вариант 2
    @Test
    public void testEdit2() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        mPresenter.edit(server2, "name2");
        mPresenter.connect(server2);
    }

    // редактирование сервера, вариант 3
    @Test
    public void testEdit3() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        mPresenter.edit(server2, "name3");
        Mockito.verify(mServersView)
                .showMessage("Server with specified name is already exists!", true);
    }

    // успешное соединение с сервером
    @Test
    public void testConnectSuccess() throws Exception {

        IOException ex = new IOException();
        FtpServer server = new FtpServer();
        server.setName("name");
        server.setAddress("127.0.0.1");
        server.setPort(21);
        server.setPath("/");
        server.setUsername("username");
        server.setPassword("passwd");

        TestFtpProvider ftpConnectTest = new TestFtpProvider(ex, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.connect(server);
    }

    // ошибка соединения с сервером
    @Test
    public void testConnectError() throws Exception {

        IOException ex = new IOException();
        FtpServer server = new FtpServer();
        server.setName("name");
        server.setAddress("0.0.0.0");
        server.setPort(21);
        server.setPath("/");
        server.setUsername("username");
        server.setPassword("passwd");

        TestFtpProvider ftpConnectTest = new TestFtpProvider(ex, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.connect(server);
    }

    // ошибка с установкой рабочего каталога при соединении с сервером
    @Test
    public void testPathError() throws Exception {

        IOException ex = new IOException();
        FtpServer server = new FtpServer();
        server.setName("name");
        server.setAddress("0.0.0.0");
        server.setPort(21);
        server.setPath("/asd");
        server.setUsername("username");
        server.setPassword("passwd");

        TestFtpProvider ftpConnectTest = new TestFtpProvider(ex, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.connect(server);
    }

    // успешное отключение от сервера
    @Test
    public void testDisconnectTrue() throws Exception {

        TestFtpProvider ftpConnectTest = new TestFtpProvider(null, true);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.disconnect();
    }

    // ошибка соединения с сервером
    @Test
    public void testDisconnectFalse() throws Exception {

        TestFtpProvider ftpConnectTest = new TestFtpProvider(null, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.disconnect();
    }

    // ошибка с установкой рабочего каталога при соединении с сервером
    @Test
    public void testDisconnectError() throws Exception {

        IOException ex = new IOException();

        TestFtpProvider ftpConnectTest = new TestFtpProvider(ex, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.disconnect();
    }

    //
    @Test
    public void testSaveAdd() throws Exception {

        List<FtpServer> servers = new ArrayList<>();

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        FtpServer server = new FtpServer();
        server.setName("name");
        server.setAddress("127.0.0.1");
        server.setPort(21);
        server.setPath("/");
        server.setUsername("username");
        server.setPassword("passwd");

        mPresenter.save(server, "");

        Mockito.verify(mServersView).taskComplete("Successfully added");
    }

    //
    @Test
    public void testSaveEdit() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        mPresenter.save(server1, "new_name");

        Mockito.verify(mServersView).taskComplete("Successfully edited");
    }

    // ошибка сохранения
    @Test
    public void testSaveError() throws Exception {

        FtpServer server1 = new FtpServer();
        server1.setName("name1");
        server1.setAddress("127.0.0.1");
        server1.setPort(21);
        server1.setPath("/");
        server1.setUsername("username");
        server1.setPassword("passwd");

        FtpServer server2 = new FtpServer();
        server2.setName("name2");
        server2.setAddress("127.0.0.1");
        server2.setPort(21);
        server2.setPath("/");
        server2.setUsername("username");
        server2.setPassword("passwd");

        List<FtpServer> servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);

        TestRealmProvider realmTest = new TestRealmProvider(servers);
        RealmProvider.setServiceRealm(realmTest);

        FtpServer server = new FtpServer();
        server.setName("name");
        server.setAddress("127.0.0.1");
        server.setPort(21);
        server.setPath("/");
        server.setUsername("username");
        server.setPassword("passwd");

        mPresenter.save(server, "new_name");

        Mockito.verify(mServersView).showMessage("Item is not found. Database is corrupted!", true);
    }

    // тестирование постановки презентера на паузу
    @Test
    public void pause() throws Exception {

        IOException ex = new IOException();

        FtpServer server = new FtpServer();
        server.setName("name");
        server.setAddress("127.0.0.1");
        server.setPort(21);
        server.setPath("/");
        server.setUsername("username");
        server.setPassword("passwd");

        TestFtpProvider ftpConnectTest = new TestFtpProvider(ex, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.connect(server);
        mPresenter.pause();
    }

    // тестирование закрытия презентера
    @Test
    public void close() throws Exception {
        mPresenter.close();
    }

    // завершение
    @After
    public void tearDown() throws Exception {
        RealmProvider.setServiceRealm(null);
        RxAndroidPlugins.getInstance().reset();
    }

    // переопределенный класс для тестирования FtpProvider
    private static class TestFtpProvider extends TestFtpService {

        private final IOException ex;
        private final boolean disconnected;

        // конструктор
        public TestFtpProvider(IOException ex, boolean disconnected) {
            this.disconnected = disconnected;
            this.ex = ex;
        }

        // соединение с сервером
        @NonNull
        @Override
        public Observable<String> connect(@NonNull FTPClient client, @NonNull FtpServer server,
                                          String reconnect_path) {
            if (!server.getAddress().equals("127.0.0.1"))
                return Observable.error(this.ex);
            else if (server.getPort() != 21)
                return Observable.just("Connection failed!");
            else if (reconnect_path != null || !server.getPath().equals("/"))
                return Observable.just("Specified working directory failed!");
            else if (!server.getUsername().equals("username") || !server.getPassword().equals("passwd"))
                return Observable.just("Login failed!");
            else return Observable.just("Success");
        }

        // отключение от сервера
        @Override
        public Observable<Boolean> disconnect(FTPClient client) {
            if (this.ex != null) return Observable.error(this.ex);
            else return Observable.just(this.disconnected);
        }
    }

    // переопределенный класс для тестирования RealmProvider
    private static class TestRealmProvider extends TestRealmService {

        private List<FtpServer> servers;

        // конструктор
        public TestRealmProvider(List<FtpServer> servers) { this.servers = servers; }

        // получение списка серверов
        @Override
        public List<FtpServer> getFtpServers() {
            return this.servers;
        }

        // удаление серверов по указанному списку
        @Override
        public boolean removeFtpServers(@NonNull List<FtpServer> list) {

            for (FtpServer server : list) {
                if (this.servers.contains(server))
                    return true;
            }
            return false;
        }

        // поиск сервера по указанному имени
        @Nullable
        @Override
        public FtpServer findFtpServer(String name) {

            for (FtpServer server : this.servers) {
                if (server.getName().equals(name))
                    return server;
            }
            return null;
        }

        // редактирование сервера
        @Override
        public boolean editFtpServer(FtpServer server, String name) {

            for (FtpServer item : this.servers) {
                if (item.getName().equals(server.getName()))
                    return true;
            }
            return false;
        }
    }
}
