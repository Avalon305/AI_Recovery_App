package com.bdl.airecovery.activity;


import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.entity.CalibrationParameter;
import com.bdl.airecovery.widget.CircularRingPercentageView;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

@ContentView(R.layout.activity_calibration)
public class CalibrationActivity extends BaseActivity {
    @ViewInject(R.id.calibration_toque)     //力矩值
    private Spinner torque;

    @ViewInject(R.id.calibration_front_limit)     //前方限制
    private Spinner frontLimit;

    @ViewInject(R.id.calibration_rear_limit)     //后方限制
    private Spinner rearLimit;

    @ViewInject(R.id.calibration_min_torque)     //最小力矩
    private Spinner minTorque;

    @ViewInject(R.id.calibration_back_speed)     //返回速度
    private Spinner backSpeed;

    @ViewInject(R.id.calibration_normal_speed)     //非运动状态下的速度
    private Spinner normalSpeed;

    @ViewInject(R.id.calibration_min_back_torque)     //最小返回力矩
    private Spinner minBackTorque;

    @ViewInject(R.id.calibration_bounce)     //初始反弹力量
    private Spinner bounce;

    @ViewInject(R.id.calibration_lead)     //提前量
    private Spinner lead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam(); //初始化数据

//        String[] ctype = new String[]{"全部", "游戏", "电影", "娱乐", "图书"};
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ctype);  //创建一个数组适配器
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
//
//        Spinner spinner = super.findViewById(R.id.calibration_min_torque);
//        spinner.setAdapter(adapter);
    }



    private void initParam() {
        //初始化各个参数的范围
        List<Integer> torqueList = generateRange(5, 100, 1);
        List<Integer> frontLimitList= generateRange(0, 200, 5);
        List<Integer> rearLimitList = generateRange(0, 200, 5);
        List<Integer> minTorqueList = generateRange(0, 20, 1);
        List<Integer> backSpeedList = generateRange(10, 60, 1);
        List<Integer> normalSpeedList = generateRange(2, 10, 1);
        List<Integer> minBackTorqueList = generateRange(5, 50, 1);
        List<Integer> bounceList = generateRange(0, 20, 1);
        List<Integer> leadList = generateRange(0, 10, 1);

        //力矩Spinner
        final Spinner torqueSpinner = findViewById(R.id.calibration_toque);
        createSpinner(torqueSpinner, torqueList);
        torqueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {//通过此方法为下拉列表设置点击事件
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String text= torqueSpinner.getItemAtPosition(i).toString();
                Toast.makeText(CalibrationActivity.this,text,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });



        //默认值
        CalibrationParameter calibrationParameter = new CalibrationParameter();
        calibrationParameter.setMinTorque(0);
        calibrationParameter.setBackSpeed(35);
        calibrationParameter.setNormalSpeed(5);
        calibrationParameter.setMinBackTorque(20);
        calibrationParameter.setBounce(10);
        calibrationParameter.setLead(3);
    }

    /**
     * 创建下拉框
     * @param spinner
     * @param list
     */
    private void createSpinner(final Spinner spinner, List<Integer> list) {
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, list);  //创建一个数组适配器
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        spinner.setAdapter(adapter);
    }

    /**
     * 生成list
     * @param begin
     * @param end
     * @param difference
     * @return
     */
    private List<Integer> generateRange(int begin, int end, int difference) {
        List<Integer> list = new ArrayList<>();
        int length = (end - begin) + 1;
        for (int i = 0; i < length; i += difference) {
            list.add(begin + i);
        }
//        return list.toArray(new Integer[list.size()]);
        return list;
    }


}
