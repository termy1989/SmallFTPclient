package ru.oepak22.smallftpclient.screen.servers;

import androidx.annotation.NonNull;

import org.apache.commons.net.ftp.FTPClient;

import java.util.List;

import ru.oepak22.smallftpclient.data.FtpService;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.data.RealmService;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

// класс презентера для получения списка серверов
public class ServersPresenter {

    private ServersView mView;                                                                      // интерфейс презентера
    private Subscription mSubscription;                                                             // реактивный наблюдатель
    private FTPClient mClient;                                                                      // FTP клиент для проверки соединения
    private final RealmService mRealmService;                                                       // интерфейс операций Realm
    private final FtpService mFtpService;                                                           // интерфейс операций FTP

    // конструктор
    public ServersPresenter(@NonNull ServersView view, RealmService realmService,
                                                            FtpService ftpService) {
        mView = view;
        mRealmService = realmService;
        mFtpService = ftpService;
        mClient = new FTPClient();
    }

    // обновление списка серверов (извлечение из базы)
    public void refresh() {
        List<FtpServer> servers = mRealmService.getFtpServers();
        if (servers == null || servers.isEmpty()) mView.showEmpty();
        else mView.showServers(servers);
    }

    // удаление указанных серверов
    public void remove(@NonNull List<FtpServer> servers) {

        // удаление
        if (mRealmService.removeFtpServers(servers))
            mView.taskComplete("Successfully deleted");
        else
            mView.showMessage("Database error!", true);

        // обновление списка серверов
        refresh();
    }

    // поиск редактируемого сервера по имени
    public void find(String name) {

        // поиск редактируемого элемента в базе данных
        FtpServer server = mRealmService.findFtpServer(name);

        if (server != null) mView.showFoundedServer(server);
        else mView.showMessage("Item is not found. Database is corrupted!", true);
    }

    // добавление нового сервера в список
    public void add(@NonNull FtpServer server) {

        // поиск в базе элемента с таким же именем, как в поле ввода
        if (mRealmService.findFtpServer(server.getName()) != null)
            mView.showMessage("Server with specified name is already exists!", true);

        // проверка соединения с сервером
        else connect(server);
    }

    // редактирование указанного сервера
    public void edit(@NonNull FtpServer server, String serverName) {

        // поиск сервера по указанному имени
        FtpServer findServer = mRealmService.findFtpServer(server.getName());

        // имя сервера изменилось
        if (findServer == null) connect(server);

        // имя сервера не изменилось
        else if (findServer.getName().equals(serverName)) connect(server);

        // имя сервера изменилось, но сервер с указанным именем уже существует
        else mView.showMessage("Server with specified name is already exists!", true);
    }

    // соединение с сервером
    public void connect(@NonNull FtpServer server) {

        // попытка соединения
        mSubscription = mFtpService.connect(mClient, server, null)
                                    .doOnSubscribe(mView::showLoading)
                                    .doAfterTerminate(mView::hideLoading)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(mView::connectResult,
                                                throwable -> mView.showMessage(throwable.getMessage(),
                                                                                            true));
    }

    // отключение от сервера
    public void disconnect() {

        // попытка отключения
        mSubscription = mFtpService.disconnect(mClient)
                                    .doOnSubscribe(mView::showLoading)
                                    .doAfterTerminate(mView::hideLoading)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(mView::disconnectResult,
                                                throwable -> mView.showMessage(throwable.getMessage(),
                                                                                            true));
    }

    // сохранение сервера в базе данных
    public void save(FtpServer server, @NonNull String name) {

        // добавление
        if (name.isEmpty()) {
            mRealmService.addFtpServer(server);
            mView.taskComplete("Successfully added");
        }

        // редактирование
        else {
            if (mRealmService.editFtpServer(server, name))
                mView.taskComplete("Successfully edited");
            else
                mView.showMessage("Item is not found. Database is corrupted!", true);
        }
    }

    // постановка презентера на паузу
    public void pause() {

        // отписка от всех наблюдения
        if (mSubscription != null)
            mSubscription.unsubscribe();
    }

    // уничтожение презентера
    public void close() {
        mView = null;
        mClient = null;
    }
}