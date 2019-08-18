package com.bdl.airecovery.activity;


import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.telephony.mbms.MbmsErrors;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.constant.MotorConstant;
import com.bdl.airecovery.contoller.Reader;
import com.bdl.airecovery.contoller.Writer;
import com.bdl.airecovery.dialog.CommonDialog;
import com.bdl.airecovery.entity.CalibrationParameter;
import com.bdl.airecovery.service.MotorService;

import org.xutils.DbManager;
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

@ContentView(R.layout.activity_calibration)
public class CalibrationActivity extends BaseActivity {

    @ViewInject(R.id.calibration_toque)          //力矩值
    private Spinner torqueSpinner;

    @ViewInject(R.id.calibration_front_limit)    //前方限制
    private Spinner frontLimitSpinner;

    @ViewInject(R.id.calibration_rear_limit)     //后方限制
    private Spinner rearLimitSpinner;

    @ViewInject(R.id.calibration_min_torque)     //最小力矩
    private Spinner minTorqueSpinner;

    @ViewInject(R.id.calibration_back_speed)     //返回速度
    private Spinner backSpeedSpinner;

    @ViewInject(R.id.calibration_normal_speed)   //非运动状态下的速度
    private Spinner normalSpeedSpinner;

    @ViewInject(R.id.calibration_min_back_torque)//最小返回力矩
    private Spinner minBackTorqueSpinner;

    @ViewInject(R.id.calibration_bounce)         //初始反弹力量
    private Spinner bounceSpinner;

    @ViewInject(R.id.calibration_lead)          //提前量
    private Spinner leadSpinner;

    @ViewInject(R.id.calibration_btn_update)    //保存设置
    private Button btnUpdateCalibrationParam;

    @ViewInject(R.id.calibration_btn_reset)     //恢复出厂设置
    private Button btnResetCalibrationParam;

    @ViewInject(R.id.calibration_btn_return)    //返回
    private Button btnReturnCalibrationParam;

    private int torque = 0;
    private int frontLimitedPosition = 0;
    private int rearLimitedPosition = 0;

