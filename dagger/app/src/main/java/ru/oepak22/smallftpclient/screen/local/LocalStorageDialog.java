package ru.oepak22.smallftpclient.screen.local;

import android.app.Dialog;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import ru.oepak22.smallftpclient.MainApp;
import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.LocalFile;
import ru.oepak22.smallftpclient.data.FilesService;
import ru.oepak22.smallftpclient.screen.main.MainActivity;
import ru.oepak22.smallftpclient.widget.BaseAdapter;
import ru.oepak22.smallftpclient.widget.DividerItemDecoration;
import ru.oepak22.smallftpclient.widget.EmptyRecyclerView;

// класс диалогового окна выбора папки для скачивания
public class LocalStorageDialog extends DialogFragment implements LocalStorageView,
                                            BaseAdapter.OnItemClickListener<LocalFile> {

    private LocalStorageAdapter mAdapter;                                                           // адаптер списка
    private LocalStoragePresenter mPresenter;                                                       // презентер локального хранилища
    private TextView pathTextView;                                                                  // текст с путем к текущей рабочей директории
    private int operationID;                                                                        // идентификатор операции - скачивание/копирование/перемещение
    @Inject FilesService mService;                                                                  // внедрение зависимости - интерфейс операций с файлами

    // создание фрагмента
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // определение пути к текущей директории
        Bundle bundle = getArguments();
        if (bundle != null) {

            // внедрение зависимостей
            MainApp.getAppComponent().injectLocalStorageDialog(this);

            // инициализация презентера
            mPresenter = new LocalStoragePresenter(this, mService,
                                                    requireActivity().getApplicationContext(),
                                                                bundle.getString("START_DIR"));
            operationID = bundle.getInt("OP_ID");
        }
    }

    // формирование диалогового окна с кнопкой
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // вычисление свободного места в выбранном каталоге
        long availableSpace = mService.getSpace(mPresenter.getCurrentDir()).first;

        // добавление кнопки
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.message_folder)
                //.setIcon(R.drawable.ic_textview_path)
                .setCancelable(true)
                .setPositiveButton(R.string.message_ok,
                        (dialog, whichButton) -> ((MainActivity) requireActivity())
                                    .downloadCopyMoveFiles(mPresenter.getCurrentDir(),
                                                                        availableSpace,
                                                                            operationID))
                .setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());


        // инициализация разметки
        View view = requireActivity().getLayoutInflater()
                                        .inflate(R.layout.fragment_local_storage, null);

        // компоненты фрагмента
        view.findViewById(R.id.fab_upload).setVisibility(View.GONE);
        view.findViewById(R.id.local_swipe_container).setEnabled(false);
        EmptyRecyclerView mRecyclerView = view.findViewById(R.id.file_name_recycler_view);
        View mEmptyView = view.findViewById(R.id.nofiles_textview);
        pathTextView = view.findViewById(R.id.local_path_text_view);

        // настройка списка
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(requireActivity()));
        mRecyclerView.setEmptyView(mEmptyView);

        // инициализация адаптера списка
        mAdapter = new LocalStorageAdapter(new ArrayList<>(), true);
        mAdapter.attachToRecyclerView(mRecyclerView);
        mAdapter.setOnItemClickListener(this);

        // добавление разметки в диалог и вывод диалога на экран
        onViewCreated(view, null);
        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    // возобновление фрагмента
    public void onResume() {
        //setDialogSize();
        super.onResume();
        mPresenter.refresh(mPresenter.getCurrentDir());
    }

    // уничтожение фрагмента
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPresenter != null)
            mPresenter.close();
        if (mAdapter != null)
            mAdapter.destroy();
    }

    // вывод содержимого каталога на экран
    @Override
    public void showFiles(@NonNull List<LocalFile> files) {
        pathTextView.setText(mPresenter.getCurrentDir());
        List<LocalFile> dirs = new ArrayList<>();
        for (LocalFile file : files) {
            if (file.isDirectory() || file.isNavigator())
                dirs.add(file);
        }
        mAdapter.changeDataSet(dirs);
    }

    // каталог пустой, переход выше невозможен
    @Override
    public void showEmpty() {
        pathTextView.setText(mPresenter.getCurrentDir());
        mAdapter.clear();
    }

    // обработчик нажатия на элемент списка
    @Override
    public void onItemClick(@NonNull LocalFile item) { mPresenter.onItemClick(item); }

    // настройка размеров диалогового окна
    private void setDialogSize() {

        // сохранение переменных доступа для окна и пустой точки
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        Point size = new Point();

        // сохранение размеров экрана в size
        Display display = window.getWindowManager().getDefaultDisplay();
        display.getSize(size);

        // установка ширины диалога, составляющей 75% ширины экрана;
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, (int) (size.y * 0.75));
        window.setGravity(Gravity.CENTER);
    }
}
