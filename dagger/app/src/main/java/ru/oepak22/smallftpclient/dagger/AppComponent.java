package ru.oepak22.smallftpclient.dagger;

import javax.inject.Singleton;

import dagger.Component;
import ru.oepak22.smallftpclient.screen.ftp.FtpStorageDialog;
import ru.oepak22.smallftpclient.screen.ftp.FtpStorageFragment;
import ru.oepak22.smallftpclient.screen.local.LocalStorageDialog;
import ru.oepak22.smallftpclient.screen.local.LocalStorageFragment;
import ru.oepak22.smallftpclient.screen.main.MainActivity;
import ru.oepak22.smallftpclient.screen.servers.EditServerActivity;
import ru.oepak22.smallftpclient.screen.servers.ListServersActivity;
import ru.oepak22.smallftpclient.screen.servers.ListServersDialog;
import ru.oepak22.smallftpclient.tasks.download.DownloadTask;
import ru.oepak22.smallftpclient.tasks.management.CopyMoveDeleteTask;
import ru.oepak22.smallftpclient.tasks.upload.UploadTask;


//интерфейс для внедрения зависимостей в активности и задачи
@Singleton
@Component(modules = {DataModule.class})
public interface AppComponent {

    void injectMainActivity(MainActivity mainActivity);
    void injectFtpStorageFragment(FtpStorageFragment ftpStorageFragment);
    void injectFtpStorageDialog(FtpStorageDialog ftpStorageDialog);
    void injectLocalStorageFragment(LocalStorageFragment localStorageFragment);
    void injectLocalStorageDialog(LocalStorageDialog localStorageDialog);
    void injectEditServerActivity(EditServerActivity editServerActivity);
    void injectListServersActivity(ListServersActivity listServersActivity);
    void injectListServersDialog(ListServersDialog listServersDialog);

    void injectDownloadTask(DownloadTask downloadTask);
    void injectUploadTask(UploadTask uploadTask);
    void injectCopyMoveDeleteTask(CopyMoveDeleteTask copyMoveDeleteTask);
}