package ru.oepak22.smallftpclient.screen.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.util.Pair;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.util.List;
import java.util.Objects;

import io.realm.Realm;
import io.realm.RealmResults;
import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.Define;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.LocalFile;
import ru.oepak22.smallftpclient.content.RemoteFile;
import ru.oepak22.smallftpclient.data.FilesProvider;
import ru.oepak22.smallftpclient.general.LoadingDialog;
import ru.oepak22.smallftpclient.general.LoadingView;
import ru.oepak22.smallftpclient.screen.ftp.FtpStorageDialog;
import ru.oepak22.smallftpclient.screen.local.LocalStorageDialog;
import ru.oepak22.smallftpclient.screen.servers.EditServerActivity;
import ru.oepak22.smallftpclient.screen.ftp.FtpStorageFragment;
import ru.oepak22.smallftpclient.screen.local.LocalStorageFragment;
import ru.oepak22.smallftpclient.screen.servers.ListServersActivity;
import ru.oepak22.smallftpclient.screen.servers.ListServersDialog;
import ru.oepak22.smallftpclient.tasks.TasksFragment;
import ru.oepak22.smallftpclient.utils.TextUtils;

// класс основной активности приложения
public class MainActivity extends AppCompatActivity implements FtpConnectView {

    private LocalStorageFragment mLocalStorageFragment;                                                 // фрагмент для навигации по локальному хранилищу
    private FtpStorageFragment mFtpStorageFragment;                                                     // фрагмент для навигации по хранилищу FTP
    private TasksFragment mTasksFragment;                                                               // фрагмент для запуска асинхронных процессов

    private LoadingView mLoadingView;                                                                   // диалог загрузки

    private AlertDialog mDialog;                                                                        // диалог предупреждений/ошибок
    private LocalStorageDialog mLocalStorageDialog;                                                     // диалог выбора папки для скачивания файлов
    private ListServersDialog mListServersDialog;                                                       // диалог выбора сервера для загрузки файлов
    private FtpStorageDialog mFtpStorageDialog;                                                         // диалог выбора папки на сервере для загрузки файлов

    private DrawerLayout drawerLayout;                                                                  // навигатор бокового меню
    private ActionBarDrawerToggle actionBarDrawerToggle;                                                // переключатель меню для открытия навигатора

    private Realm mRealm;                                                                               // база данных
    private FtpConnectPresenter mFtpConnectPresenter;                                                   // презентер для активации соединения с сервером FTP
    private String mServerName;                                                                         // имя сервера для активации соединения

    private boolean isConnectingProcess;                                                                // флаг процесса соединения (true, если соединение в процессе установки)
    private boolean isOpenDialog;                                                                       // соединение устанавливается для открытия диалога, а не фрагмента

    private SharedPreferences mSharedPreferences;                                                       // хранилище флагов состояний трансфера

    // содание активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // флаги процесса соединения обнуляются
        isConnectingProcess = false;
        isOpenDialog = false;

        // инициализация базы данных
        mRealm = Realm.getDefaultInstance();

        // инициализация хранилища настроек
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // инициализация презентера
        mFtpConnectPresenter = new FtpConnectPresenter(this);

        // инициализация диалога загрузки
        mLoadingView = LoadingDialog.view(getSupportFragmentManager());

