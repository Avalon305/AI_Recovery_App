package com.bdl.airecovery.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;
import com.bdl.airecovery.contoller.MotorProcess;
import com.bdl.airecovery.dialog.MenuDialog;
import com.bdl.airecovery.entity.DTO.PersonalSettingDTO;

import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import com.bdl.airecovery.MyApplication;
import com.bdl.airecovery.entity.Personal;
import com.bdl.airecovery.entity.TempStorage;
import com.bdl.airecovery.entity.login.Helperuser;
import com.bdl.airecovery.proto.BdlProto;
import com.google.gson.Gson;
import com.bdl.airecovery.dialog.CommonDialog;

import java.util.ArrayList;
import java.util.List;

import com.bdl.airecovery.personalInfoDAO;
import com.bdl.airecovery.service.StaticMotorService;

@ContentView(R.layout.activity_personal_setting)
public class PersonalSettingActivity extends BaseActivity {

    /**
     * 医护设置界面
     * 主要业务：
     *      根据系统设置的设备类型，查询当前设备图片、当前设备可调节的部位
     *      根据当前用户，查询当前用户名、当前设备的参数
     *      根据当前设备可调节的部位（不同设备，可调节部位的数量不同），在左侧显示相应的按钮，并监听
     *      根据左侧点击相应的调节按钮，当前按钮高亮显示，同时在右侧实时显示提示文本、参数值与拖动条数值
     *      监听“设置模式”按钮，点击弹出单选菜单模态框，可选择当前训练的模式
     *      监听“保存医护设置”按钮，点击会将当前设备的医护设置参数传给教练机保存
     *      监听“返回”按钮
     */

    /**
     * 类成员
     */
    private DbManager db;                       //数据库管理单例
    private List<Button> btn_params;            //将5个参数按钮添加到集合类，方便动态设置按钮
    private int btn_params_id[];                //将5个参数按钮对应ID保存，方便动态设置
    private int curIndex = 0;                   //记录当前点击的参数按钮索引（默认为0，第一个参数按钮）
    private int SeekBarCurProgress = 0;         //保存当前拖动条的值
    private String curMode;                     //当前训练模式
    private int curModeIndex = 0;               //选择模式的索引值（默认为0，即“标准模式”）
    private Boolean isSave = true;              //标识用户是否保存医护设置（一旦对 参数/模式 进行修改，则为false）
    String[] modeItems = new String[]{          //设置菜单选项内容（有5种训练模式可设置）（通过curModeIndex选择，默认为0，即“标准模式”）
            "主被动模式",
            "被动模式",
            "康复模式"};
    //发送保存医护设置请求需要打包的参数
    private int deviceTypeValue;            //设备类型
    private int activityTypeValue;          //循环类型
    private int seatHeight;                 //座位高度
    private int backDistance;               //靠背距离
    private int leverLength;                //杠杆长度
    private double leverAngle;              //杠杆角度
    private int frontLimit;                 //前方限制
    private int backLimit;                  //后方限制
    private int power;                      //初始功率
//    private BdlProto.TrainMode trainMode; //训练模式 （使用curModeIndex即可）

    /**
     * 控件绑定
     */
    //TextView
    @ViewInject(R.id.tv_user_name)
    private TextView tv_user_name;                  //用户名
    @ViewInject(R.id.tv_tips_right)
    private TextView tv_tips_right;                 //右侧提示文本（根据左侧调节按钮的选择，显示对应文本）
    @ViewInject(R.id.tv_number)
    private TextView tv_number;                     //参数大小数值
    @ViewInject(R.id.tv_mode_note)
    private TextView tv_mode_note;                  //提示文本 当前模式：
    @ViewInject(R.id.tv_cur_mode)
    private TextView tv_cur_mode;                   //当前模式文本（根据设置模式按钮弹出模态框选择后显示在该文本中）
    //ImageView
    @ViewInject(R.id.iv_dev_setting)
    private ImageView iv_dev_setting;               //设备图片
    @ViewInject(R.id.iv_ps_state)
    private ImageView iv_ps_state;                  //登录状态
    @ViewInject(R.id.iv_param_add)
    private ImageView iv_param_add;                 //参数加号
    @ViewInject(R.id.iv_param_minus)
    private ImageView iv_param_minus;               //参数减号
    //Button
    @ViewInject(R.id.btn_param_0)
    private Button btn_param_0;            //参数0 按钮
    @ViewInject(R.id.btn_param_1)
    private Button btn_param_1;            //参数1 按钮
    @ViewInject(R.id.btn_param_2)
    private Button btn_param_2;            //参数2 按钮
    @ViewInject(R.id.btn_param_3)
    private Button btn_param_3;            //参数3 按钮
    @ViewInject(R.id.btn_param_4)
    private Button btn_param_4;            //参数4 按钮
    @ViewInject(R.id.btn_setting_mode)
    private Button btn_setting_mode;       //“设置模式”按钮
    @ViewInject(R.id.btn_save)
    private Button btn_save;               //“保存医护设置”按钮
    @ViewInject(R.id.btn_back)
    private Button btn_back;               //“返回”按钮
    //SeekBar
    @ViewInject(R.id.seekBar)
    private SeekBar seekBar;                        //用于医护设置中调节各选项的拖动条


