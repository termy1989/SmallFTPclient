package ru.oepak22.smallftpclient.widget;

import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.oepak22.smallftpclient.content.LocalFile;

public abstract class BaseAdapter<VH extends RecyclerView.ViewHolder, T> extends RecyclerView.Adapter<VH> {

    private final List<T> mItems = new ArrayList<>();
    private List<T> mItemsOld;

    @Nullable
    private OnItemClickListener<T> mOnItemClickListener;

    @Nullable
    private OnItemLongClickListener<T> mOnItemLongClickListener;

    private final View.OnClickListener mInternalClickListener = (view) -> {
        if (mOnItemClickListener != null) {
            int position = (int) view.getTag();
            T item = mItems.get(position);
            mOnItemClickListener.onItemClick(item);
        }
    };

    private final View.OnLongClickListener mInternalLongClickListener = (view) -> {
        if (mOnItemLongClickListener != null) {
            int position = (int) view.getTag();
            T item = mItems.get(position);
            mOnItemLongClickListener.onItemLongClick(item);
        }
        return true;
    };

    @Nullable
    private EmptyRecyclerView mRecyclerView;

    public BaseAdapter(@NonNull List<T> items) {
        mItemsOld = items;
        mItems.addAll(items);
    }

    public void attachToRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.setAdapter(this);
        refreshRecycler();
    }

    public void changeDataSet(@NonNull List<T> values) {
        mItems.clear();
        mItems.addAll(values);
        mItemsOld = values;
        refreshRecycler();
    }

    public List<T> getValues() { return mItems; }

    public List<T> getValuesOld() { return mItemsOld; }

    public final void clear() {
        mItems.clear();
        refreshRecycler();
    }

    public final void add(T item) {
        mItems.add(item);
        refreshRecycler();
    }

    public void recover() {
        mItems.clear();
        mItems.addAll(mItemsOld);
        refreshRecycler();
    }

    public void refreshRecycler() {
        notifyDataSetChanged();
        if (mRecyclerView != null) {
            mRecyclerView.checkIfEmpty();
        }
    }

    @CallSuper
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(mInternalClickListener);
        holder.itemView.setOnLongClickListener(mInternalLongClickListener);
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener<T> onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(@Nullable OnItemLongClickListener<T> onItemLongClickListener) {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    public void destroy() {
        mRecyclerView = null;
        mOnItemClickListener = null;
        mOnItemLongClickListener = null;
        mItems.clear();
    }

    @NonNull
    public T getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    // короткое нажатие
    public interface OnItemClickListener<T> {
        void onItemClick(@NonNull T item);
    }

    // длинное нажатие
    public interface OnItemLongClickListener<T> {
        void onItemLongClick(@NonNull T item);
    }
}
