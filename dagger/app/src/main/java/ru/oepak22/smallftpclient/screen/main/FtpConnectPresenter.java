package ru.oepak22.smallftpclient.screen.main;

import androidx.annotation.NonNull;

import org.apache.commons.net.ftp.FTPClient;

import ru.oepak22.smallftpclient.data.FtpService;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.data.RealmService;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

// класс презентера установки соединения с сервером
public class FtpConnectPresenter {

    private FtpConnectView mView;                                                                       // интерфейс презентера
    private Subscription mSubscription;                                                                 // реактивный наблюдатель
    private FTPClient mClient;                                                                          // клиент FTP для установки соединения
    private FtpServer mServer;                                                                          // параметры сервера
    private final RealmService mRealmService;                                                           // интерфейс операций Realm
    private final FtpService mFtpService;                                                               // интерфейс операций FTP

    // конструктор
    public FtpConnectPresenter(@NonNull FtpConnectView view, RealmService realmService,
                                                                    FtpService ftpService) {
        mView = view;
        mRealmService = realmService;
        mFtpService = ftpService;
    }
    
    // подключение к указанному серверу
    public void connect(String name, boolean isDialog) {

        mServer = mRealmService.findFtpServer(name);
        if (mServer != null) {
            mClient = new FTPClient();
            mSubscription = mFtpService.connect(mClient, mServer, null)
                                        .doOnSubscribe(mView::showLoading)
                                        .doOnTerminate(mView::hideLoading)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(status -> {
                                                        if (status.equals("Success"))
                                                            mView.connectSuccess(mClient, mServer, isDialog);
                                                        else mView.showMessage(status, true);
                                                        mClient = null;
                                                    },
                                                    throwable -> mView.showMessage(throwable.getMessage(),
                                                                                                true));
        }
        else mView.showMessage("Item is not found. Database is corrupted!", true);
    }

    // постановка презентера на паузу
    public void pause() {

        // отписка от наблюдения
        if (mSubscription != null)
            mSubscription.unsubscribe();
    }

    // уничтожение презентера
    public void close() {
        mView = null;
        mClient = null;
    }

}