    Helperuser helperuser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取数据库管理单例
        if (MyApplication.getInstance() != null && MyApplication.getInstance().getDbManager() != null) {
            db = MyApplication.getInstance().getDbManager(); //获取DbManager
        }
        initImmersiveMode(); //隐藏状态栏与导航栏
        queryDevParam(); //查询设备参数
        SeekBarCartoonSetting(); //拖动条动画设置
        defaultSetting(); //控件内容的默认设置
        queryUserInfo(); //获取用户相关信息
//        isOpenFatLossModeEvent(); //减脂模式CheckBox事件监听
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //调用onDestroy方法后电机复位到初始位置
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MotorProcess.motorInitialization();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 右侧拖动条动画设置
     */
    private void SeekBarCartoonSetting() {
        //对拖动条设置监听事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            //拖动条监督被改变的时候调用该方法
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                //标识当前拖动进度
                SeekBarCurProgress = progress;
                tv_number.setText("" + SeekBarCurProgress);
            }

            @Override
            //拖动条开始拖动的时候调用该方法
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            //拖动条停止拖动的时候调用
            public void onStopTrackingTouch(SeekBar seekBar) {
                //判断参数值是否变化，如果变化了
                if (MyApplication.getInstance().getCurrentDevice() != null && MyApplication.getInstance().getCurrentDevice().getPersonalList() != null) {
                    //1.拖动结束，保存当前参数值
                    MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).setValue(String.valueOf(SeekBarCurProgress));
                    //2.更新标识
                    isSave = false;
                    //3.如果是座位高度，靠背距离，实时发送参数给静态电机
                    //3.0安卓-电机传值映射：AndroidParam*10+10 = MotorParam
                    //3.1获取当前电机
                    if (MyApplication.getInstance().getCurrentDevice() != null && MyApplication.getInstance().getCurrentDevice().getPersonalList() != null) {
                        if (curIndex >= 0 && MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex) != null) {
                            switch (String.valueOf(tv_tips_right.getText())) {
                                case "座位高度":
                                case "靠背距离":
                                    int MotorIndex = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getMachine());
                                    int MotorType = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getType());
                                    //发送参数给静态电机
                                    Intent intent = new Intent(PersonalSettingActivity.this, StaticMotorService.class);
                                    intent.putExtra("position", SeekBarCurProgress * 10 + 10);
                                    intent.putExtra("index", MotorIndex);
                                    intent.putExtra("type", MotorType);
                                    intent.putExtra("command", "SETPOSITION");
                                    startService(intent);
                                    Log.d("医护设置", "发送参数给静态电机[ Position=" + SeekBarCurProgress + "MotorIndex=" + MotorIndex + "]");
                                    break;
                                case "前方限制":
                                case "后方限制":
                                    try {
                                        MotorProcess.motorDirection(SeekBarCurProgress);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;
                            }
                        }
                    }
                }

            }
        });
    }

    /**
     * 参数加/减号单击事件
     */
    @Event({R.id.iv_param_add, R.id.iv_param_minus})
    private void iv_param_add_onClick(View v) {
        isSave = false;

        //0.获取当前参数值，相应增减数值
        int curParam = Integer.parseInt(tv_number.getText().toString());
        switch (v.getId()) {
            case R.id.iv_param_add:
                if (curParam != seekBar.getMax()) {
                    curParam++;
                } else {
                    return;
                }
                break;
            case R.id.iv_param_minus:
                if (curParam != 0) {
                    curParam--;
                } else {
                    return;
                }
                break;
            default:
                return;
        }
        //1.更新实体类currentDevice
        MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).setValue(String.valueOf(curParam));
        //2.更新控件
        tv_number.setText(String.valueOf(curParam));
        SeekBarCurProgress = curParam;
        seekBar.setProgress(curParam);
        //3.如果是座位高度，靠背距离，实时发送参数给静态电机
        //3.0安卓-电机传值映射：AndroidParam*10+10 = MotorParam
        //3.1获取当前电机
        if (MyApplication.getInstance().getCurrentDevice() != null && MyApplication.getInstance().getCurrentDevice().getPersonalList() != null) {
            if (curIndex >= 0 && MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex) != null) {
                switch (tv_tips_right.getText().toString()) {
                    case "座位高度":
                    case "靠背距离":
                        int MotorIndex = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getMachine());
                        //发送参数给静态电机
                        Intent intent = new Intent(this, StaticMotorService.class);
                        intent.putExtra("position", curParam * 10 + 10);
                        intent.putExtra("index", MotorIndex);
                        intent.putExtra("command", "SETPOSITION");
                        startService(intent);
                        Log.d("医护设置", "发送参数给静态电机[ Position=" + curParam + "MotorIndex=" + MotorIndex + "]");
                        break;
                    case "前方限制":
                    case "后方限制":
                        try {
                            MotorProcess.motorDirection(SeekBarCurProgress);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }

    }

    /**
     * 查询设备参数、设备图片
     */
    private void queryDevParam() {
        //获取设备信息
        if (MyApplication.getInstance().getCurrentDevice() == null) {
            return;
        }
        //获取医护设置表
        if (MyApplication.getInstance().getCurrentDevice().getPersonalList() == null) {
            return;
        }
        //设置设备图片
        if (MyApplication.getInstance().getCurrentDevice().getGeneralImg() != null && !MyApplication.getInstance().getCurrentDevice().getGeneralImg().equals("")) {
            iv_dev_setting.setImageResource(getResources().getIdentifier(MyApplication.getInstance().getCurrentDevice().getGeneralImg(), "drawable", getPackageName()));
        }

        //将5个参数按钮添加到集合类，方便动态设置按钮
        btn_params = new ArrayList<Button>();
        btn_params.add(btn_param_0);
        btn_params.add(btn_param_1);
        btn_params.add(btn_param_2);
        btn_params.add(btn_param_3);
        btn_params.add(btn_param_4);
        //将5个参数按钮对应的R.id保存，方便动态设置
        btn_params_id = new int[5];
        btn_params_id[0] = R.id.btn_param_0;
        btn_params_id[1] = R.id.btn_param_1;
        btn_params_id[2] = R.id.btn_param_2;
        btn_params_id[3] = R.id.btn_param_3;
        btn_params_id[4] = R.id.btn_param_4;

        for (int i = 0; i < MyApplication.getInstance().getCurrentDevice().getPersonalList().size(); ++i) {
            //根据当前设备的参数个数，动态显示按钮个数（第一个值为训练模式，其余为设备可调参数）
            btn_params.get(i).setVisibility(View.VISIBLE);
            //判空
            if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getName() != null
                    && !MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getName().equals("")) {
                //根据当前设备的参数，动态显示按钮内容
                btn_params.get(i).setText(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getName());
            }
        }
    }

