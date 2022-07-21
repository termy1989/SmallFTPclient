package ru.oepak22.smallftpclient.screen.servers;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.commons.net.ftp.FTPClient;

import java.util.List;
import java.util.Objects;

import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.general.LoadingDialog;
import ru.oepak22.smallftpclient.general.LoadingView;

// класс окна добавления сервера FTP
public class EditServerActivity extends AppCompatActivity implements ServersView {

    // поля ввода
    private EditText serverNameText;
    private EditText serverAddressText;
    private EditText serverPortText;
    private EditText serverPathText;
    private EditText serverUsernameText;
    private EditText serverPasswordText;
    private CheckBox anonCheck;

    private ServersPresenter mPresenter;                                                        // презентер списка серверов
    private LoadingView mLoadingView;                                                           // диалог загрузки
    private AlertDialog mDialog;                                                                // диалог предупреждений/ошибок

    private String mServerName;                                                                 // переданное имя сервера для редактирования
    private boolean isConnectingProcess;                                                        // флаг процесса соединения (true, если соединение в процессе установки)
    private boolean isClicked;                                                                  // флаг нажатия кнопки



    // создание активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_server);

        // инициализация диалога загрузки
        mLoadingView = LoadingDialog.view(getSupportFragmentManager());

        // флаг процесса соединения обнуляется
        isConnectingProcess = false;

        // флаг нажатия кнопки обнуляется
        isClicked = false;

        // компоненты активности
        serverNameText = findViewById(R.id.edit_server_name);
        serverAddressText = findViewById(R.id.edit_server_address);
        serverPortText = findViewById(R.id.edit_server_port);
        serverPathText = findViewById(R.id.edit_server_path);
        serverUsernameText = findViewById(R.id.edit_server_username);
        serverPasswordText = findViewById(R.id.edit_server_password);

        // инициализация кнопки
        FloatingActionButton mButtonApp = findViewById(R.id.fab_app);
        mButtonApp.setOnClickListener(view -> {

            // проверка на заполненность полей
            if (serverNameText.getText().toString().isEmpty() ||
                    serverAddressText.getText().toString().isEmpty() ||
                    (serverUsernameText.getText().toString().isEmpty() &&
                            serverUsernameText.getVisibility() == View.VISIBLE)) {
                showMessage("Please, fill in all required fields", false);
            }

            // создание нового сервера FTP или редактирование существующего
            else if (!isClicked) {
                isClicked = true;
                if (mServerName.isEmpty()) mPresenter.add(createServerFTP());
                else mPresenter.edit(createServerFTP(), mServerName);
            }
        });

        // обработчик checkBox
        anonCheck = findViewById(R.id.check_anonym);
        anonCheck.setOnClickListener(view -> {
            if (serverUsernameText.getVisibility() == View.VISIBLE
                    && serverPasswordText.getVisibility() == View.VISIBLE) {
                serverUsernameText.setVisibility(View.GONE);
                serverPasswordText.setVisibility(View.GONE);
            }
            else {
                serverUsernameText.setVisibility(View.VISIBLE);
                serverPasswordText.setVisibility(View.VISIBLE);
            }
        });

        // извлечение переданного имени сервера для редактирования
        mServerName = getIntent().getStringExtra("SERVER_NAME");

        // инициализация презентера
        mPresenter = new ServersPresenter(this);

        // тулбар
        Toolbar toolbar = findViewById(R.id.toolbar_edit_server);
        setSupportActionBar(toolbar);

