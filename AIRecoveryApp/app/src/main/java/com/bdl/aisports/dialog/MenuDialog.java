package com.bdl.aisports.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bdl.aisports.R;
import com.bdl.aisports.adapter.MenuAdapter;

import org.apache.log4j.lf5.util.Resource;
import org.w3c.dom.Text;

import java.util.List;

/**
 * @Author: 闫科宇
 * @Data: 2019/4/10 15:42
 * @Description:
 * 自定义仿IOS - 单选菜单Dialog
 */
public class MenuDialog extends Dialog {

    /**
     * 类成员
     */
    private String title; //标题
    private List<String> menuItems; //菜单子项
    private String negativeBtnText = "取消"; //Negative按钮文本
    private String positiveBtnText = "确定"; //Positive按钮文本
    private View.OnClickListener onNegativeClickListener;
    private View.OnClickListener onPositiveClickListener;
    private AdapterView.OnItemClickListener onItemClickListener;
    private MenuAdapter adapter;
    private int selectedIndex;
    //控件
    private TextView textTitle;
    private ListView menuList;
    private TextView sure;
    private TextView cancel;

    public MenuDialog(@NonNull Context context) {
        super(context, R.style.menuDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.dialog_menu);
        ViewInject();
        ViewVisibility();
        setArrayAdapter();
    }

    /**
     * 控件绑定
     */
    private void ViewInject() {
        textTitle = (TextView) findViewById(R.id.menu_dialog_title);
        menuList = (ListView) findViewById(R.id.lv_menu_list);
        sure = (TextView) findViewById(R.id.menu_dialog_sure);
        cancel = (TextView) findViewById(R.id.menu_dialog_cancel);
    }

    /**
     * 动态设置控件的显示与隐藏
     */
    private void ViewVisibility() {
        //判断是否设置了title
        if (!TextUtils.isEmpty(title)) {
            textTitle.setVisibility(View.VISIBLE);
            textTitle.setText(title);
        } else {
            textTitle.setVisibility(View.GONE);
        }

        //判断是否设置了返回按钮
        if (onNegativeClickListener != null) {
            cancel.setVisibility(View.VISIBLE);
            cancel.setOnClickListener(onNegativeClickListener);
            cancel.setText(negativeBtnText);
        } else {
            cancel.setVisibility(View.GONE);
        }

        //判断是否设置了确认按钮
        if (onPositiveClickListener != null) {
            sure.setVisibility(View.VISIBLE);
            sure.setOnClickListener(onPositiveClickListener);
            sure.setText(positiveBtnText);
        } else {
            sure.setVisibility(View.GONE);
        }
    }

    /**
     * 填充ListView的内容
     */
    private void setArrayAdapter() {
        if(menuItems != null) {
            menuList.setVisibility(View.VISIBLE);
            adapter = new MenuAdapter(getContext(), menuItems, selectedIndex);
            menuList.setAdapter(adapter);
            menuList.setOnItemClickListener(onItemClickListener);
        } else {
            menuList.setVisibility(View.GONE);
        }
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMenuItems(List<String> menuItems) {
        this.menuItems = menuItems;
    }

    public void setNegativeBtnText(String negativeBtnText) {
        this.negativeBtnText = negativeBtnText;
    }

    public void setPositiveBtnText(String positiveBtnText) {
        this.positiveBtnText = positiveBtnText;
    }

    public void setOnNegativeClickListener(View.OnClickListener onNegativeClickListener) {
        this.onNegativeClickListener = onNegativeClickListener;
    }

    public void setOnPositiveClickListener(View.OnClickListener onPositiveClickListener) {
        this.onPositiveClickListener = onPositiveClickListener;
    }
}