//    /**
//     * 减脂模式 CheckBox
//     */
//    private void isOpenFatLossModeEvent() {
//        cb_isopen_fatlossmode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked) {
//                    isOpenFatLossMode = true;
//                } else {
//                    isOpenFatLossMode = false;
//                }
//                isSave = false;
//            }
//        });
//    }

    /**
     * “参数”按钮的单击事件
     */
    @Event({R.id.btn_param_0, R.id.btn_param_1, R.id.btn_param_2, R.id.btn_param_3, R.id.btn_param_4})
    private void btn_param_onClick(View v) {
        //判空
        if (MyApplication.getInstance().getCurrentDevice().getPersonalList() == null) {
            return;
        }

        //恢复上一次点击的按钮样式，保持高亮效果只存在于当前点击的按钮中
        btn_params.get(curIndex).setBackgroundResource(R.drawable.btnbackground);

        for (int i = 0; i < 5; ++i) {
            if (v.getId() == btn_params_id[i]) {
                //更改选中按钮的样式
                btn_params.get(i).setBackgroundResource(R.drawable.btnbackground_pressed);
                //判空
                if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i) != null && MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getName() != null && !MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getName().equals("")) {
                    //更新右侧文本的提示内容
                    tv_tips_right.setText(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getName());
                    //更新右侧参数值
                    tv_number.setText(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(i).getValue());
                }
                //记录当前选中的按钮索引
                curIndex = i;
            }
        }

        //更新右侧拖动条动画
        if (curIndex >= 0 && MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex) != null) {
            if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getMax() != null && !MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getMax().equals("")) {
                //更新拖动条最大值
                //如果参数是double类型（杠杆角度/初始功率）
                if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getName().equals("杠杆角度") || MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getName().equals("初始功率")) {
                    //转换为Integer
                    Double paramDouble = Double.parseDouble(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getMax());
                    Integer paramInteger = paramDouble.intValue();
                    seekBar.setProgress(paramInteger);
                } else {
                    seekBar.setMax(Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getMax()));
                }
            }
            if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getValue() != null && !MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getValue().equals("")) {
                //更新拖动条进度（参数需要int类型）
                //如果参数是double类型（杠杆角度/初始功率）
                if (MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getName().equals("杠杆角度") || MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getName().equals("初始功率")) {
                    //转换为Integer
                    Double paramDouble = Double.parseDouble(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getValue());
                    Integer paramInteger = paramDouble.intValue();
                    seekBar.setProgress(paramInteger);
                } else {
                    seekBar.setProgress(Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getValue()));
                }
            }
        }
    }

    /**
     * “设置模式”按钮的单击事件
     */
    @Event(R.id.btn_setting_mode)
    private void setBtn_setting_mode_onClick(View v) {
        List<String> menuItemsList = new ArrayList<String>();
        for(int i = 0; i < modeItems.length; i++) {
            menuItemsList.add(modeItems[i]);
        }

        final MenuDialog menuDialog = new MenuDialog(PersonalSettingActivity.this);
        menuDialog.setTitle("选择训练模式");
        menuDialog.setMenuItems(menuItemsList);
        menuDialog.setSelectedIndex(curModeIndex);
        //ListView子项点击事件监听
        menuDialog.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(curModeIndex != i) {
                    //更新标识
                    isSave = false;
                    //更新当前选择项的索引
                    curModeIndex = i;
                    //更新当前模式
                    curMode = modeItems[curModeIndex];
                    //更新文本
                    tv_cur_mode.setText(modeItems[i]);
                }
                menuDialog.dismiss();
            }
        });
        //模态框隐藏导航栏
        menuDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        menuDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        //全屏
                        //View.SYSTEM_UI_FLAG_FULLSCREEN |
                        //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= 19) {
                    uiOptions |= 0x00001000;
                } else {
                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                }
                menuDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });
        menuDialog.show();
    }

    /**
     * “保存医护设置”按钮的单击事件
     * 保存业务有：
     * 1.更新实体类User与实体类CurrentDevice（已实时更新）
     * 2.保存数据到本地PersonalInfo表
     * 3.获取需要打包的数据
     * 4.打包数据保存到暂存表
     * 5.返回主界面
     */
    @Event(R.id.btn_save)
    private void btn_save_onClick(View v) {
        //如果标识为假，表明有修改过医护设置且未保存，此时点击保存按钮可以保存
        if (!isSave) {
            int hasFrontBackLimit = 0; //判断是否既有前方限制，又有后方限制
            //1.获取需要打包的数据
            if (MyApplication.getInstance().getCurrentDevice() != null) {
                //1.1.获取设备类型
                if (MyApplication.getInstance().getCurrentDevice().getDeviceInnerID() != null) {
                    deviceTypeValue = Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getDeviceInnerID()); //得到枚举量
                }
//                //1.2.获取循环类型
//                activityTypeValue = MyApplication.getInstance().getCurrentDevice().getActivityType();
                //1.3.获取训练模式
                //trainModeValue = curModeIndex
                //1.4.获取医护设置参数
                if (MyApplication.getInstance().getCurrentDevice().getPersonalList() != null && MyApplication.getInstance().getCurrentDevice().getPersonalList().size() > 0) {
                    for (Personal curPersonal : MyApplication.getInstance().getCurrentDevice().getPersonalList()) {
                        switch (curPersonal.getName()) {
                            case "前方限制":
                                frontLimit = Integer.parseInt(curPersonal.getValue());
                                hasFrontBackLimit++;
                                break;
                            case "后方限制":
                                backLimit = Integer.parseInt(curPersonal.getValue());
                                hasFrontBackLimit++;
                                break;
                            case "座位高度":
                                seatHeight = Integer.parseInt(curPersonal.getValue());
                                break;
                            case "靠背距离":
                                backDistance = Integer.parseInt(curPersonal.getValue());
                                break;
                            case "杠杆角度":
                                leverAngle = Double.parseDouble(curPersonal.getValue());
                                break;
                            default:
                                break;
                        }
                    }
                }
            }

            //2.如果既有前方限制，又有后方限制，判断两者大小
            if (hasFrontBackLimit == 2) {
                //如果前方小于等于后方，不符合规定，弹出不能保存的模态框
                if (frontLimit <= backLimit) {
                    final CommonDialog cannotSaveDialog = new CommonDialog(PersonalSettingActivity.this);
                    cannotSaveDialog.setTitle("警告");
                    cannotSaveDialog.setMessage("前方限制必须大于后方限制，请更改后重新保存");
                    cannotSaveDialog.setOnPositiveClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            cannotSaveDialog.dismiss();
                        }
                    });
                    cannotSaveDialog.setPositiveBtnText("我知道了");
                    //模态框隐藏导航栏
                    cannotSaveDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    cannotSaveDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                    //布局位于状态栏下方
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                    //全屏
                                    //View.SYSTEM_UI_FLAG_FULLSCREEN |
                                    //隐藏导航栏
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                            if (Build.VERSION.SDK_INT >= 19) {
                                uiOptions |= 0x00001000;
                            } else {
                                uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                            }
                            cannotSaveDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
                        }
                    });
                    cannotSaveDialog.show();

                    return;
                }
            }

            //3.更新实体类User与实体类CurrentDevice
            //3.1.更新实体类User
            if (MyApplication.getInstance().getUser() != null) {
//                MyApplication.getInstance().getUser().setDefatModeEnable(isOpenFatLossMode); //更新 是否开启减脂模式
                MyApplication.getInstance().getUser().setTrainMode(curMode); //更新 训练模式
            }
            //3.2.更新实体类CurrentDevice（已实时更新）

            //4.保存数据到本地PersonalInfo表
            try {
                if (MyApplication.getInstance().getUser() != null && MyApplication.getInstance().getUser().getUserId() != null && MyApplication.getInstance().getCurrentDevice() != null) {
                    personalInfoDAO.getInstance().SavrOrUpdata(MyApplication.getInstance().getUser().getUserId(),BdlProto.DeviceType.getDescriptor().getName(),MyApplication.getInstance().getUser(), MyApplication.getInstance().getCurrentDevice());
                }
            } catch (DbException e) {
                e.printStackTrace();
            }


            //5.打包医护设置
            PersonalSettingDTO personalSettingDTO = new PersonalSettingDTO();
            if (MyApplication.getInstance().getUser() != null && MyApplication.getInstance().getUser().getUserId() != null) {
                personalSettingDTO.setUid(MyApplication.getInstance().getUser().getUserId());
                personalSettingDTO.setSeatHeight(seatHeight);
                personalSettingDTO.setBackDistance(backDistance);
                personalSettingDTO.setLeverAngle(leverAngle);
                personalSettingDTO.setForwardLimit(frontLimit);
                personalSettingDTO.setBackLimit(backLimit);
                personalSettingDTO.setTrainMode(curModeIndex);
            }
            //6.存暂存表
            Log.d("暂存业务", "保存医护设置数据至暂存表：" + personalSettingDTO.toString());
            TempStorage tempStorage = new TempStorage();
            Gson gson = new Gson();
            tempStorage.setData(gson.toJson(personalSettingDTO)); //重传数据（转换为JSON串）
            tempStorage.setType(1); //重传类型
            try {
                db.saveBindingId(tempStorage);
            } catch (DbException e) {
                e.printStackTrace();
                Log.d("暂存业务", "医护设置暂存失败");
            }


            //更新标识
            isSave = true;
            //7.弹出“保存医护设置成功”模态框
            final CommonDialog commonDialog = new CommonDialog(PersonalSettingActivity.this);
            commonDialog.setTitle("温馨提示");
            commonDialog.setMessage("医护设置信息保存成功");
            commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    commonDialog.dismiss();
                }
            });
            commonDialog.setPositiveBtnText("我知道了");
            //模态框隐藏导航栏
            commonDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            commonDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            //布局位于状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            //全屏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN |
                            //隐藏导航栏
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    if (Build.VERSION.SDK_INT >= 19) {
                        uiOptions |= 0x00001000;
                    } else {
                        uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                    }
                    commonDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
                }
            });
            commonDialog.show();
        } else {
            //弹出“您未进行修改，无需保存”模态框
            final CommonDialog commonDialog = new CommonDialog(PersonalSettingActivity.this);
            commonDialog.setTitle("温馨提示");
            commonDialog.setMessage("您尚未修改任何信息，无需保存");
            commonDialog.setPositiveBtnText("我知道了");
            commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    commonDialog.dismiss();
                }
            });
            //模态框隐藏导航栏
            commonDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            commonDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            //布局位于状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            //全屏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN |
                            //隐藏导航栏
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    if (Build.VERSION.SDK_INT >= 19) {
                        uiOptions |= 0x00001000;
                    } else {
                        uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                    }
                    commonDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
                }
            });
            commonDialog.show();
        }
    }

    /**
     * “返回”按钮的单击事件
     */
    @Event(R.id.btn_back)
    private void btn_back_onClick(View v) {
        //如果标识为真，表明已经保存或未修改医护设置，此时可以返回主界面
        if (isSave) {
            Intent intent = new Intent(PersonalSettingActivity.this, MainActivity.class); //新建一个跳转到主界面Activity的显式意图
            startActivity(intent); //启动
            PersonalSettingActivity.this.finish(); //结束当前Activity
        } else {
            //弹出“您的医护设置已修改，请先保存医护设置”模态框
            final CommonDialog commonDialog = new CommonDialog(PersonalSettingActivity.this);
            commonDialog.setTitle("温馨提示");
            commonDialog.setMessage("您的医护设置信息尚未保存");
            commonDialog.setPositiveBtnText("我知道了");
            commonDialog.setOnPositiveClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    commonDialog.dismiss();
                }
            });
            //模态框隐藏导航栏
            commonDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            commonDialog.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            //布局位于状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            //全屏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN |
                            //隐藏导航栏
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    if (Build.VERSION.SDK_INT >= 19) {
                        uiOptions |= 0x00001000;
                    } else {
                        uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                    }
                    commonDialog.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
                }
            });
            commonDialog.show();
        }
    }

    /**
     * 界面控件内容的默认设置（该方法必须在queryDevParam()之后调用）
     */
    private void defaultSetting() {
        //默认高亮第一个按钮
        curIndex = 0;
        btn_params.get(curIndex).setBackgroundResource(R.drawable.btnbackground_pressed);
        //动画部分默认显示第一个参数（所以该方法必须在queryDevParam()之后调用）
        //更新右侧拖动条动画
        if (MyApplication.getInstance().getCurrentDevice().getPersonalList() != null && curIndex >= 0 && MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex) != null) {
            //更新拖动条最大值
            seekBar.setMax(Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getMax()));
            //更新拖动条进度（参数需要int类型）
            seekBar.setProgress(Integer.parseInt(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getValue()));
            //更新提示文本
            tv_tips_right.setText(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getName());
            //更新参数值
            tv_number.setText(MyApplication.getInstance().getCurrentDevice().getPersonalList().get(curIndex).getValue());
        }
        //默认选择 标准模式
        curMode = modeItems[curModeIndex];
        //更新文本
        tv_cur_mode.setText(curMode);
    }

    /**
     * 查询需要的用户类信息（需要先执行queryDevParam()查询设备信息）
     */
    private void queryUserInfo() {
        //获取实体类User
        if (MyApplication.getInstance().getUser() == null) {
            tv_user_name.setText("开发者");
            iv_ps_state.setImageDrawable(getResources().getDrawable(R.drawable.banshou1));
            return;
        }

        //获取用户名
        if (MyApplication.getInstance().getUser().getUserId() != null && !MyApplication.getInstance().getUser().getUserId().equals("")) {
            tv_user_name.setText(MyApplication.getInstance().getUser().getUserId());
            iv_ps_state.setImageDrawable(getResources().getDrawable(R.drawable.yonghu1));
        }

        //获取当前训练模式
        if (MyApplication.getInstance().getUser().getTrainMode() != null && !MyApplication.getInstance().getUser().getTrainMode().equals("")) {
            curMode = MyApplication.getInstance().getUser().getTrainMode();
            for (int i = 0; i < modeItems.length; i++) {
                if (curMode.equals(modeItems[i])) {
                    curModeIndex = i;
                }
            }
            tv_cur_mode.setText(curMode); //前端显示当前训练模式
        }
    }

    /**
     * 隐藏状态栏，导航栏
     */
    @SuppressLint("NewApi")
    private void initImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 19) {
            View.OnSystemUiVisibilityChangeListener listener = new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        enterImmersiveMode();
                    }
                }
            };
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(listener);
            enterImmersiveMode();
        }
    }

    @SuppressLint("NewApi")
    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }
}
