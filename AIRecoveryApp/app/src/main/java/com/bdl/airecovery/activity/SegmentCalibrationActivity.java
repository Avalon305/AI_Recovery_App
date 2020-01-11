package com.bdl.airecovery.activity;

import android.os.Bundle;
import android.os.Message;
import android.support.annotation.RequiresPermission;
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
import com.bdl.airecovery.contoller.MotorProcess;
import com.bdl.airecovery.contoller.Reader;
import com.bdl.airecovery.contoller.Writer;
import com.bdl.airecovery.dialog.CommonDialog;
import com.bdl.airecovery.entity.CalibrationParameter;
import com.bdl.airecovery.entity.Help;
import com.bdl.airecovery.entity.SegmentCalibration;
import com.bdl.airecovery.service.MotorService;
import com.bdl.airecovery.util.CalibrationUtil;

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
import java.util.concurrent.TimeUnit;

import static com.bdl.airecovery.contoller.Writer.setParameter;

@ContentView(R.layout.activity_segment_calibration)
public class SegmentCalibrationActivity extends BaseActivity {
    @ViewInject(R.id.seg_calibration_torque)
    private Spinner torqueSpinner;  //当前标定的力矩

    @ViewInject(R.id.seg_calibration_position)
    private TextView tvPosition;  //当前标定的位置

    @ViewInject(R.id.seg_calibration_btn_update)
    private Button updateBtn;  //保存按钮

    @ViewInject(R.id.seg_calibration_btn_reset)
    private Button resetBtn;  //恢复出场设置按钮

    @ViewInject(R.id.seg_calibration_btn_return)
    private Button returnBtn;  //返回按钮

    @ViewInject(R.id.seg_calibration_going_torque)
    private Spinner goingTorqueSpinner; //去程力矩

    @ViewInject(R.id.seg_calibration_return_torque)
    private Spinner returnTorqueSpinner; //回程力矩

    @ViewInject(R.id.seg_calibration_going_speed)
    private Spinner goingSpeedSpinner; //去程速度

    @ViewInject(R.id.seg_calibration_return_speed)
    private Spinner returnSpeedSpinner; //回程速度

    @ViewInject(R.id.seg_calibration_bounce)
    private Spinner bounceSpinner; //反弹力量

    @ViewInject(R.id.seg_calibration_pull_threshold)
    private Spinner pullThresholdSpinner; //拉动力臂的阈值


    //全局变量
    private int torque = 5;
    private int position = CalibrationUtil.SEGMENT_START;

    //Spinner填充数据
    private List<Integer> torqueList = generateRange(5, 99, 1);
    private List<Integer> goingTorqueList = generateRange(0, 99, 1);
    private List<Integer> returnTorqueList = generateRange(0, 99, 1);
    private List<Integer> goingSpeedList = generateRange(-50, 50, 1);
    private List<Integer> returnSpeedList= generateRange(10, 50, 1);
    private List<Integer> bounceList = generateRange(10, 50, 1);
    private List<Integer> pullThresholdList =  generateRange(-20, 50, 1);

    private DbManager db = MyApplication.getInstance().getDbManager(); //获取DbManager对象
    private CalibrationUtil calibrationUtil = new CalibrationUtil(db);
    private List<SegmentCalibration> calibrations; //接收某个力矩值下的所有分段的List
    private SegmentCalibration calibration;
    private SegmentCalibration calibrationInProcess;

    private boolean haveDataChanged = false;
    private int deviceType = MyApplication.getInstance().getCurrentDevice().getDeviceType(); //获得设备信息
    private Timer timer = new Timer();

