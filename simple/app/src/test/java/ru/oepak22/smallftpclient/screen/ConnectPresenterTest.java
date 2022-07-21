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

import ru.oepak22.smallftpclient.data.FtpProvider;
import ru.oepak22.smallftpclient.data.RealmProvider;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.screen.main.FtpConnectPresenter;
import ru.oepak22.smallftpclient.screen.main.FtpConnectView;
import ru.oepak22.smallftpclient.test.TestFtpService;
import ru.oepak22.smallftpclient.test.TestRealmService;
import rx.Observable;
import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;
import rx.schedulers.Schedulers;

// тестирование презентера установки соединения с сервером
@RunWith(JUnit4.class)
public class ConnectPresenterTest {

    private FtpConnectPresenter mPresenter;
    private FtpConnectView mFtpConnectView;
    private FtpServer mServer;
    private FTPClient mClient;

    // инициализация объектов
    @Before
    public void setUp() throws Exception {

        RxAndroidPlugins.getInstance().registerSchedulersHook(new RxAndroidSchedulersHook() {
            @Override
            public Scheduler getMainThreadScheduler() {
                return Schedulers.immediate();
            }
        });

        mFtpConnectView = Mockito.mock(FtpConnectView.class);
        mPresenter = new FtpConnectPresenter(mFtpConnectView);
    }

    // проверка на то, что презентер корректно создается и инициализируется
    @Test
    public void testCreated() throws Exception {
        assertNotNull(mPresenter);
    }

    // проверка на отсутствие каких-либо действий
    @Test
    public void testNoActionsWithView() throws Exception {
        Mockito.verifyNoMoreInteractions(mFtpConnectView);
    }

    // ошибка базы данных, элемент не найден
    @Test
    public void testDatabaseError() throws Exception {

        TestRealmProvider realmTest = new TestRealmProvider(null);
        RealmProvider.setServiceRealm(realmTest);

        mPresenter.connect("name", true);

        Mockito.verify(mFtpConnectView)
                .showMessage("Item is not found. Database is corrupted!", true);
    }

    // успешное соединение с сервером
    @Test
    public void testConnectSuccess() throws Exception {

        IOException ex = new IOException();
        mClient = new FTPClient();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.1");
        mServer.setPort(21);
        mServer.setPath("/");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        TestRealmProvider realmTest = new TestRealmProvider(mServer);
        TestFtpProvider ftpConnectTest = new TestFtpProvider(/*mServer, mClient,*/ ex);

        RealmProvider.setServiceRealm(realmTest);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.connect("name", true);
    }

    // ошибка соединения с сервером
    @Test
    public void testConnectError() throws Exception {

        IOException ex = new IOException();
        mClient = new FTPClient();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("0.0.0.0");
        mServer.setPort(21);
        mServer.setPath("/");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        TestRealmProvider realmTest = new TestRealmProvider(mServer);
        TestFtpProvider ftpConnectTest = new TestFtpProvider(/*mServer, mClient,*/ ex);

        RealmProvider.setServiceRealm(realmTest);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.connect("name", true);
    }

    // ошибка с установкой рабочего каталога при соединении с сервером
    @Test
    public void testPathError() throws Exception {

        IOException ex = new IOException();
        mClient = new FTPClient();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.1");
        mServer.setPort(21);
        mServer.setPath("/asd");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        TestRealmProvider realmTest = new TestRealmProvider(mServer);
        TestFtpProvider ftpConnectTest = new TestFtpProvider(/*mServer, mClient,*/ ex);

        RealmProvider.setServiceRealm(realmTest);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.connect("name", true);
    }

    // тестирование постановки презентера на паузу
    @Test
    public void pause() throws Exception {

        IOException ex = new IOException();
        mClient = new FTPClient();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.1");
        mServer.setPort(21);
        mServer.setPath("/");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        TestRealmProvider realmTest = new TestRealmProvider(mServer);
        TestFtpProvider ftpConnectTest = new TestFtpProvider(ex);

        RealmProvider.setServiceRealm(realmTest);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.connect("name", true);
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
        FtpProvider.setServiceFTP(null);
        RealmProvider.setServiceRealm(null);
        RxAndroidPlugins.getInstance().reset();
    }

    // переопределенный класс для тестирования FtpProvider
    private static class TestFtpProvider extends TestFtpService {

        private final IOException ex;

        // конструктор
        public TestFtpProvider(IOException ex) {
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
    }

    // переопределенный класс для тестирования RealmProvider
    private class TestRealmProvider extends TestRealmService {

        private FtpServer server;

        // конструктор
        public TestRealmProvider(FtpServer server) { this.server = server; }

        // поиск сервера в базе по имени
        @Nullable
        @Override
        public FtpServer findFtpServer(String name) {
            if (this.server == null || !server.getName().equals(name))
                return null;
            else return this.server;
        }
    }
}
