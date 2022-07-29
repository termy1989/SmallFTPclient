package ru.oepak22.smallftpclient.content;

import io.realm.RealmObject;

// класс настроек сервера FTP для соединения
public class FtpServer extends RealmObject {

    private String mName;                                                           // имя сервера
    private String mAddress;                                                        // адрес сервера
    private int mPort;                                                              // порт сервера
    private String mPath;                                                           // корневой каталог сервера
    private boolean mAnon;                                                          // флаг анонимного соединения
    private String mUsername;                                                       // имя пользователя
    private String mPassword;                                                       // пароль
    private boolean mSelect;                                                        // флаг выделения в списке

    // конструктор
    public FtpServer() {
        mSelect = false;
    }

    // GET и SET имени сервера
    public String getName() { return mName; }
    public void setName(String name) { mName = name; }

    // GET и SET адреса сервера
    public String getAddress() { return mAddress; }
    public void setAddress(String address) { mAddress = address; }

    // GET и SET порта сервера
    public int getPort() { return mPort; }
    public void setPort(int port) { mPort = port; }

    // GET и SET стартового каталога сервера
    public String getPath() { return mPath; }
    public void setPath(String path) { mPath = path; }

    // GET и SET режима анонимного соединения
    public boolean isAnon() { return mAnon; }
    public void setAnon(boolean anon) { mAnon = anon; }

    // GET и SET имени пользователя
    public String getUsername() { return mUsername; }
    public void setUsername(String name) { mUsername = name; }

    // GET и SET пароля
    public String getPassword() { return mPassword; }
    public void setPassword(String password) { mPassword = password; }

    // GET и SET выделения
    public boolean isSelected() { return mSelect; }
    public void setSelected() { mSelect = !mSelect; }
}
