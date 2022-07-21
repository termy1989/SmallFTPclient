package ru.oepak22.smallftpclient.screen;

import static org.junit.Assert.assertNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.RemoteFile;
import ru.oepak22.smallftpclient.data.FtpProvider;
import ru.oepak22.smallftpclient.screen.ftp.FtpStoragePresenter;
import ru.oepak22.smallftpclient.screen.ftp.FtpStorageView;
import ru.oepak22.smallftpclient.test.TestFtpService;
import rx.Observable;
import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;
import rx.schedulers.Schedulers;

// тестирование презентера работы с хранилищем FTP
@RunWith(JUnit4.class)
public class FtpPresenterTest {

    private FtpStoragePresenter mPresenter;
    private FtpStorageView mFtpView;
    private FtpServer mServer;
    private FTPClient mClient;
    private List<RemoteFile> mCachedList;

    // инициализация объектов
    @Before
    public void setUp() throws Exception {

        RxAndroidPlugins.getInstance().registerSchedulersHook(new RxAndroidSchedulersHook() {
            @Override
            public Scheduler getMainThreadScheduler() {
                return Schedulers.immediate();
            }
        });

        mClient = new FTPClient();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.1");
        mServer.setPort(21);
        mServer.setPath("/");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        mFtpView = Mockito.mock(FtpStorageView.class);
        mPresenter = new FtpStoragePresenter(mFtpView, mServer, mClient);
    }

    // проверка на то, что презентер корректно создается и инициализируется
    @Test
    public void testCreated() throws Exception {
        assertNotNull(mPresenter);
    }

    // проверка на отсутствие каких-либо действий
    @Test
    public void testNoActionsWithView() throws Exception {
        Mockito.verifyNoMoreInteractions(mFtpView);
    }

    // обновление содержимого каталога
    @Test
    public void testRefresh() throws Exception {

        IOException ex = new IOException();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.1");
        mServer.setPort(21);
        mServer.setPath("/");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        TestFtpProvider ftpConnectTest = new TestFtpProvider(mServer, ex, true, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.refresh();

        //mPresenter.refreshConnected();
    }

    // обновление содержимого каталога с переустановкой соединения
    @Test
    public void testNewRefresh() throws Exception {

        IOException ex = new IOException();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.1");
        mServer.setPort(21);
        mServer.setPath("/");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        TestFtpProvider ftpConnectTest = new TestFtpProvider(mServer, ex, false, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.refresh();
    }

    // восстановление после поворота экрана
    @Test
    public void testRotate() throws Exception {

        IOException ex = new IOException();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.1");
        mServer.setPort(21);
        mServer.setPath("/");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        mCachedList = new ArrayList<>();
        RemoteFile f1 = new RemoteFile();
        RemoteFile f2 = new RemoteFile();
        RemoteFile f3 = new RemoteFile();
        mCachedList.add(f1);
        mCachedList.add(f2);
        mCachedList.add(f3);

        TestFtpProvider ftpConnectTest = new TestFtpProvider(mServer, ex, false, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        mPresenter.saveCache(mCachedList);

        mPresenter.refresh();
    }

    // ошибка - исключение
    @Test
    public void testRefreshError1() throws Exception {

        IOException ex = new IOException();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.0");
        mServer.setPort(21);
        mServer.setPath("/");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        /*mCachedList = new ArrayList<>();
        RemoteFile f1 = new RemoteFile();
        RemoteFile f2 = new RemoteFile();
        RemoteFile f3 = new RemoteFile();
        mCachedList.add(f1);
        mCachedList.add(f2);
        mCachedList.add(f3);*/

        TestFtpProvider ftpConnectTest = new TestFtpProvider(mServer, ex, false, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        //mPresenter.saveCache(mCachedList);

        mPresenter.refresh();
    }

    // ошибка - сообщение
    @Test
    public void testRefreshError2() throws Exception {

        IOException ex = new IOException();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.1");
        mServer.setPort(21);
        mServer.setPath("/asd");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        /*mCachedList = new ArrayList<>();
        RemoteFile f1 = new RemoteFile();
        RemoteFile f2 = new RemoteFile();
        RemoteFile f3 = new RemoteFile();
        mCachedList.add(f1);
        mCachedList.add(f2);
        mCachedList.add(f3);*/

        TestFtpProvider ftpConnectTest = new TestFtpProvider(mServer, ex, false, false);
        FtpProvider.setServiceFTP(ftpConnectTest);

        //mPresenter.saveCache(mCachedList);

        mPresenter.refresh();
    }

    // нет файлов
    @Test
    public void testRefreshEmpty() throws Exception {

        IOException ex = new IOException();
        mServer = new FtpServer();
        mServer.setName("name");
        mServer.setAddress("127.0.0.1");
        mServer.setPort(21);
        mServer.setPath("/");
        mServer.setUsername("username");
        mServer.setPassword("passwd");

        /*mCachedList = new ArrayList<>();
        RemoteFile f1 = new RemoteFile();
        RemoteFile f2 = new RemoteFile();
        RemoteFile f3 = new RemoteFile();
        mCachedList.add(f1);
        mCachedList.add(f2);
        mCachedList.add(f3);*/

        TestFtpProvider ftpConnectTest = new TestFtpProvider(mServer, ex, false, true);
        FtpProvider.setServiceFTP(ftpConnectTest);

        //mPresenter.saveCache(mCachedList);

        mPresenter.refresh();

        //Mockito.verify(mFtpView).showEmpty();
    }










    // завершение
    @After
    public void tearDown() throws Exception {
        RxAndroidPlugins.getInstance().reset();
    }

    // переопределенный класс для тестирования FtpProvider
    private static class TestFtpProvider extends TestFtpService {

        private final IOException ex;
        private final boolean isAcive;
        private final boolean isEmpty;
        private final FtpServer server;

        // конструктор
        public TestFtpProvider(FtpServer server, IOException ex, boolean isAcive, boolean isEmpty) {
            this.isAcive = isAcive;
            this.ex = ex;
            this.server = server;
            this.isEmpty = isEmpty;
        }

        // соединение с сервером
        @NonNull
        @Override
        public Observable<String> connect(@NonNull FTPClient client, @NonNull FtpServer server,
                                          String reconnect_path) {
            if (!this.server.getAddress().equals("127.0.0.1"))
                return Observable.error(this.ex);
            else if (this.server.getPort() != 21)
                return Observable.just("Connection failed!");
            else if (reconnect_path != null || !this.server.getPath().equals("/"))
                return Observable.just("Specified working directory failed!");
            else if (!this.server.getUsername().equals("username")
                        || !this.server.getPassword().equals("passwd"))
                return Observable.just("Login failed!");
            else return Observable.just("Success");
        }

        // проверка активности клиента
        @Override
        public boolean isActive(FTPClient client) {
            return isAcive;
        }

        // получение списка файлов
        @Nullable
        @Override
        public Observable<List<FTPFile>> getFiles(FTPClient client) {
            if (this.isEmpty) return Observable.empty();
            else {
                List<FTPFile> list = new ArrayList<>();
                FTPFile file1 = new FTPFile();
                FTPFile file2 = new FTPFile();
                list.add(file1);
                list.add(file2);
                return Observable.just(list);
            }
        }

        // получение текущей рабочей директории
        @Override
        public Observable<String> getWorkingDirectory(FTPClient client) {
            return Observable.just("/");
        }
    }
}
