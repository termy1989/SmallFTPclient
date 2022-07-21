package ru.oepak22.smallftpclient.data;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

// провайдер базы данных Realm
public final class RealmProvider {

    // экземпляр интерфейса
    private static RealmService sRealmService;

    // конструктор
    private RealmProvider() {}

    // инициализатор
    @MainThread
    public static void init() {
        sRealmService = new RealmOperations();
    }

    // проверка на то, что операционный класс проинициализирован
    @NonNull
    public static RealmService provideServiceRealm() {
        if (sRealmService == null) {
            sRealmService = new RealmOperations();
        }
        return sRealmService;
    }

    // установка переопределенного операционного класса (для тестирования)
    public static void setServiceRealm(RealmService realmService) {
        sRealmService = realmService;
    }
}
