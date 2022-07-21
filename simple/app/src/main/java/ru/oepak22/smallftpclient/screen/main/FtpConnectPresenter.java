package ru.oepak22.smallftpclient.screen.main;

import androidx.annotation.NonNull;

import org.apache.commons.net.ftp.FTPClient;

import ru.oepak22.smallftpclient.data.FtpProvider;
import ru.oepak22.smallftpclient.data.RealmProvider;
import ru.oepak22.smallftpclient.content.FtpServer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

// класс презентера установки соединения с сервером
public class FtpConnectPresenter {

    private FtpConnectView mView;                                                                       // интерфейс презентера
    private Subscription mSubscription;                                                                 // реактивный наблюдатель
    private FTPClient mClient;                                                                          // клиент FTP для установки соединения
    private FtpServer mServer;                                                                          // параметры сервера

    // конструктор
    public FtpConnectPresenter(@NonNull FtpConnectView view) {
        mView = view;
    }
    
    // подключение к указанному серверу
    public void connect(String name, boolean isDialog) {

        mServer = RealmProvider.provideServiceRealm().findFtpServer(name);
        if (mServer != null) {
            mClient = new FTPClient();
            mSubscription = FtpProvider.provideServiceFTP()
                    .connect(mClient, mServer, null)
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
                            throwable -> mView.showMessage(throwable.getMessage(), true));
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
