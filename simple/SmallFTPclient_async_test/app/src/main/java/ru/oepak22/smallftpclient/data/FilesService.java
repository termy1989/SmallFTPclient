package ru.oepak22.smallftpclient.data;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import ru.oepak22.smallftpclient.content.LocalFile;

// интефейс для операций с файлами
public interface FilesService {

    String getSDcard();

    boolean isSDcard(String path);

    boolean isRoot(Context context, String path);

    Pair<Long, Long> getSpace(String path);

    String changeOfExisting(String path);

    List<LocalFile> getListFiles(String dir);

    DocumentFile getDocumentFile(Context context, final File file, final boolean isDirectory);

    boolean mkdir(Context context, File dir);

    boolean delete(Context context, File file);

    boolean rename(Context context, File src, File dest);

    OutputStream getOutputStream(Context context, File destFile) throws IOException;

    InputStream getInputStream(Context context, File destFile) throws FileNotFoundException;
}
