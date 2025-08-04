package com.zcb.packinglistedit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView 的适配器，用于显示零件列表数据（保修编号、名称、箱号）
 */
public class PartAdapter extends RecyclerView.Adapter<PartAdapter.ViewHolder> {
    private List<Part> parts; // 数据源：存储要显示的零件列表

    /**
     * 构造函数：初始化适配器的数据源
     * @param parts 要显示的零件列表
     */
    public PartAdapter(List<Part> parts) {
        this.parts = parts;
    }

    /**
     * 创建 ViewHolder 实例（每个列表项的视图容器）
     * @param parent   父布局（RecyclerView）
     * @param viewType 视图类型（用于多类型列表，此处未使用）
     * @return 返回自定义的 ViewHolder
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 1. 加载列表项布局文件（这里使用 Android 内置的简单布局）
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        // 2. 创建并返回 ViewHolder，绑定布局中的控件
        return new ViewHolder(view);
    }

    /**
     * 绑定数据到 ViewHolder（将数据填充到列表项的视图上）
     * @param holder   ViewHolder 实例
     * @param position 当前数据项的位置
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // 1. 获取当前位置的零件数据
        Part part = parts.get(position);
        // 2. 将数据设置到 ViewHolder 的控件中
        holder.idTextView.setText("保修单号: " + part.getBxid());
        holder.nameTextView.setText("箱号: " + (part.getBoxNumber().isEmpty() ? "未装箱" : part.getBoxNumber()) + ",名称: " + part.getName() + ",数量：" + part.getNum());
    }

    /**
     * 返回数据源的总项数（决定 RecyclerView 显示多少项）
     * @return 列表数据的总数
     */
    @Override
    public int getItemCount() {
        return parts != null ? parts.size() : 0;
    }

    /**
     * 更新数据源并刷新列表（外部调用此方法更新 UI）
     * @param newParts 新的零件列表数据
     */
    public void updateData(List<Part> newParts) {
        parts = newParts;
        notifyDataSetChanged(); // 通知 RecyclerView 数据已变更，强制刷新所有列表项
    }

    /**
     * ViewHolder 内部类：缓存列表项的视图控件，避免重复 findViewById
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView idTextView;    // 显示零件序号
        public TextView nameTextView;  // 显示零件名称和箱号

        /**
         * ViewHolder 构造函数
         * @param view 列表项的根视图
         */
        public ViewHolder(View view) {
            super(view);
            // 绑定布局中的控件（使用 Android 内置控件的 ID）
            idTextView = view.findViewById(android.R.id.text1);   // 列表项的第一行文本
            nameTextView = view.findViewById(android.R.id.text2); // 列表项的第二行文本
        }
    }
}