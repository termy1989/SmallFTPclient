package ru.oepak22.smallftpclient.screen.local;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.Define;
import ru.oepak22.smallftpclient.content.LocalFile;
import ru.oepak22.smallftpclient.data.FilesProvider;
import ru.oepak22.smallftpclient.screen.main.MainActivity;
import ru.oepak22.smallftpclient.widget.BaseAdapter;
import ru.oepak22.smallftpclient.widget.DividerItemDecoration;
import ru.oepak22.smallftpclient.widget.EmptyRecyclerView;

// класс фрагмента для работы с локальным хранилищем
public class LocalStorageFragment extends Fragment implements LocalStorageView,
                                        BaseAdapter.OnItemClickListener<LocalFile> {

    private Realm mRealm;                                                                               // база данных
    private LocalStorageAdapter mAdapter;                                                               // адаптер списка
    private LocalStoragePresenter mPresenter;                                                           // презентер локального хранилища
    private FileObserver fileObserver;                                                                  // наблюдатель за изменениями в текущем каталоге
    private TextView pathTextView;                                                                      // текст с путем к текущей рабочей директории
    private FloatingActionButton fab;                                                                   // кнопка загрузки
    private SwipeRefreshLayout mSwipeRefreshLayout;                                                     // обновлялка

    // создание фрагмента
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // добавление меню
        setHasOptionsMenu(true);

        // инициализация базы данных
        mRealm = Realm.getDefaultInstance();

        // определение/восстановление презентера
        if (savedInstanceState != null) {
            mPresenter = new LocalStoragePresenter(this,
                                                    requireActivity().getApplicationContext(),
                                                    savedInstanceState.getString("SAVED_DIR"));
            RealmResults<LocalFile> cache = mRealm.where(LocalFile.class).findAll();
            mPresenter.saveCache(mRealm.copyFromRealm(cache));
        }
        else {
            Bundle bundle = getArguments();
            if (bundle != null)
                mPresenter = new LocalStoragePresenter(this,
                                                        requireActivity().getApplicationContext(),
                                                        bundle.getString("START_DIR"));
        }

        // переопределение кнопки "Назад"
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!FilesProvider.provideFilesService().isRoot(requireActivity()
                                                                    .getApplicationContext(),
                                                                        mPresenter.getCurrentDir()))
                    mPresenter.toParentDir();
                else ((MainActivity) requireActivity()).closeApplication();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    // отрисовка фрагмента
    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                                        Bundle savedInstanceState) {

        // инициализация разметки
        View view = inflater.inflate(R.layout.fragment_local_storage, null);

        // инициализация кнопки загрузки
        fab = view.findViewById(R.id.fab_upload);
        fab.setOnClickListener(view2 -> {
            List<LocalFile> list = getSelectedItems();
            if (list.isEmpty())
                ((MainActivity) requireActivity())
                        .showMessage("Please, select at least one item for uploading",
                                        false);
            else ((MainActivity) requireActivity()).checkUploadRunning();
        });

        // инициализация обновлялки
        mSwipeRefreshLayout = view.findViewById(R.id.local_swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mPresenter.refresh(mPresenter.getCurrentDir());
            new Handler().postDelayed(() -> mSwipeRefreshLayout.setRefreshing(false), 500);
        });
        mSwipeRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN);

        // компоненты фрагмента
        EmptyRecyclerView mRecyclerView = view.findViewById(R.id.file_name_recycler_view);
        View mEmptyView = view.findViewById(R.id.nofiles_textview);
        pathTextView = view.findViewById(R.id.local_path_text_view);

        // настройка списка
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(requireActivity()));
        mRecyclerView.setEmptyView(mEmptyView);

        // инициализация адаптера списка
        mAdapter = new LocalStorageAdapter(new ArrayList<>(), false);
        mAdapter.attachToRecyclerView(mRecyclerView);
        mAdapter.setOnItemClickListener(this);

        return view;
    }

    // возобновление работы фрагмента
    @Override
    public void onResume() {
        super.onResume();
        mPresenter.refresh(mPresenter.getCurrentDir());
    }

    // уничтожение фрагмента
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) mPresenter.close();
        if (mAdapter != null) mAdapter.destroy();
        if (fab != null) fab.setOnClickListener(null);
        if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setOnRefreshListener(null);
        if (fileObserver != null) {
            fileObserver.stopWatching();
            fileObserver = null;
        }
    }

    // инициализация главного меню
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.local_fragment_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.action_local_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Type here to search");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                mAdapter.filter(s);
                return false;
            }
        });
    }

    // обработчик главного меню
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        List<LocalFile> selectedItems;

        // кнопки главного меню
        switch (item.getItemId()) {

            // кнопка "Select all"
            case R.id.action_local_select_all:
                mAdapter.changeDataSet(mPresenter.selectItems(mAdapter.getValues(), true));
                break;

            // кнопка "Clear all"
            case R.id.action_local_clear_all:
                mAdapter.changeDataSet(mPresenter.selectItems(mAdapter.getValues(), false));
                break;

            // кнопка "New folder"
            case R.id.action_local_new_folder:
                ((MainActivity) requireActivity()).openEditDialog("", Define.LOCAL_DIR_CREATE);
                break;

            // кнопка "Rename"
            case R.id.action_local_rename:
                selectedItems = getSelectedItems();
                if (selectedItems.size() != 1)
                    ((MainActivity) requireActivity())
                            .showMessage("Please, select one item for rename", false);
                else
                    ((MainActivity) requireActivity()).openEditDialog(selectedItems.get(0).getName(),
                                                                            Define.LOCAL_FILE_RENAME);
                break;

            // кнопка "Delete"
            case R.id.action_local_delete:
                selectedItems = getSelectedItems();
                if (selectedItems == null || selectedItems.isEmpty())
                    ((MainActivity) requireActivity())
                            .showMessage("Please, select at least one item for deleting",
                                                                                    false);
                else
                    ((MainActivity) requireActivity()).deleteFiles(null, getCurrentDir(),
                                                                                null,
                                                                                selectedItems,
                                                                                    null);
                break;

            // кнопка "Move"
            case R.id.action_local_move:
                selectedItems = getSelectedItems();
                if (selectedItems.isEmpty())
                    ((MainActivity) requireActivity())
                            .showMessage("Please, select at least one item for moving",
                                                                                    false);
                else ((MainActivity) requireActivity())
                                        .checkCopyMoveRunning(null, null,
                                                                    Define.MOVE_TASK);
                break;

            // кнопка "Copy"
            case R.id.action_local_copy:
                selectedItems = getSelectedItems();
                if (selectedItems.isEmpty())
                    ((MainActivity) requireActivity())
                            .showMessage("Please, select at least one item for copying",
                                                                                    false);
                else ((MainActivity) requireActivity())
                                        .checkCopyMoveRunning(null, null,
                                                                    Define.COPY_TASK);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // сохранение данных перед поворотом экрана
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("SAVED_DIR", mPresenter.getCurrentDir());
        mAdapter.recover();
        mRealm.beginTransaction();
        mRealm.delete(LocalFile.class);
        mRealm.insert(mAdapter.getValues());
        mRealm.commitTransaction();
    }

    // вывод содержимого каталога на экран и установка наблюдения за каталогом
    @Override
    public void showFiles(@NonNull List<LocalFile> files) {
        pathTextView.setText(mPresenter.getCurrentDir());
        mAdapter.changeDataSet(files);
        setCurrentDirObserver();
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

    // установка наблюдения за изменениями в текущем каталоге
    void setCurrentDirObserver() {

        // остановка наблюдателя, если он запущен
        if (fileObserver != null) {
            fileObserver.stopWatching();
            fileObserver = null;
        }

        // определение пути к текущему каталогу
        String path = mPresenter.getCurrentDir();

        // инициализация наблюдателя
        fileObserver = new FileObserver(path) {

            // обработчик изменений в каталоге
            @Override
            public void onEvent(int event, @Nullable String name) {

                if ((FileObserver.CREATE & event) != 0 ||
                        (FileObserver.MOVED_TO & event) != 0 ||
                        (FileObserver.DELETE_SELF & event) != 0 ||
                        (FileObserver.MOVE_SELF & event) != 0 ||
                        //(FileObserver.MODIFY & event) != 0 ||
                        //(FileObserver.ATTRIB & event) != 0 ||
                        (FileObserver.CLOSE_WRITE & event) != 0 ||
                        (FileObserver.DELETE & event) != 0 ||
                        (FileObserver.MOVED_FROM & event) != 0) {
                    requireActivity().runOnUiThread(() -> mPresenter.refresh(path));
                }
            }
        };

        // запуск наблюдателя
        fileObserver.startWatching();
    }

    // получение пути к текущей директории
    public String getCurrentDir() {
        return mPresenter.getCurrentDir();
    }

    // получение выделенных элементов списка
    public List<LocalFile> getSelectedItems() {
        return mPresenter.getSelectedItems(mAdapter.getValues());
    }

    // создание нового каталога
    public boolean createNewFolder(String name) {
        return mPresenter.createNewFolder(name);
    }

    // переименование элемента
    public boolean renameFile(String name) {
        return mPresenter.renameFile(name, getSelectedItems().get(0));
    }
}