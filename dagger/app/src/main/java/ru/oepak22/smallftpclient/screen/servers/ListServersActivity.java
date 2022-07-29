package ru.oepak22.smallftpclient.screen.servers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import ru.oepak22.smallftpclient.MainApp;
import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.data.FtpService;
import ru.oepak22.smallftpclient.data.RealmService;
import ru.oepak22.smallftpclient.widget.BaseAdapter;
import ru.oepak22.smallftpclient.widget.DividerItemDecoration;
import ru.oepak22.smallftpclient.widget.EmptyRecyclerView;

// класс активности для работы с серверами FTP
public class ListServersActivity extends AppCompatActivity implements ServersView,
                                            BaseAdapter.OnItemClickListener<FtpServer> {

    private ServersAdapter mAdapter;                                                                // адаптер списка серверов
    private ServersPresenter mPresenter;                                                            // презентер списка серверов
    private AlertDialog mDialog;                                                                    // диалог предупреждений/ошибок
    private boolean isClicked;                                                                      // флаг нажатия на кнопки
    @Inject RealmService mRealmService;                                                             // внедрение зависимости - интерфейс операций Realm
    @Inject FtpService mFtpService;                                                                 // внедрение зависимости - интерфейс операций FTP

    // создание активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servers);

        // флаг нажатия кнопки обнуляется
        isClicked = false;

        // тулбар
        Toolbar toolbar = findViewById(R.id.toolbar_servers);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.servers);

        // компоненты активности
        EmptyRecyclerView mRecyclerView = findViewById(R.id.server_name_recycler_view);
        View mEmptyView = findViewById(R.id.noservers_textview);

        // настройка списка
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this));
        mRecyclerView.setEmptyView(mEmptyView);

        // инициализация адаптера списка
        mAdapter = new ServersAdapter(new ArrayList<>(), false);
        mAdapter.attachToRecyclerView(mRecyclerView);
        mAdapter.setOnItemClickListener(this);

        // внедрение зависимостей
        MainApp.getAppComponent().injectListServersActivity(this);

        // инициализация адаптера
        mPresenter = new ServersPresenter(this, mRealmService, mFtpService);

        // инициализация кнопок
        FloatingActionButton mButtonAdd = findViewById(R.id.fab_add);
        FloatingActionButton mButtonDel = findViewById(R.id.fab_del);

        // обработчик нажатия на кнопку добавления сервера
        mButtonAdd.setOnClickListener(view -> {
            Intent intent = new Intent(ListServersActivity.this,
                                                    EditServerActivity.class);
            intent.putExtra("SERVER_NAME", "");
            startActivity(intent);
        });

        // обработчик нажатия на кнопку удаления сервера
        mButtonDel.setOnClickListener(view -> {
            List<FtpServer> list = new ArrayList<>();
            for (FtpServer server : mAdapter.getValues()) {
                if (server.isSelected())
                    list.add(server);
            }
            if (list.isEmpty()) {
                showMessage("Please, select at least one item for deleting", false);
            }
            else if (!isClicked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.message_warning);
                builder.setMessage(R.string.message_sure_delete);
                builder.setIcon(R.drawable.ic_message_warning);
                builder.setCancelable(true);
                builder.setPositiveButton(R.string.message_delete, (dialog, which) -> mPresenter.remove(list));
                builder.setNegativeButton(R.string.message_cancel, (dialog, which) -> {
                                                                            dialog.dismiss();
                                                                            isClicked = false;
                });
                mDialog = builder.create();
                mDialog.show();
                isClicked = true;
            }
        });
    }

    // возобновление работы активности
    @Override
    protected void onResume() {
        super.onResume();
        if (mPresenter != null)
            mPresenter.refresh();
    }

    // постановка активности на паузу
    @Override
    protected void onPause() {
        super.onPause();
        if (mDialog != null)
            mDialog.dismiss();
    }

    // уничтожение активности
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenter != null)
            mPresenter.close();
        findViewById(R.id.fab_add).setOnClickListener(null);
        findViewById(R.id.fab_del).setOnClickListener(null);
    }

    // выбор сервера из списка для редактирования
    @Override
    public void onItemClick(@NonNull FtpServer item) {
        Intent intent = new Intent(ListServersActivity.this,
                                                EditServerActivity.class);
        intent.putExtra("SERVER_NAME", item.getName());
        startActivity(intent);
    }

    // вывод списка серверов из базы
    @Override
    public void showServers(@NonNull List<FtpServer> servers) {
        mAdapter.changeDataSet(servers);
        isClicked = false;
    }

    // отображение пустого списка
    @Override
    public void showEmpty() {
        mAdapter.clear();
        isClicked = false;
    }

    // вывод сообщения об ошибке
    @Override
    public void showMessage(String errorMsg, boolean isError) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(errorMsg);
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

    // завершение работы
    @Override
    public void taskComplete(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        isClicked = false;
    }

    @Override
    public void showFoundedServer(FtpServer server) {}

    @Override
    public void connectResult(String stat) {}

    @Override
    public void disconnectResult(boolean stat) {}

    @Override
    public void showLoading() {}

    @Override
    public void hideLoading() {}
}