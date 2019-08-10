package com.bdl.aisports.widget;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.bdl.aisports.R;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 * 小球控件
 */
public class MyBallView extends View {

    //小球对外参数，需要在使用时进行初始化
    // - 静态参数，只需要设置一次
    public int stateS=-1; //开始运动为1，初始状态赋值为-1，通过Activity激活state状态
    public float MaxR; //合理的速度范围的最大边界值
    public float MinR; //合理的速度范围的最小边界值
    public int frontLimit; //前方限制
    public int backLimit; //后方限制
    // - 动态参数，需要不断更新
    public float powerX; //电机物理位移
    public float speedY; //电机推拉速度

    //小球坐标系
    private float currentX = 40; //画布坐标系的x坐标，一般不要随意修改
    private float currentY = 200; //画布坐标系的y坐标，一般不要随意修改
    private float calculateX; //计算坐标系的x坐标
    private float calculateY; //计算坐标系的y坐标

    //辅助计算参数
    private float rectX=40; //辅助图形的左上角x坐标
    private float rectY=100; //辅助图形的左上角y坐标
    private int aLen=500; //椭圆长轴长度2a,aLen/2为半长轴长度
    private int bLen=200; //椭圆短轴长度2b,bLen/2为半短轴长度

    //辅助判定参数
    private final float errorLimit = 0.5f; //判定到达前后方的误差限制
    private boolean isOnLower = false; //是否在下半轴
    private boolean isOnBackLimit = false; //是否在后方限制
    private boolean isOnFrontLimit = false; //是否在前方限制

    //Canvas绘图
    Paint mPaint = new Paint(); //定义、创建绘制小球的画笔
    private Bitmap bitmap; //绘制小球的位图


    public MyBallView(Context context, AttributeSet set) {
        super(context,set);
        //初始化画笔设置
        init();
        //从资源文件中生成位图Bitmap
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
    }

    @Override
    /*重写绘画部分*/
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制椭圆轨迹
        //canvas.drawPath(mPath, mPaint);
        //绘制小球【参数：圆心坐标X，圆心坐标Y，半径，所使用的画笔】
        //canvas.drawCircle(currentX, currentY, 15, pBall);
        canvas.drawBitmap(bitmap, currentX-25, currentY-25, mPaint);
        //设置小球类的状态，为1时可动
        if(stateS == 1) {
            translateLR();
        }
    }
    /**
     * 初始化设置
     */
    private void init() {
        //Paint.ANTI_ALIAS_FLAG使位图抗锯齿的标志
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    /**
     * 小球坐标更新函数
     * 控制小球按照顺时针绕球往复运动
     * 当 当前所获得的速度/力量在合理范围之内(比如速度在[5,10]之内合理)，小球的x和y符合椭圆方程，呈现效果就是将沿着椭圆进行运动
     * 如果当前速度/力量不再合理范围之内，将根据当前速度脱离椭圆轨道，向心或者离心
     */
    public void translateLR() {
        //椭圆参数
        final float a = aLen / 2; //半长轴
        final float b = bLen / 2; //半短轴

        //映射参数
        final float k = (float)aLen / (frontLimit - backLimit);
        final float c = rectX - backLimit*k;

        //powerX 映射到 画布坐标系中小球的X
        currentX = powerX*k + c;
        //画布坐标系中小球的X 映射到 计算坐标系中小球的X
        calculateX = currentX - rectX - a;

        //speedY分情况讨论
        //1.如果speedY处于合理速度区间内
        calculateY = abs(b*b - b*b/a/a *calculateX*calculateX);
        calculateY = (float)sqrt(calculateY);
        //2.如果speedY不在合理速度区间内，根据偏移量/偏移比率进行相应增减
        final float rate = 2; //偏移比率
        if(abs(speedY) > MaxR) {
            //2.1 如果speedY速度过大
            float diff = abs(speedY) - MaxR;
            calculateY = calculateY + (diff/rate);
        } else if(abs(speedY) < MinR) {
            //2.2 如果speedY速度过小
            float diff = MinR - abs(speedY);
            calculateY = calculateY - (diff/rate);
        }

        //判断是否在前方限制或后方限制附近
        if(powerX < backLimit + errorLimit) {
            //如果到达后方限制
            isOnBackLimit = true;
            isOnFrontLimit = false;
        } else if(powerX > frontLimit - errorLimit) {
            //如果到达前方限制
            isOnFrontLimit = true;
            isOnBackLimit = false;
        }

        //判断是否完成半程（在前方限制或后方限制附近则视为完成）
        if(isOnFrontLimit) {
            if(!isOnLower) {
                //在上半轴且到达前方限制
                isOnLower = true; //更新为下半轴
//                count++;
            }
        } else if(isOnBackLimit) {
            if(isOnLower) {
                //在下半轴且到达后方限制
                isOnLower = false; //更新为上半轴
//                count++;
            }
        }

        //根据小球在上半轴还是下半轴，采取不同的Y值计算
        if(isOnLower) {
            currentY = calculateY + rectY+b;
        } else if(!isOnLower){
            currentY = rectY+b - calculateY;
        }

        //限制小球运动范围，超过指定范围会无视前面的坐标计算，而固定坐标。
        //根据小球X值是否超过椭圆轨迹[rectX,rectX+aLen]，如果超过，固定小球在端点
        if(powerX > frontLimit - errorLimit) {
            //如果在前方限制，固定小球在(rectX+aLen,0)
            currentX = rectX+aLen;
            currentY = bLen;

        } else if(powerX < backLimit + errorLimit) {
            //如果在后方限制，固定小球在(rectX,0)
            currentX = rectX;
            currentY = bLen;
        }

        //重新绘制
        this.invalidate();
    }
}
