package com.bdl.aisports.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bdl.aisports.R;

import java.util.List;

/**
 * @Author: 闫科宇
 * @Data: 2019/4/11 13:30
 * @Description:
 */
public class MenuAdapter extends BaseAdapter {
    private List<String> mList; //数据源1
    private int selectedIndex;  //数据源2
    private LayoutInflater mInflater;   //布局装载器对象

    public MenuAdapter(Context context, List<String> mList, int selectedIndex) {
        this.mList = mList;
        this.selectedIndex = selectedIndex;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    //ListView需要显示的数据数量
    public int getCount() {
        return this.mList.size();
    }

    @Override
    //指定的索引对应的数据项
    public Object getItem(int position) {
        return this.mList.get(position);
    }

    @Override
    //制定的索引对应的数据项ID
    public long getItemId(int position) {
        return position;
    }

    @Override
    //返回每一项的显示内容
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        //如果view未被实例化过，缓存池中没有对应的缓存
        if (convertView == null) {
            viewHolder = new ViewHolder();
            //由于我们只需要将XML转化为view，并不涉及具体布局，所以第二个参数通常设置为null
            convertView = mInflater.inflate(R.layout.item_menu,null);

            //对viewHolder的属性赋值
            viewHolder.content = (TextView) convertView.findViewById(R.id.tv_menu_item);

            //通过setTag将converView与viewHolder关联
            convertView.setTag(viewHolder);
        } else { //如果缓存池中有对应的view缓存，则直接通过getTag取出viewHolder
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //取出String对象
        String strBean = mList.get(position);

        //设置控件的数据
        viewHolder.content.setText(strBean);

        //如果当前子项是被选项，高亮处理[蓝色字体]
        if(position == selectedIndex) {
            viewHolder.content.setTextColor(Color.parseColor("#1B88EE"));
        } else { //否则，普通处理[灰色字体]
            viewHolder.content.setTextColor(Color.parseColor("#696969"));
        }

        return convertView;
    }

    //ViewHolder用于缓存控件，属性分别对应item布局文件的控件
    class ViewHolder {
        public TextView content;
    }
}