        // инициализация тулбара
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        // инициализация навигатора, обработка открытия/закрытия
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this,
                                                            drawerLayout,
                                                                    R.string.nav_open,
                                                                    R.string.nav_close) {
            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                setNavigationMenu();
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // инициализация меню навигатора
        setNavigationMenu();

        // инициализация фрагментов
        mLocalStorageFragment = (LocalStorageFragment) getSupportFragmentManager()
                                                        .findFragmentByTag("LOCAL_FRAG");
        mFtpStorageFragment = (FtpStorageFragment) getSupportFragmentManager()
                                                        .findFragmentByTag("REMOTE_FRAG");
        mTasksFragment = (TasksFragment) getSupportFragmentManager()
                                                        .findFragmentByTag("TASK_FRAG");

        if (mLocalStorageFragment != null) Objects.requireNonNull(getSupportActionBar())
                                                    .setTitle(R.string.local_storage);
        if (mFtpStorageFragment != null) Objects.requireNonNull(getSupportActionBar())
                                                    .setTitle(mServerName);

        if (mTasksFragment == null) {
            mTasksFragment = new TasksFragment();
            getSupportFragmentManager().beginTransaction()
                                        .add(mTasksFragment, "TASK_FRAG")
                                        .commit();
        }

        // регистрация ресивера
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Define.SERVICE_DOWNLOAD);
        intentFilter.addAction(Define.SERVICE_UPLOAD);
        intentFilter.addAction(Define.SERVICE_OTHER);
        registerReceiver(mBroadcastReceiver, intentFilter);

        // первый запуск приложения, переход в корень локального хранилища
        if (savedInstanceState == null) startLocalRoot();
        else {
            isOpenDialog = savedInstanceState.getBoolean("DIALOG");
            isConnectingProcess = savedInstanceState.getBoolean("CONNECTING");
            mServerName = savedInstanceState.getString("SERVER_NAME");
        }
    }

    // возобновление активности
    @Override
    protected void onResume() {
        super.onResume();
        if (isConnectingProcess && mFtpConnectPresenter != null)
            mFtpConnectPresenter.connect(mServerName, isOpenDialog);
    }

    // постановка активности на паузу
    @Override
    protected void onPause() {
        super.onPause();
        if (mFtpConnectPresenter != null)
            mFtpConnectPresenter.pause();
        if (mDialog != null)
            mDialog.dismiss();
        if (mLocalStorageDialog != null)
            mLocalStorageDialog.dismiss();
        if (mListServersDialog != null)
            mListServersDialog.dismiss();
        if (mFtpStorageDialog != null)
            mFtpStorageDialog.dismiss();
    }

    // уничтожение активности
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
        if (mFtpConnectPresenter != null)
            mFtpConnectPresenter.close();
        unregisterReceiver(mBroadcastReceiver);
    }

    // инициализация главного меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // обработчик главного меню
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        // кнопка открытия навигатора
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) return true;

        // кнопка настроек серверов
        if (item.getItemId() == R.id.action_servers) {
            Intent intent = new Intent(MainActivity.this, ListServersActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    // обработка результата запроса на доступ к локальной файловой системе
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startLocalRoot();
        else finish();
    }

    // сохранение данных перед поворотом экрана
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("SERVER_NAME", mServerName);
        outState.putBoolean("CONNECTING", isConnectingProcess);
        outState.putBoolean("DIALOG", isOpenDialog);
    }

    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        //
        if (requestCode == Define.SD_ROOT && resultCode == RESULT_OK) {

            Uri uri = data.getData();
            String path = uri.getPath();

            if (!path.split(":")[0].equals("/tree/primary")
                    && !path.split(":")[0].equals("/tree/downloads")) {

                String sd = FilesProvider.provideFilesService().getSDcard();

                if (sd != null) {

                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putString("SD-root", sd + "/" + path.split(":")[1]);
                    editor.putString("SD-uri", uri.toString());
                    editor.apply();

                    getContentResolver().takePersistableUriPermission(uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    //grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
            }
        }
    }

    // настройка меню для навигатора локального хранилища
    public void setNavigationMenu() {

        // определение и очистка меню навигатора
        NavigationView navigationView = findViewById(R.id.nav_view);
        Menu menu = navigationView.getMenu();
        menu.clear();

        // под-меню локального хранилища
        Menu localSubmenu = menu.addSubMenu(R.string.local_storage);

        // переход в корень хранилища
        Pair<Long, Long> space = FilesProvider
                                    .provideFilesService()
                                    .getSpace(Environment.getExternalStorageDirectory().getPath());

        localSubmenu.add(getResources().getStringArray(R.array.locals_with_sd)[0]
                                            + " (" + TextUtils.bytesToString(space.first)
                                            + " / " + TextUtils.bytesToString(space.second) + ")")
                    .setIcon(R.drawable.ic_menu_homepage)
                    .setOnMenuItemClickListener(menuItem -> {
                        drawerLayout.closeDrawers();
                        navigateLocalStorage(Environment.getExternalStorageDirectory().getPath());
                        return false;
                    });

        // переход в папку с изображениями
        localSubmenu.add(getResources().getStringArray(R.array.locals_with_sd)[1])
                    .setIcon(R.drawable.ic_menu_pictures)
                    .setOnMenuItemClickListener(menuItem -> {
                        drawerLayout.closeDrawers();
                        navigateLocalStorage(Environment.getExternalStorageDirectory().getPath()
                                                                                        + "/Pictures");
                        return false;
                    });

        // переход в папку загрузок
        localSubmenu.add(getResources().getStringArray(R.array.locals_with_sd)[2])
                    .setIcon(R.drawable.ic_menu_download)
                    .setOnMenuItemClickListener(menuItem -> {
                        drawerLayout.closeDrawers();
                        navigateLocalStorage(Environment.getExternalStorageDirectory().getPath()
                                                                                        + "/Download");
                        return false;
                    });

        // переход в папку с видео-файлами
        localSubmenu.add(getResources().getStringArray(R.array.locals_with_sd)[3])
                    .setIcon(R.drawable.ic_menu_movies)
                    .setOnMenuItemClickListener(menuItem -> {
                        drawerLayout.closeDrawers();
                        navigateLocalStorage(Environment.getExternalStorageDirectory().getPath()
                                                                                            + "/Movies");
                        return false;
                    });

        // переход в папку с музыкой
        localSubmenu.add(getResources().getStringArray(R.array.locals_with_sd)[4])
                    .setIcon(R.drawable.ic_menu_music)
                    .setOnMenuItemClickListener(menuItem -> {
                        drawerLayout.closeDrawers();
                        navigateLocalStorage(Environment.getExternalStorageDirectory().getPath()
                                                                                            + "/Music");
                        return false;
                    });

        // проверка наличия SD-карты
        String sd = FilesProvider.provideFilesService().getSDcard();
        if (sd != null) {

            space = FilesProvider.provideFilesService().getSpace(sd);

            // переход в SD-карту с дополнительной проверкой её наличия
            localSubmenu.add(getResources().getStringArray(R.array.locals_with_sd)[5]
                                    + " (" + TextUtils.bytesToString(space.first) + " / " +
                                                    TextUtils.bytesToString(space.second)+ ")")
                        .setIcon(R.drawable.ic_menu_sd)
                        .setOnMenuItemClickListener(menuItem -> {

                            // проверка наличия SD-карты
                            String sdPath = FilesProvider.provideFilesService().getSDcard();

                            // SD-карта в наличии
                            if (sdPath != null) {
                                if (mSharedPreferences.contains("SD-root")) {

                                    // проверка на наличие корневого каталога SD-карты
                                    File file = new File(mSharedPreferences.getString("SD-root",
                                                                                            sdPath));
                                    if (file.exists())
                                        navigateLocalStorage(mSharedPreferences.getString("SD-root",
                                                                                                sdPath));
                                    else startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                                                                                                Define.SD_ROOT);
                                }
                                else startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                                                                                            Define.SD_ROOT);
                            }

                            // SD-карта недоступна
                            else {
                                showMessage("Could not open SD card!", true);
                                localSubmenu.removeItem(5);
                                localSubmenu.removeItem(6);
                                navigationView.invalidate();
                            }
                            drawerLayout.closeDrawers();
                            return false;
                        });

            //
            localSubmenu.add(R.string.set)
                    .setOnMenuItemClickListener(menuItem -> {
                        if (FilesProvider.provideFilesService().getSDcard() != null)
                            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                                                                                    Define.SD_ROOT);
                        else {
                            showMessage("Could not open SD card!", true);
                            localSubmenu.removeItem(5);
                            localSubmenu.removeItem(6);
                            navigationView.invalidate();
                        }
                        drawerLayout.closeDrawers();
                        return false;
                    });
        }

        // под-меню серверов FTP
        Menu remoteSubmenu = menu.addSubMenu(R.string.servers);

        // переход к добавлению нового сервера
        remoteSubmenu.add(R.string.add)
                .setOnMenuItemClickListener(menuItem -> {
                    drawerLayout.closeDrawers();
                    Intent intent = new Intent(MainActivity.this,
                                                        EditServerActivity.class);
                    intent.putExtra("SERVER_NAME", "");
                    startActivity(intent);
                    return false;
                });

        // поиск ранее добавленных серверов в базе данных
        RealmResults<FtpServer> results = mRealm.where(FtpServer.class).findAll();
        List<FtpServer> servers = mRealm.copyFromRealm(results);

        // добавление найденных серверов
        for (FtpServer server : servers) {
            remoteSubmenu.add(server.getName())
                    .setIcon(R.drawable.ic_baseline_server_24)
                    .setOnMenuItemClickListener(menuItem -> {
                        drawerLayout.closeDrawers();
                        mServerName = menuItem.getTitle().toString();
                        isOpenDialog = false;
                        mFtpConnectPresenter.connect(mServerName, false);
                        return false;
                    });
        }

    }

    // вызов при первом запуске приложения - получение пути до корневой папки локального хранилища
    public void startLocalRoot() {

        // разрешения нет - нужен вызов запроса
        if (ActivityCompat
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat
                    .requestPermissions(MainActivity.this,
                            new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                            1);
        }

        // разрешение получено - переход в корень локального хранилища
        else navigateLocalStorage(Environment.getExternalStorageDirectory().getPath());
    }

    // переход по локальному хранилищу
    public void navigateLocalStorage(String path) {

        // инициализация фрагмента
        mLocalStorageFragment = new LocalStorageFragment();

        // передача фрагменту параметра - пути к корню локального хранилища
        Bundle bundle = new Bundle();
        bundle.putString("START_DIR", path);
        mLocalStorageFragment.setArguments(bundle);

        // добавление фрагмента в контейнер
        getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.container, mLocalStorageFragment, "LOCAL_FRAG")
                                    .commit();

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.local_storage);
    }

    //
    @Override
    public void connectSuccess(FTPClient client, FtpServer server, boolean isDialog) {

        // открывается фрагмент хранилища FTP
        if (!isDialog) {

            // инициализация фрагмента
            mFtpStorageFragment = new FtpStorageFragment();
            mFtpStorageFragment.init(server, client);

            // добавление фрагмента в контейнер
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, mFtpStorageFragment, "REMOTE_FRAG")
                    .commit();

            Objects.requireNonNull(getSupportActionBar()).setTitle(mServerName);
        }

        // открывается диалог для выбора папки в хранилище FTP
        else {
            mFtpStorageDialog = new FtpStorageDialog();
            mFtpStorageDialog.init(server, client, Define.UPLOAD_TASK);
            mFtpStorageDialog.show(getSupportFragmentManager(), "FtpStorageDialog");
        }
    }

    // вывод сообщения об ошибке
    @Override
    public void showMessage(String msg, boolean isError) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        if (isError) {
            builder.setTitle(R.string.message_error);
            builder.setIcon(R.drawable.ic_message_error);
        }
        else {
            builder.setTitle(R.string.message_warning);
            builder.setIcon(R.drawable.ic_message_warning);
        }
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.message_ok, (dialog, which) -> dialog.dismiss());
        mDialog = builder.create();
        mDialog.show();
    }

    // открытие индикатора загрузки
    @Override
    public void showLoading() {
        mLoadingView.showLoading();
        isConnectingProcess = true;
    }

    // закрытие индикатора загрузки
    @Override
    public void hideLoading() {
        mLoadingView.hideLoading();
        isConnectingProcess = false;
    }

    // проверка, идет ли загрузка файлов в данный момент
    public void checkUploadRunning() {

        // проверка, идет ли загрузка в данный момент
        if (mTasksFragment.isUploadRunning()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.message_warning);
            builder.setIcon(R.drawable.ic_message_warning);
            builder.setMessage(R.string.message_upload_cancel);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.message_abort, (dialog, which) -> mTasksFragment.stopUpload());
            builder.setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());
            mDialog = builder.create();
            mDialog.show();
        }
        else selectUploadServer();
    }

    // открытие диалога выбора сервера для загрузки файлов
    public void selectUploadServer() {

        // поиск ранее добавленных серверов в базе данных
        RealmResults<FtpServer> results = mRealm.where(FtpServer.class).findAll();
        List<FtpServer> servers = mRealm.copyFromRealm(results);

        // проверка на наличие серверов
        if (servers == null || servers.isEmpty())
            showMessage("There are not servers for upload files!", true);
        else {
            FragmentManager manager = getSupportFragmentManager();
            mListServersDialog = new ListServersDialog();

            // открытие диалога выбора сервера для загрузки файлов
            mListServersDialog.show(manager, "ListServersDialog");
        }
    }

    // открытие диалога выбора целевой папки для загрузки файлов
    public void openUploadDialog(String name) {
        isOpenDialog = true;
        mServerName = name;
        mFtpConnectPresenter.connect(mServerName, true);
    }

    // загрузка/копирование/перемещение файлов
    public void uploadCopyMoveFiles(FtpServer server, String path, int operationID) {

        // загрузка файлов
        if (operationID == Define.UPLOAD_TASK) {
            if (mLocalStorageFragment != null && mTasksFragment != null)
                mTasksFragment.startUpload(server, mLocalStorageFragment.getSelectedItems(),
                        mLocalStorageFragment.getCurrentDir(), path);
        }
        else if (mFtpStorageFragment != null && mTasksFragment != null) {

            // копирование файлов
            if (operationID == Define.COPY_TASK)
                mTasksFragment.startCopy(server, null,
                                            mFtpStorageFragment.getSelectedItems(),
                                            mFtpStorageFragment.getCurrentPath(), path, 0);

            // перемещение файлов
            else
                mTasksFragment.startMove(server, null,
                                            mFtpStorageFragment.getSelectedItems(),
                                            mFtpStorageFragment.getCurrentPath(), path, 0);
        }
    }

    // проверка, идет ли скачивание файлов в данный момент
    public void checkDownloadRunning() {

        if (mTasksFragment.isDownloadRunning()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.message_warning);
            builder.setIcon(R.drawable.ic_message_warning);
            builder.setMessage(R.string.message_download_cancel);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.message_abort, (dialog, which) -> mTasksFragment.stopDownload());
            builder.setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());
            mDialog = builder.create();
            mDialog.show();
        }
        else selectDownloadCopyMovePath(Define.DOWNLOAD_TASK);

    }

    // открытие диалога выбора целевого пути скачивания/копирования/перемещения файлов
    public void selectDownloadCopyMovePath(int operationID) {

        // проверка на наличие SD-карты
        int array_id;
        String sdPath = FilesProvider.provideFilesService().getSDcard();
        if (sdPath != null && mSharedPreferences.contains("SD-root")) {

            // проверка на наличие корневого каталога SD-карты
            File file = new File(mSharedPreferences.getString("SD-root", sdPath));
            if (file.exists()) array_id = R.array.locals_with_sd;
            else array_id = R.array.locals_without_sd;
        }
        else array_id = R.array.locals_without_sd;

        // формирование диалогового окна со списком возможных целевых путей для скачивания
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setIcon(R.drawable.ic_textview_path);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());
        builder.setTitle(R.string.message_download_path)
                    .setItems(array_id, (dialog, which) -> {

                        switch (which) {

                            case 0: openDownloadCopyMoveDialog(Environment
                                                                .getExternalStorageDirectory()
                                                                        .getPath(), operationID);
                                    break;

                            case 1: openDownloadCopyMoveDialog(Environment
                                                                    .getExternalStorageDirectory()
                                                                            .getPath() + "/Pictures",
                                                                                        operationID);
                                    break;

                            case 2: openDownloadCopyMoveDialog(Environment
                                                                .getExternalStorageDirectory()
                                                                        .getPath() + "/Download",
                                                                                    operationID);
                                    break;

                            case 3: openDownloadCopyMoveDialog(Environment
                                                                    .getExternalStorageDirectory()
                                                                            .getPath() + "/Movies",
                                                                                        operationID);
                                    break;

                            case 4: openDownloadCopyMoveDialog(Environment
                                                                    .getExternalStorageDirectory()
                                                                            .getPath() + "/Music",
                                                                                        operationID);
                                    break;

                            case 5:
                                if (FilesProvider.provideFilesService().getSDcard() == null)
                                    Toast.makeText(MainActivity.this,
                                        R.string.toast_sd_error,
                                        Toast.LENGTH_SHORT).show();
                                else if (mSharedPreferences.contains("SD-root"))
                                    openDownloadCopyMoveDialog(mSharedPreferences
                                                                .getString("SD-root", null),
                                                                                        operationID);
                                break;
                        }
                    });

        mDialog = builder.create();
        mDialog.show();
    }

    // открытие диалога выбора целевой папки для скачивания/перемещения/копирования файлов
    public void openDownloadCopyMoveDialog(String path, int operationID) {

        // инициализация фрагмента
        mLocalStorageDialog = new LocalStorageDialog();

        // передача фрагменту параметра - пути к корню локального хранилища
        Bundle bundle = new Bundle();
        bundle.putString("START_DIR", path);
        bundle.putInt("OP_ID", operationID);
        mLocalStorageDialog.setArguments(bundle);

        // открытие диалога выбора папки для скачивания файлов
        mLocalStorageDialog.show(getSupportFragmentManager(), "LocalStorageDialog");
    }

    // скачивание/копирование/перемещение файлов
    public void downloadCopyMoveFiles(String path, long space, int operationID) {

        if (operationID == Define.DOWNLOAD_TASK) {

            // скачивание файлов
            if (mFtpStorageFragment != null && mTasksFragment != null) {
                mTasksFragment.startDownload(mFtpStorageFragment.getServer(),
                                                mFtpStorageFragment.getSelectedItems(),
                                                path, mFtpStorageFragment.getCurrentPath(), space);
            }
        }
        else if (mLocalStorageFragment != null && mTasksFragment != null) {

            // копирование файлов
            if (operationID == Define.COPY_TASK)
                mTasksFragment.startCopy(null, mLocalStorageFragment.getSelectedItems(),
                                            null, mLocalStorageFragment.getCurrentDir(),
                                                                                        path, space);

            // перемещение файлов
            else
                mTasksFragment.startMove(null, mLocalStorageFragment.getSelectedItems(),
                                            null, mLocalStorageFragment.getCurrentDir(),
                                                                                        path, space);
        }
    }

    // проверка, идёт ли какая-либо операция с файлами
    public void checkCopyMoveRunning(FTPClient client, FtpServer server, int operationID) {

        if (mTasksFragment.isFileOperationRunning()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.message_warning);
            builder.setIcon(R.drawable.ic_message_warning);
            builder.setMessage(R.string.message_other_cancel);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.message_abort, (dialog, which) -> mTasksFragment.stopFileOperation());
            builder.setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());
            mDialog = builder.create();
            mDialog.show();
        }
        else {

            // перемещение/копирование файлов на сервере FTP
            if (client != null && server != null) {
                if (client.isConnected()) {
                    mFtpStorageDialog = new FtpStorageDialog();
                    mFtpStorageDialog.init(server, client, operationID);
                    mFtpStorageDialog.show(getSupportFragmentManager(), "FtpStorageDialog");
                }
                else showMessage("Connection failed!", true);
            }

            // перемещение/копирование файлов в локальном хранилище
            else selectDownloadCopyMovePath(operationID);
        }
    }

    // удаление выбранных файлов
    public void deleteFiles(FtpServer server, String sourcePath, String destinationPath,
                                List<LocalFile> localFiles, List<RemoteFile> remoteFiles) {

        if (mTasksFragment.isFileOperationRunning()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.message_warning);
            builder.setIcon(R.drawable.ic_message_warning);
            builder.setMessage(R.string.message_other_cancel);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.message_abort, (dialog, which) -> mTasksFragment.stopFileOperation());
            builder.setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());
            mDialog = builder.create();
            mDialog.show();
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.message_warning);
            builder.setMessage(R.string.message_sure_delete);
            builder.setIcon(R.drawable.ic_message_warning);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.message_delete,
                    (dialog, which) -> mTasksFragment.startDelete(server, localFiles,
                                                                    remoteFiles, sourcePath,
                                                                                destinationPath)
            );
            builder.setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());
            mDialog = builder.create();
            mDialog.show();
        }
    }

    // открытие диалога для ввода текста (создание/переименование элементов)
    public void openEditDialog(String text, int operation) {

        // инициализация диалогового окна с полем ввода
        final EditText input = new EditText(MainActivity.this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // размеры поля ввода
        input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                            LinearLayout.LayoutParams.MATCH_PARENT));

        // подсказка в поле ввода
        if (operation == Define.LOCAL_FILE_RENAME || operation == Define.FTP_FILE_RENAME)
            input.setHint(R.string.file_name);
        else input.setHint(R.string.folder_name);

        // текст в поле (в одну строку)
        input.setText(text);
        input.setSingleLine();

        // заголовок сообщения
        if (operation == Define.LOCAL_DIR_CREATE || operation == Define.FTP_DIR_CREATE)
            builder.setTitle(R.string.message_new_folder);
        else if (operation == Define.LOCAL_FILE_RENAME || operation == Define.FTP_FILE_RENAME)
            builder.setTitle(R.string.message_rename_file);
        else builder.setTitle(R.string.message_rename_folder);

        // иконка сообщения
        if (operation == Define.LOCAL_DIR_CREATE || operation == Define.FTP_DIR_CREATE)
            builder.setIcon(R.drawable.ic_textview_path);
        else builder.setIcon(R.drawable.ic_textview_name);

        // обработка кнопки "ОК"
        builder.setPositiveButton(R.string.message_ok, (dialog, which) -> {
            String inputText = input.getText().toString();
            if (!inputText.isEmpty()) {

                // выполнение соответствующей операции
                switch (operation) {

                    // переименование локального файла
                    case Define.LOCAL_FILE_RENAME:
                        if (mLocalStorageFragment != null) {
                            if (mLocalStorageFragment.renameFile(inputText))
                                Toast.makeText(this, "Successfully renamed",
                                                                    Toast.LENGTH_SHORT).show();
                            else showMessage("Could not rename an item!", true);
                        }
                        break;

                    // создание локального каталога
                    case Define.LOCAL_DIR_CREATE:
                        if (mLocalStorageFragment != null) {
                            if (mLocalStorageFragment.createNewFolder(inputText))
                                Toast.makeText(this, "Successfully created",
                                                                    Toast.LENGTH_SHORT).show();
                            else showMessage("Could not create a directory!", true);
                        }
                        break;

                    // переименование файла FTP
                    case Define.FTP_FILE_RENAME:
                        if (mFtpStorageFragment != null)
                            mFtpStorageFragment.renameFile(inputText);
                        break;

                    // создание каталога FTP
                    case Define.FTP_DIR_CREATE:
                        if (mFtpStorageFragment != null)
                            mFtpStorageFragment.createNewFolder(inputText);
                        break;
                }
            }
        });

        // открытие диалогового окна
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());
        mDialog = builder.create();
        mDialog.setView(input);
        mDialog.show();
    }

    // закрытие приложения
    public void closeApplication() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.message_warning);
        builder.setMessage(R.string.message_sure_exit);
        builder.setIcon(R.drawable.ic_message_warning);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.message_quit, (dialog, which) -> finish());
        builder.setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());
        mDialog = builder.create();
        mDialog.show();
    }

    // широковещательный ресивер для приема сообщений от процессов скачивания/загрузки
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, @NonNull Intent intent) {

            if (intent.getAction().equals(Define.SERVICE_DOWNLOAD)) {
                mTasksFragment.stopDownload();
                Log.v("DownloadTask", "Send cancel");
            }
            else if (intent.getAction().equals(Define.SERVICE_UPLOAD)) {
                mTasksFragment.stopUpload();
                Log.v("UploadTask", "Send cancel");
            }
            else if (intent.getAction().equals(Define.SERVICE_OTHER)) {
                mTasksFragment.stopFileOperation();
                Log.v("FileOperationTask", "Send cancel");
            }
        }
    };
}