    private Timer timer = new Timer();
    private DbManager db = MyApplication.getInstance().getDbManager(); //获取DbManager对象
    private CalibrationParameter calibrationParameter = null;
    private boolean haveDataChanged = false; //是否修改过数据
    private int deviceType = MyApplication.getInstance().getCurrentDevice().getDeviceType(); //获得设备信息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();                //初始化数据
        MotorService                //运动前电机的初始化
                .getInstance()
                .initializationBeforeStart(1500000, deviceType, 500, 500);
        initLimit();                //初始化前后方限制
        openMovementProcess();      //打开运动过程
        setSpinnerOnclickEvent();   //设置Spinner点击事件
        setBtnOnclickEvent();       //设置Button点击事件
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 初始前后方限制
     */
    private void initLimit() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Writer.setParameter(150 * 4856, MotorConstant.SET_FRONTLIMIT);
                    Writer.setParameter(30 * 4856, MotorConstant.SET_REARLIMIT);
                    setParameter(calibrationParameter.getBackSpeed() * 100, MotorConstant.SET_BACK_SPEED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 初始化各种数据
     */
    private void initParam() {
        //获取数据库中的数据
        calibrationParameter = MyApplication.getInstance().getCalibrationParam();
        //初始化各个参数的范围
        List<Integer> torqueList = generateRange(5, 100, 1);
        List<Integer> frontLimitList = generateRange(0, 200, 5);
        List<Integer> rearLimitList = generateRange(0, 200, 5);
        List<Integer> minTorqueList = generateRange(0, 20, 1);
        List<Integer> backSpeedList = generateRange(10, 60, 1);
        List<Integer> normalSpeedList = generateRange(2, 10, 1);
        List<Integer> minBackTorqueList = generateRange(5, 50, 1);
        List<Integer> bounceList = generateRange(0, 20, 1);
        List<Integer> leadList = generateRange(0, 10, 1);

        //创建Spinner
        createSpinner(torqueSpinner, torqueList,
                getIndexOfList(torqueList, 5));
        createSpinner(frontLimitSpinner, frontLimitList,
                getIndexOfList(frontLimitList, 150));
        createSpinner(rearLimitSpinner, rearLimitList,
                getIndexOfList(rearLimitList, 30));
        createSpinner(minTorqueSpinner, minTorqueList,
                getIndexOfList(minTorqueList, calibrationParameter.getMinTorque()));
        createSpinner(backSpeedSpinner, backSpeedList,
                getIndexOfList(backSpeedList, calibrationParameter.getBackSpeed()));
        createSpinner(normalSpeedSpinner, normalSpeedList,
                getIndexOfList(normalSpeedList, calibrationParameter.getNormalSpeed()));
        createSpinner(minBackTorqueSpinner, minBackTorqueList,
                getIndexOfList(minBackTorqueList, calibrationParameter.getMinBackTorque()));
        createSpinner(bounceSpinner, bounceList,
                getIndexOfList(bounceList, calibrationParameter.getBounce()));
        createSpinner(leadSpinner, leadList,
                getIndexOfList(leadList, calibrationParameter.getLead()));
    }

    /**
     * 设备运动过程
     */
    private void openMovementProcess() {
        //打开运动过程
        final int[] lastLocation = {frontLimitedPosition}; //上一次的位置，初始值为前方限制
        //如果出现修改，该位置就改变
        final boolean[] haveStopped = {false};
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //读取当前位置
                    int currentSpeed = Math.abs(Integer.valueOf(Reader.getRespData(MotorConstant.READ_ROTATIONAL_SPEED)));
                    String currentLocation = Reader.getRespData(MotorConstant.READ_ACTUAL_LOCATION);
                    int difference = Integer.parseInt(currentLocation) - lastLocation[0]; //本次位置和上次读到的位置差
                    if (currentSpeed <= 10 && torque < calibrationParameter.getMinBackTorque() * 100) {
                        setParameter(calibrationParameter.getMinBackTorque() * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                        haveStopped[0] = true;
                    }
                    if (difference > 20000) { //回程
                        //超过前方限制
                        if (Integer.valueOf(currentLocation) >= frontLimitedPosition - 50000) {

                            if (haveStopped[0]) { //是否需要恢复反向力量
                                setParameter(torque, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                                haveStopped[0] = false;
                            }
                        }
                        //更新lastLocation
                        lastLocation[0] = Integer.parseInt(currentLocation);
                    } else if (difference < -20000) {//去程
                        //转速超过500，且与最新的限位比较，如果距离大于20000，则可以继续更改，考虑一些延时的因素
                        if (currentSpeed >= 450 && currentSpeed <= 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 100 - 2; //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        } else if (currentSpeed > 1000 && Integer.valueOf(currentLocation) > rearLimitedPosition + 50000) {
                            int leads = currentSpeed / 100 + calibrationParameter.getLead(); //提前量
                            setParameter(leads * 10000, MotorConstant.SET_LEADS);
                        }
                        //超过后方限制
                        if (Integer.valueOf(currentLocation) < rearLimitedPosition + 50000) {
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
     * 创建下拉框
     *
     * @param spinner
     * @param list
     */
    private void createSpinner(final Spinner spinner, List<Integer> list, int position) {
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, list);  //创建一个数组适配器
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        spinner.setAdapter(adapter);
        spinner.setSelection(position, true);
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
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setParameter((torque + calibrationParameter.getMinTorque()) * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                            setParameter((torque + calibrationParameter.getMinTorque()) * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                            setInitialBounce((torque + calibrationParameter.getBounce()) * 100);
                            setKeepArmTorque(torque * 100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //前方限制Spinner点击事件
        frontLimitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                frontLimitedPosition = Integer.parseInt(frontLimitSpinner.getItemAtPosition(i).toString());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setParameter(frontLimitedPosition * 4856, MotorConstant.SET_FRONTLIMIT);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //后方限制Spinner点击事件
        rearLimitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                rearLimitedPosition = Integer.parseInt(rearLimitSpinner.getItemAtPosition(i).toString());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setParameter(rearLimitedPosition * 4856, MotorConstant.SET_FRONTLIMIT);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //最小力矩Spinner点击事件
        minTorqueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                haveDataChanged = true;
                int minTorque = Integer.parseInt(minTorqueSpinner.getItemAtPosition(i).toString());
                calibrationParameter.setMinTorque(minTorque);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setParameter((torque + calibrationParameter.getMinTorque()) * 100, MotorConstant.SET_POSITIVE_TORQUE_LIMITED);
                            setParameter((torque + calibrationParameter.getMinTorque()) * 100, MotorConstant.SET_NEGATIVE_TORQUE_LIMITED);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //返回速度Spinner点击事件
        backSpeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                haveDataChanged = true;
                int backSpeed = Integer.parseInt(backSpeedSpinner.getItemAtPosition(i).toString());
                calibrationParameter.setBackSpeed(backSpeed);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setParameter(calibrationParameter.getBackSpeed() * 100, MotorConstant.SET_BACK_SPEED);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //非运动状态下速度Spinner点击事件
        normalSpeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                haveDataChanged = true;
                int normalSpeed = Integer.parseInt(normalSpeedSpinner.getItemAtPosition(i).toString());
                calibrationParameter.setNormalSpeed(normalSpeed);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //最小返回力矩Spinner点击事件
        minBackTorqueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                haveDataChanged = true;
                int minBackTorque = Integer.parseInt(minBackTorqueSpinner.getItemAtPosition(i).toString());
                calibrationParameter.setMinBackTorque(minBackTorque);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //初始反弹力矩
        bounceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                haveDataChanged = true;
                int bounce = Integer.parseInt(bounceSpinner.getItemAtPosition(i).toString());
                calibrationParameter.setBounce(bounce);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //提前量Spinner点击事件
        leadSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                haveDataChanged = true;
                int lead = Integer.parseInt(leadSpinner.getItemAtPosition(i).toString());
                calibrationParameter.setLead(lead);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    /**
     * 恢复默认值
     */
    private void resetCalibrationParameter() {
        calibrationParameter.setMinTorque(0);
        calibrationParameter.setBackSpeed(35);
        calibrationParameter.setNormalSpeed(5);
        calibrationParameter.setMinBackTorque(20);
        calibrationParameter.setBounce(10);
        calibrationParameter.setLead(3);
        try {
            db.update(calibrationParameter);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    /**
     * 按钮点击事件
     */
    private void setBtnOnclickEvent() {
        //更新的参数
        btnUpdateCalibrationParam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    db.update(calibrationParameter);
                    Toast.makeText(CalibrationActivity.this, "保存成功!", Toast.LENGTH_SHORT).show();
                    haveDataChanged = false;
                } catch (DbException e) {
                    e.printStackTrace();
                }
            }
        });

        //恢复出厂设置
        btnResetCalibrationParam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CommonDialog commonDialog = new CommonDialog(CalibrationActivity.this);
                commonDialog.setTitle("温馨提示");
                commonDialog.setMessage("当前操作会将标定参数恢复到出厂默认值,确定继续进行该操作？");
                commonDialog.setOnNegativeClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        commonDialog.dismiss();
                    }
                });
                commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        resetCalibrationParameter();
                        Toast.makeText(CalibrationActivity.this, "当前标定参数值已经恢复到出厂设置!", Toast.LENGTH_SHORT).show();
                        commonDialog.dismiss();
                        CalibrationActivity.this.finish();
                    }
                });
                commonDialog.show();
            }
        });

        //返回按钮
        btnReturnCalibrationParam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (haveDataChanged) {
                    final CommonDialog commonDialog = new CommonDialog(CalibrationActivity.this);
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
                            CalibrationActivity.this.finish();
                        }
                    });
                    commonDialog.show();
                } else {
                    CalibrationActivity.this.finish();
                }
            }
        });
    }

    /**
     * 生成固定间距的list
     *
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
        return list;
    }

    /**
     * 获取List某个值的下标值
     *
     * @param list
     * @param value
     * @return
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
}
