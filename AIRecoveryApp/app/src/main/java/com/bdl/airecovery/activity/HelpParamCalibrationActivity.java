package com.bdl.airecovery.activity;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.contoller.Reader;
import com.bdl.airecovery.contoller.Writer;
import com.bdl.airecovery.dialog.CommonDialog;
import com.bdl.airecovery.entity.CalibrationParameter;
import com.bdl.airecovery.entity.Help;
import com.bdl.airecovery.service.MotorService;

import org.w3c.dom.Text;
import org.xutils.DbManager;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.bdl.airecovery.contoller.Writer.setInitialBounce;
import static com.bdl.airecovery.contoller.Writer.setKeepArmTorque;
import static com.bdl.airecovery.contoller.Writer.setParameter;

@ContentView(R.layout.activity_help)
public class HelpParamCalibrationActivity extends BaseActivity {
    @ViewInject(R.id.help_torque)
    private TextView tvTorque;  //力矩值

    @ViewInject(R.id.help_front_limit)
    private TextView tvFrontLimit;  //前方限制Spinner

    @ViewInject(R.id.help_btn_update)
    private Button updateBtn;  //保存按钮

    @ViewInject(R.id.help_btn_reset)
    private Button resetBtn;  //恢复出场设置按钮

    @ViewInject(R.id.help_btn_return)
    private Button returnBtn;  //返回按钮

    @ViewInject(R.id.help_speed)
    private Spinner speedSpinner;

    @ViewInject(R.id.help_param_a)
    private Spinner paramASpinner;

    @ViewInject(R.id.help_param_b)
    private Spinner paramBSpinner;


    private int torque = 0;
    private int frontLimitedPosition = 0;
    private int rearLimitedPosition = 0;

    private DbManager db = MyApplication.getInstance().getDbManager(); //获取DbManager对象
    private int currentTorque = 1; //当前标定的力矩值
    private int currentFrontLimit = 20; //当前标定的前方限制
    private boolean haveDataChanged = false;
    private Help helpParam = null;
    private CalibrationParameter calibrationParameter = null;
    private int deviceType = MyApplication.getInstance().getCurrentDevice().getDeviceType(); //获得设备信息