    private CalibrationParameter calibrationParameter = null;
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
        initViews(); //初始化Views
        MotorService                //运动前电机的初始化
                .getInstance()
                .initializationBeforeStart(CalibrationUtil.SEGMENT_START, deviceType, torque * 100, torque * 100);
        initMovementParam();                //初始化前后方限制
        openMovementProcess();      //打开运动过程
        setBtnOnclickEvent(); //注册各种按钮点击事件
        setSpinnerOnclickEvent(); //注册各种Spinner点击事件
    }

    /**
     * 初始前后方限制
     */
    private void initMovementParam() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Writer.setParameter((CalibrationUtil.SEGMENT_START - 10) * 4856, MotorConstant.SET_FRONTLIMIT);
                    Writer.setParameter(20 * 4856, MotorConstant.SET_REARLIMIT);
                    calibrationInProcess = calibrationUtil.getCalibrationsByCurrentPosition(calibrations, CalibrationUtil.SEGMENT_START);
                    setParameter(calibrationInProcess.getGoingTorque() * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                    setParameter(calibrationInProcess.getReturnTorque()* 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                    setParameter(calibrationInProcess.getGoingSpeed()* 100, MotorConstant.SET_GOING_SPEED);
                    setParameter(calibrationInProcess.getReturnSpeed()* 100, MotorConstant.SET_BACK_SPEED);
                    setParameter(calibrationInProcess.getBounce()* 100, MotorConstant.SET_INITIAL_BOUNCE);
                    setParameter(calibrationInProcess.getPullThresholdVal() * 100, MotorConstant.SET_PULL_THRESHOLD);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 打开运动过程
     */
    void openMovementProcess() {
        final int[] lastLocation = {position}; //上一次的位置，初始值为前方限制
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
                    calibrationInProcess = calibrationUtil.getCalibrationsByCurrentPosition(calibrations, Integer.parseInt(currentLocation) / 10000);
                    setParameter(calibrationInProcess.getGoingTorque() * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                    setParameter(calibrationInProcess.getReturnTorque()* 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                    setParameter(calibrationInProcess.getGoingSpeed()* 100, MotorConstant.SET_GOING_SPEED);
                    setParameter(calibrationInProcess.getReturnSpeed()* 100, MotorConstant.SET_BACK_SPEED);
                    setParameter(calibrationInProcess.getBounce()* 100, MotorConstant.SET_INITIAL_BOUNCE);
                    setParameter(calibrationInProcess.getPullThresholdVal() * 100, MotorConstant.SET_PULL_THRESHOLD);
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    if (currentSpeed <= 10) {
                        haveStopped[0] = true;
                    }
                    if (difference > 20000) { //回程
                        //超过前方限制
                        if (Integer.valueOf(currentLocation) >= position - 50000) {
                            setParameter(calibrationInProcess.getGoingSpeed() * 100, MotorConstant.SET_GOING_SPEED);
                            if (haveStopped[0]) { //是否需要恢复反向力量
                                setParameter(calibrationInProcess.getReturnTorque() * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                                haveStopped[0] = false;
                            }
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    } else if (difference < -20000) {//去程
                        //转速超过500，且与最新的限位比较，如果距离大于20000，则可以继续更改，考虑一些延时的因素
                        Log.e("----", String.valueOf(currentTorque));
//                        if (currentTorque < -35) {
//                            Log.e("----", "来程速度设置为0");
//                            setParameter(0, MotorConstant.SET_GOING_SPEED);
//                        }
                        if (currentSpeed >= 450 && currentSpeed <= 1000 && Integer.valueOf(currentLocation) >  250000) {
                            int leads = currentSpeed / 150 - 2; //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        } else if (currentSpeed > 1000 && Integer.valueOf(currentLocation) > 250000) {
                            int leads = currentSpeed / 150 + calibrationParameter.getLead(); //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        }
                        //超过后方限制
                        if (Integer.valueOf(currentLocation) < 300000) {
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
     * 初始化Spinners
     * @throws DbException
     */
    void initViews() {
        try {
            calibrations = calibrationUtil.getCalibrationsByForce(torque);
            calibration = calibrationUtil.getCalibrationsByCurrentPosition(calibrations, position);
        } catch (DbException e) {
            e.printStackTrace();
        }

        tvPosition.setText(position + "");
        //创建Spinner
        createSpinner(torqueSpinner, torqueList,
                getIndexOfList(torqueList, torque));

        createSpinner(goingTorqueSpinner, goingTorqueList,
                getIndexOfList(goingTorqueList, calibration.getGoingTorque()));

        createSpinner(returnTorqueSpinner, returnTorqueList,
                getIndexOfList(returnTorqueList, calibration.getReturnTorque()));

        createSpinner(goingSpeedSpinner, goingSpeedList,
                getIndexOfList(goingSpeedList, calibration.getGoingSpeed()));

        createSpinner(returnSpeedSpinner, returnSpeedList,
                getIndexOfList(returnSpeedList, calibration.getReturnSpeed()));

        createSpinner(bounceSpinner, bounceList,
                getIndexOfList(bounceList, calibration.getBounce()));

        createSpinner(pullThresholdSpinner, pullThresholdList,
                getIndexOfList(pullThresholdList, calibration.getPullThresholdVal()));
    }



    void updateViews(boolean isChangeTorque) {
        if (isChangeTorque) { //是否改变的是力矩Spinner
            try {
                calibrations = calibrationUtil.getCalibrationsByForce(torque);
            } catch (DbException e) {
                e.printStackTrace();
            }
            torqueSpinner.setSelection(getIndexOfList(torqueList, torque), true);
            position = CalibrationUtil.SEGMENT_START;
        }
        try {
            calibration = calibrationUtil.getCalibrationsByCurrentPosition(calibrations, position);
        } catch (DbException e) {
            e.printStackTrace();
        }
        tvPosition.setText(position + "");
        goingTorqueSpinner.setSelection(getIndexOfList(goingTorqueList, calibration.getGoingTorque()), true);
        returnTorqueSpinner.setSelection(getIndexOfList(returnTorqueList, calibration.getReturnTorque()), true);
        goingSpeedSpinner.setSelection(getIndexOfList(goingSpeedList, calibration.getGoingSpeed()), true);
        returnSpeedSpinner.setSelection(getIndexOfList(returnSpeedList, calibration.getReturnSpeed()), true);
        bounceSpinner.setSelection(getIndexOfList(bounceList, calibration.getBounce()), true);
        pullThresholdSpinner.setSelection(getIndexOfList(pullThresholdList, calibration.getPullThresholdVal()), true);
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
     * Spinner点击事件
     */
    private void setSpinnerOnclickEvent() {
        //力矩Spinner点击事件
        torqueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                torque = Integer.parseInt(torqueSpinner.getItemAtPosition(i).toString());
                MotorService
                        .getInstance()
                        .initializationBeforeStart(CalibrationUtil.SEGMENT_START, deviceType, torque * 100, torque * 100);
                initMovementParam();
                Toast.makeText(SegmentCalibrationActivity.this, "现在进行" + torque + "KG的标定", Toast.LENGTH_SHORT).show();
                LogUtil.e("触发点击");
                updateViews(true);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        goingTorqueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int goingTorque = Integer.parseInt(goingTorqueSpinner.getItemAtPosition(i).toString());
                calibration.setGoingTorque(goingTorque);
                try {
                    db.update(calibration);
                } catch (DbException e) {
                    e.printStackTrace();
                }
                try {
                    setParameter(goingTorque * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                haveDataChanged = true;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //Spinner点击事件
        returnTorqueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int returnTorque = Integer.parseInt(returnTorqueSpinner.getItemAtPosition(i).toString());
                calibration.setReturnTorque(returnTorque);
                try {
                    db.update(calibration);
                } catch (DbException e) {
                    e.printStackTrace();
                }
                try {
                    setParameter(returnTorque * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                haveDataChanged = true;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        goingSpeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int goingSpeed = Integer.parseInt(goingSpeedSpinner.getItemAtPosition(i).toString());
                calibration.setGoingSpeed(goingSpeed);
                try {
                    db.update(calibration);
                } catch (DbException e) {
                    e.printStackTrace();
                }
                try {
                    setParameter(goingSpeed * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                haveDataChanged = true;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        returnSpeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int returnSpeed = Integer.parseInt(returnSpeedSpinner.getItemAtPosition(i).toString());
                calibration.setReturnSpeed(returnSpeed);
                try {
                    db.update(calibration);
                } catch (DbException e) {
                    e.printStackTrace();
                }
                try {
                    setParameter(returnSpeed * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                haveDataChanged = true;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        bounceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int bounce = Integer.parseInt(bounceSpinner.getItemAtPosition(i).toString());
                calibration.setBounce(bounce);
                try {
                    db.update(calibration);
                } catch (DbException e) {
                    e.printStackTrace();
                }
                try {
                    setParameter(bounce * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                haveDataChanged = true;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        pullThresholdSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int pullThresholdVal = Integer.parseInt(pullThresholdSpinner.getItemAtPosition(i).toString());
                calibration.setPullThresholdVal(pullThresholdVal);
                try {
                    db.update(calibration);
                } catch (DbException e) {
                    e.printStackTrace();
                }
                try {
                    setParameter(pullThresholdVal * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                if (position == CalibrationUtil.SEGMENT_END) {
                    final CommonDialog commonDialog = new CommonDialog(SegmentCalibrationActivity.this);
                    commonDialog.setTitle("温馨提示");
                    commonDialog.setMessage(torque + "KG" + "已标定完毕，请进行其他公斤级的标定");
                    commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            commonDialog.dismiss();
                        }
                    });
                    commonDialog.show();
                } else {
                    position += CalibrationUtil.SEGMENT_NUM;

                }
                LogUtil.e("更新::::"+ calibration.toString());
                try {
                    Writer.setParameter((position - 10) * 4856, MotorConstant.SET_FRONTLIMIT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //修改数据库
                try {
                    db.update(calibration);
                } catch (DbException e) {
                    e.printStackTrace();
                }
                Toast.makeText(SegmentCalibrationActivity.this, torque + "KG"+ (position- CalibrationUtil.SEGMENT_NUM) + "位置参数保存成功!", Toast.LENGTH_SHORT).show();
                updateViews(false);
                haveDataChanged = false;
            }
        });

        //恢复出厂设置
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CommonDialog commonDialog = new CommonDialog(SegmentCalibrationActivity.this);
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
                        try {
                            calibrationUtil.resetSegmentCalibration();
                        } catch (DbException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(SegmentCalibrationActivity.this, "当前标定参数值已经恢复到出厂设置!", Toast.LENGTH_SHORT).show();
                        commonDialog.dismiss();
                        SegmentCalibrationActivity.this.finish();
                    }
                });
                commonDialog.show();
            }
        });

        //返回按钮
        returnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CommonDialog commonDialog = new CommonDialog(SegmentCalibrationActivity.this);
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
                        SegmentCalibrationActivity.this.finish();
                    }
                });
                if (haveDataChanged) {
                    commonDialog.show();
                } else {
                    SegmentCalibrationActivity.this.finish();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
        MotorProcess.motorInitialization();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
