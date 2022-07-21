package ru.oepak22.smallftpclient;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MainApp extends Application {

    //private static AppComponent sAppComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        // инициализация realm для кэширования
        Realm.init(this);
        RealmConfiguration configuration = new RealmConfiguration.Builder().build();
        Realm.setDefaultConfiguration(configuration);

        /*sAppComponent = DaggerAppComponent.builder()
                .dataModule(new DataModule())
                .build();*/
    }

    /*@NonNull
    public static AppComponent getAppComponent() {
        return sAppComponent;
    }*/
}
