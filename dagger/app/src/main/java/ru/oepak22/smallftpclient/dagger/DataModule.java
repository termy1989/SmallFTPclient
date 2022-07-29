package ru.oepak22.smallftpclient.dagger;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.realm.Realm;
import ru.oepak22.smallftpclient.data.FilesOperations;
import ru.oepak22.smallftpclient.data.FilesService;
import ru.oepak22.smallftpclient.data.FtpOperations;
import ru.oepak22.smallftpclient.data.FtpService;
import ru.oepak22.smallftpclient.data.RealmOperations;
import ru.oepak22.smallftpclient.data.RealmService;

// класс поставщика зависимостей
@Module
public class DataModule {

    // предоставление зависимости Realm
    // зависимость предоставляется один раз
    @Provides
    @Singleton
    Realm provideRealm() {
        return Realm.getDefaultInstance();
    }

    // предоставление зависимости FilesService
    // зависимость предоставляется один раз
    @Provides
    @Singleton
    FilesService provideFilesService() {
        return new FilesOperations();
    }

    // предоставление зависимости FtpService
    // зависимость предоставляется один раз
    @Provides
    @Singleton
    FtpService provideFtpService() {
        return new FtpOperations();
    }

    // предоставление зависимости RealmService
    // зависимость предоставляется один раз
    @Provides
    @Singleton
    RealmService provideRealmService() {
        return new RealmOperations();
    }
}