    Timer timer = new Timer();
    {
        try {
            calibrationParameter = db.findFirst(CalibrationParameter.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tvTorque.setText(currentTorque + "");
        tvFrontLimit.setText(currentFrontLimit + "");
        initParam();
        MotorService                //运动前电机的初始化
                .getInstance()
                .initializationBeforeStart(20 * 10000, deviceType, 100, 100);
        initLimit();                //初始化前后方限制
        setBtnOnclickEvent();
        setSpinnerOnclickEvent();
        LogUtil.e("======"+calibrationParameter.toString());
    }

    /**
     * 初始前后方限制
     */
    private void initLimit() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Writer.setParameter(helpParam.getPosition() * 4856, MotorConstant.SET_FRONTLIMIT);
                    Writer.setParameter(0, MotorConstant.SET_REARLIMIT);
                    setParameter(calibrationParameter.getBackSpeed() * 100, MotorConstant.SET_BACK_SPEED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 打开运动过程
     */
    void onpenMovementProcess() {
        final int[] lastLocation = {frontLimitedPosition}; //上一次的位置，初始值为前方限制
        //如果出现修改，该位置就改变
        final boolean[] haveStopped = {false};
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //读取当前位置
                    int currentSpeed = Math.abs(Integer.valueOf(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                    int currentTorque = Integer.valueOf(Reader.getRespData(MotorConstant.READ_TORQUE));
                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    if (currentSpeed <= 10 && torque < calibrationParameter.getMinBackTorque() * 100) {
                        setParameter(calibrationParameter.getMinBackTorque() * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                        haveStopped[0] = true;
                    }
                    if (difference > 20000) { //回程
                        //超过前方限制
                        if (Integer.valueOf(currentLocation) >= frontLimitedPosition - 50000) {
                            setParameter(-5 * 100, MotorConstant.SET_GOING_SPEED);
                            if (haveStopped[0]) { //是否需要恢复反向力量
                                setParameter(torque, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                                haveStopped[0] = false;
                            }
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    } else if (difference < -20000) {//去程
                        //转速超过500，且与最新的限位比较，如果距离大于20000，则可以继续更改，考虑一些延时的因素
                        Log.e("----", String.valueOf(currentTorque));
                        if (currentTorque < -35) {
                            Log.e("----", "来程速度设置为0");
                            setParameter(0, MotorConstant.SET_GOING_SPEED);
                        }
                        if (currentSpeed >= 450 && currentSpeed <= 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 150 - 2; //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        } else if (currentSpeed > 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 150 + calibrationParameter.getLead(); //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        }
                        //超过后方限制
                        if (Integer.valueOf(currentLocation) < rearLimitedPosition + 100000) {
//                            setParameter(MotorConstant.speed, MotorConstant.SET_BACK_SPEED);
                            setParameter(90 * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED1);
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(timerTask, 0, 50);
    }

    /**
     * 根据当前的力矩值和前方限制查询
     * @param torque
     * @param limit
     * @return
     * @throws DbException
     */
    public Help getHelpParamByTorqueAndLimit(int torque, int limit) throws DbException {
        return db.findById(Help.class, getIdByTorqueAndLimit(torque, limit));
    }


    /**
     * 根据力矩和限制返回id
     * @param torque
     * @param limit
     * @return
     */
    int getIdByTorqueAndLimit(int torque, int limit) {
        return torque + (limit / 20 - 1) * 5;
    }


    /**
     * 初始化各种参数
     * @throws DbException
     */
    void initParam() {
        try {
            helpParam = getHelpParamByTorqueAndLimit(currentTorque, currentFrontLimit);
        } catch (DbException e) {
            e.printStackTrace();
        }
        LogUtil.e("参数值:::" + helpParam.toString());
        List<Integer> speedList = generateRange(1, 5, 1);
        List<Integer> paramAList = generateRange(1, 50, 3);
        List<Integer> paramBList = generateRange(1, 20, 2);
        //创建Spinner
        createSpinner(speedSpinner, speedList,
                getIndexOfList(speedList, helpParam.getHelpSpeed()));
        //TODO 5改为数据库中的值
        createSpinner(paramASpinner, paramAList,
                getIndexOfList(paramAList, helpParam.getParamA()));
        createSpinner(paramBSpinner, paramBList,
                getIndexOfList(paramBList, helpParam.getParamB()));

    }

    /**
     * 创建下拉框
     *
     * @param spinner 下拉框对象
     * @param list  包含数据的list
     */
    private void createSpinner(final Spinner spinner, List<Integer> list, int position) {
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, list);  //创建一个数组适配器
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        spinner.setAdapter(adapter);
        spinner.setSelection(position, true);
    }

    /**
     * 生成固定间距的list
     *
     * @param begin 区间初始值
     * @param end 区间结束值
     * @param difference 差值
     * @return
     */
    private List<Integer> generateRange(int begin, int end, int difference) {
        List<Integer> list = new ArrayList<>();
        int length = (end - begin) + 1;
        for (int i = 0; i < length; i += difference) {
            list.add(begin + i);
        }
        return list;
    }

    /**
     * 获取List某个值的下标值
     *
     * @param list
     * @param value
     * @return 返回下标值
     */
    private int getIndexOfList(List list, int value) {
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(value)) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * 重置所有参数
     */
    private void resetParam() {
        for (int i = 20; i <= 200; i += 20) { //十个位置分段
            for (int j = 1; j <= 5; j++) { //五个力量分段
                helpParam.setId(getIdByTorqueAndLimit(j, i));
                helpParam.setTorque(j);
                helpParam.setPosition(i);
                helpParam.setHelpSpeed(MotorConstant.helpSpeed);
                helpParam.setParamA(MotorConstant.paramA);
                helpParam.setParamB(MotorConstant.paramB);
                LogUtil.e("当前更新::" + helpParam.toString());
                try {
                    db.update(helpParam);
                } catch (DbException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * Spinner点击事件
     */
    private void setSpinnerOnclickEvent() {
        //力矩Spinner点击事件
        speedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO 改变电机中的值
                int speed = Integer.parseInt(speedSpinner.getItemAtPosition(i).toString());
                helpParam.setHelpSpeed(speed);
                haveDataChanged = true;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //Spinner点击事件
        paramASpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO 改变电机中的值
                int ParamA = Integer.parseInt(paramASpinner.getItemAtPosition(i).toString());
                helpParam.setParamA(ParamA);
                haveDataChanged = true;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //Spinner点击事件
        paramBSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO 改变电机中的值
                int paramB = Integer.parseInt(paramBSpinner.getItemAtPosition(i).toString());
                helpParam.setParamB(paramB);
                haveDataChanged = true;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }


    //各种按钮点击事件
    private void setBtnOnclickEvent() {
        //保存按钮
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtil.e("更新::::"+helpParam.toString());
                //修改数据库
                try {
                    db.update(helpParam);
                } catch (DbException e) {
                    e.printStackTrace();
                }
                Toast.makeText(HelpParamCalibrationActivity.this, "保存成功!", Toast.LENGTH_SHORT).show();
                if (currentFrontLimit == 200 && currentTorque == 5) {
                    final CommonDialog commonDialog = new CommonDialog(HelpParamCalibrationActivity.this);
                    commonDialog.setTitle("温馨提示");
                    commonDialog.setMessage("标定流程进行完毕，点击确认退出");
                    commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            commonDialog.dismiss();
                            HelpParamCalibrationActivity.this.finish();
                        }
                    });
                    commonDialog.show();
                } else {
                    if (currentTorque++ >= 5) {
                        currentFrontLimit += 20;
                        currentTorque = 1;
                    }
                }
                initParam();
                torque = currentTorque;
                frontLimitedPosition = currentFrontLimit;
                try {
                    Writer.setParameter(frontLimitedPosition, MotorConstant.SET_FRONTLIMIT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                haveDataChanged = false;
                LogUtil.e("haveDataChanged" + haveDataChanged);
                tvTorque.setText(currentTorque + "");
                tvFrontLimit.setText(currentFrontLimit + "");
            }
        });


        //恢复出厂设置
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CommonDialog commonDialog = new CommonDialog(HelpParamCalibrationActivity.this);
                commonDialog.setTitle("温馨提示");
                commonDialog.setMessage("当前操作会将所有标定参数恢复到出厂默认值,确定继续进行该操作？");
                commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        commonDialog.dismiss();
                    }
                });
                commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        resetParam();
                        Toast.makeText(HelpParamCalibrationActivity.this, "当前标定参数值已经恢复到出厂设置!", Toast.LENGTH_SHORT).show();
                        commonDialog.dismiss();
                        HelpParamCalibrationActivity.this.finish();
                    }
                });
                commonDialog.show();
            }
        });

        //返回按钮
        returnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CommonDialog commonDialog = new CommonDialog(HelpParamCalibrationActivity.this);
                commonDialog.setTitle("温馨提示");
                commonDialog.setMessage("当前数据未保存，确定返回？");
                commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        commonDialog.dismiss();
                    }
                });
                commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        commonDialog.dismiss();
                        HelpParamCalibrationActivity.this.finish();
                    }
                });
                if (haveDataChanged) {
                    commonDialog.show();
                } else {
                    HelpParamCalibrationActivity.this.finish();
                }
            }
        });


    }
}