        // активность открыта в режиме редактирования
        if (!mServerName.isEmpty()) {
            Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.edit_server);
            mPresenter.find(mServerName);
        }

        // активность открыта в режиме добавления
        else Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.add_server);

        // восстановление работы после поворота экрана
        if (savedInstanceState != null) {

            isConnectingProcess = savedInstanceState.getBoolean("SERVER_CONNECTING");

            serverNameText.setText(savedInstanceState.getString("SERVER_NAME_TEXT"));
            serverAddressText.setText(savedInstanceState.getString("SERVER_ADDRESS_TEXT"));
            serverPortText.setText(savedInstanceState.getString("SERVER_PORT_TEXT"));
            serverPathText.setText(savedInstanceState.getString("SERVER_PATH_TEXT"));
            serverUsernameText.setText(savedInstanceState.getString("SERVER_USERNAME_TEXT"));
            serverPasswordText.setText(savedInstanceState.getString("SERVER_PASSWORD_TEXT"));

            serverUsernameText.setVisibility(savedInstanceState.getInt("SERVER_USERNAME_VISIBILITY"));
            serverPasswordText.setVisibility(savedInstanceState.getInt("SERVER_PASSWORD_VISIBILITY"));

            anonCheck.setChecked(serverUsernameText.getVisibility() != View.VISIBLE ||
                                    serverPasswordText.getVisibility() != View.VISIBLE);
        }
    }

    // возобновление активности
    @Override
    protected void onResume() {
        super.onResume();

        // возобновление попытки подключиться к серверу
        if (isConnectingProcess && mPresenter != null)
            mPresenter.connect(createServerFTP());
    }

    // постановка активности на паузу
    @Override
    protected void onPause() {
        super.onPause();
        if (mPresenter != null)
            mPresenter.pause();
        if (mDialog != null)
            mDialog.dismiss();
    }

    // уничтожение активности
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenter != null)
            mPresenter.close();
        if (anonCheck != null)
            anonCheck.setOnClickListener(null);
    }

    // сохранение данных перед поворотом экрана
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("SERVER_NAME_TEXT", serverNameText.getText().toString());
        outState.putString("SERVER_ADDRESS_TEXT", serverAddressText.getText().toString());
        outState.putString("SERVER_PORT_TEXT", serverPortText.getText().toString());
        outState.putString("SERVER_PATH_TEXT", serverPathText.getText().toString());

        outState.putInt("SERVER_USERNAME_VISIBILITY", serverUsernameText.getVisibility());
        outState.putInt("SERVER_PASSWORD_VISIBILITY", serverPasswordText.getVisibility());
        outState.putString("SERVER_USERNAME_TEXT", serverUsernameText.getText().toString());
        outState.putString("SERVER_PASSWORD_TEXT", serverPasswordText.getText().toString());

        outState.putBoolean("SERVER_CONNECTING", isConnectingProcess);
    }

    // инициализация сервера FTP
    @NonNull
    private FtpServer createServerFTP() {

        FtpServer server = new FtpServer();
        server.setName(serverNameText.getText().toString());
        server.setAddress(serverAddressText.getText().toString());
        if (serverPortText.getText().toString().isEmpty()) server.setPort(FTPClient.DEFAULT_PORT);
        else server.setPort(Integer.parseInt(serverPortText.getText().toString()));
        if (serverPathText.getText().toString().isEmpty()) server.setPath("/");
        else server.setPath(serverPathText.getText().toString());
        if (serverUsernameText.getVisibility() == View.GONE &&
                serverPasswordText.getVisibility() == View.GONE) server.setAnon(true);
        else {
            server.setAnon(false);
            server.setUsername(serverUsernameText.getText().toString());
            server.setPassword(serverPasswordText.getText().toString());
        }
        return server;
    }

    // сервер для редактирования найден
    @Override
    public void showFoundedServer(@NonNull FtpServer server) {

        // заполнение полей активности параметрами найденного сервера
        serverNameText.setText(server.getName());
        serverAddressText.setText(server.getAddress());
        serverPortText.setText(String.valueOf(server.getPort()));
        serverPathText.setText(server.getPath());
        anonCheck.setChecked(server.isAnon());
        if (server.isAnon()) {
            serverUsernameText.setVisibility(View.GONE);
            serverPasswordText.setVisibility(View.GONE);
        }
        else {
            serverUsernameText.setVisibility(View.VISIBLE);
            serverPasswordText.setVisibility(View.VISIBLE);
            serverUsernameText.setText(server.getUsername());
            serverPasswordText.setText(server.getPassword());
        }
    }

    // обработка результатов попытки соединения с сохраняемым сервером
    @Override
    public void connectResult(@NonNull String status) {
        if (status.equals("Success")) mPresenter.disconnect();
        else showMessage(status, true);
    }

    // обработка результатов попытки разъединения с сохраняемым сервером
    @Override
    public void disconnectResult(boolean status) {
        if (status) mPresenter.save(createServerFTP(), mServerName);
        else showMessage("Disconnection failed!", true);
    }

    // вывод сообщения об ошибке (или предупреждения)
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
        isClicked = false;
    }

    // открытие диалога загрузки
    @Override
    public void showLoading() {
        mLoadingView.showLoading();
        isConnectingProcess = true;
    }

    // закрытие диалога загрузки
    @Override
    public void hideLoading() {
        mLoadingView.hideLoading();
        isConnectingProcess = false;
    }

    // завершение работы
    @Override
    public void taskComplete(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void showServers(@NonNull List<FtpServer> servers) {}

    @Override
    public void showEmpty() {}
}