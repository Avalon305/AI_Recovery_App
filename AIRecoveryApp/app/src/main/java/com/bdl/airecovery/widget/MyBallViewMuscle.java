package com.bdl.airecovery.widget;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;




/*  Note:可能会用到的一些代码...
    加入电机传入数据，此时的x坐标需要进行映射处理,默认powerX是从0开始的
    this.currentX=(float)(offset+(powerX-(backLimit-frontLimit)/2)*rateX);
    如果speedY过大或过小，小球偏心/向心，此时的坐标需要进行映射处理
    this.currentY=(float)(offsetYu-(speedY-(MaxLimit-MinLimit)/2)*rateY);  上半球
    this.currentY=(float)(offsetYd-(speedY-(MaxLimit-MinLimit)/2)*rateY);  下半球
    关于rateY，不知道实际速度可取的范围值，目前没法做映射处理，所以没有计算，但写了计算公式。
*/
public class MyBallViewMuscle extends View {

    /* TODO 与电机有关的相关参数
    * 1.实时的速度值/力度值，也即小球的y坐标。获取之后需要对其处理转换为适应本坐标系的数值 [该值由电机获得]
    * 2.实时的位移，也即小球的x坐标，需要做的是将本坐标系的数值和位移做映射，实现一一对应 [该值由电机获得]
    * ps:上面这两个参数的获取时间好像是每200ms向电机请求一次数据，这两个数据是同时传过来的
    * */
    /*用于打印当前坐标信息的tag标签说明*/
    protected static final String ACTIVITY_TAG="MyAndroid";
    /*暂时用powerX代替电机传进来的物理位移，默认取值是[0,backLimit-frontLimit]，通过数学计算转换后的结果对应小球的x坐标*/
    public float powerX;
    /*暂时用speedY代替电机传进来的推拉速度，即对应小球的y坐标*/
    public float speedY;
    /*合理的速度范围的最大边界值*/
    public float MaxR;
    /*合理的速度范围的最小边界值*/
    public float MinR;
    /*实际运动器械可达到的最小运动速度*/
    public float MaxLimit;
    /*实际运动器械可达到的最大运动速度*/
    public float MinLimit;
    /*前方限制*/
    public int frontLimit;
    /*后方限制*/
    public int backLimit;
    /*（后方限制-前方限制）与实际aLen(500)之间换算的映射参数*/
    public float rateX;
    //rateX=500/(backLimit-frontLimit);
    /*（运动器械的最大速度-运动器械的最小速度）与实际bLen(200)之间换算的映射参数*/
    public float rateY;
    //rateY=200/(MaxLimit-MinLimit);
    /*小球当前x坐标，一般不要随意修改*/
    private float currentX = 40;
    /*小球当前y坐标，一般不要随意修改*/
    private float currentY = 200;
    /*椭圆上一点与x轴正半轴所成夹角，起始点在最左侧，所以设置为180*/
    private int angleX=180;
    /*椭圆上一点与x轴负半轴所成夹角，起始点在最左侧，所以设置为0*/
    private int angleY=0;
    /*半程计数*/
    private int cnt=0;
    //定义、创建绘制小球的画笔
    Paint pBall = new Paint();
    //定义、创建绘制椭圆的画笔
    Paint mPaint = new Paint();
    //定义、创建绘制路径
    Path mPath;
    /*开始运动为1，初始状态赋值为-1
    * 通过Activity激活state状态*/
    public int stateS=-1;
    /*辅助图形的左上角x坐标*/
    private float rectX=40;
    /*辅助图形的左上角y坐标*/
    private float rectY=100;
    /*椭圆长轴长度2a,aLen/2为半长轴长度*/
    private int aLen=500;
    /*椭圆短轴长度2b,bLen/2为半短轴长度*/
    private int bLen=200;
    /*偏移量,用于校正实际坐标系和计算坐标系之间的偏差,offset是x轴的偏移量*/
    private float offset=rectX+aLen/2;
    /*offsetYu是小球在上半轴的偏移量*/
    private float offsetYu=3*rectY-bLen/2;
    /*offsetYd是小球在下半轴的偏移量*/
    private float offsetYd=rectY+bLen/2;
    /*辅助角*/
    private float tmpAngle;
    /*辅助Y*/
    private float tmpY;
    public MyBallViewMuscle(Context context, AttributeSet set) {
        super(context,set);
        // TODO Auto-generated constructor stub
    }


    @Override
    /*重写绘画部分*/
    public void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        init();
        /*绘制椭圆*/
        canvas.drawPath(mPath, mPaint);
       /*绘制一个小球 参数分别是：圆心坐标，半径 ，所使用的画笔*/
        canvas.drawCircle(currentX, currentY, 15, pBall);
        if(stateS==1)
        {
            translateLR();
        }
    }
    /*初始化设置*/
    private void init()
    {
        //设置画笔的颜色
        /*初始化画笔，Paint.ANTI_ALIAS_FLAG使位图抗锯齿的标志*/
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        /*球的颜色填充为红色*/
        pBall.setColor(Color.RED);
        /*只绘制椭圆的轮廓，里面是空心的*/
        mPaint.setStyle(Paint.Style.STROKE);
        /*设置画笔粗细，单位是像素*/
        mPaint.setStrokeWidth(2);
        /*设置椭圆的颜色为黑色*/
        mPaint.setColor(Color.BLACK);
        mPath = new Path();
        /*定义与椭圆相切的矩形大小*/
        RectF rectF = new RectF(rectX, rectY, rectX+aLen, rectY+bLen);
        /*添加椭圆的绘制路径*/
        mPath.addOval(rectF,Path.Direction.CW);
    }

    /* 当 当前所获得的速度/力量在合理范围之内(比如速度在[5,10]之内合理)，小球的x和y符合椭圆方程，呈现效果就是将沿着椭圆进行运动
    * 如果当前速度/力量不再合理范围之内，将根据当前速度脱离椭圆轨道，向心或者离心*/

    /*作用：控制小球按照顺时针绕球往复运动*/
    public int translateLR()
    {
        if(this.currentX>=40)
        {
            /*无电机传入数据的情况*/
            this.currentX=(float)(offset+aLen/2*Math.cos(angleX*Math.PI /180));
            /*当前小球的横坐标对应传过来的值*/
            //this.currentX=offset+(powerX-(backLimit-frontLimit)/2)*rateX;
            /*求出当前小球横坐标所对应的角度*/
            this.tmpAngle= (float) Math.asin((powerX-(backLimit-frontLimit)/2)*rateX/aLen*2);
            /*当到达端点，即完成半程训练一次，cnt累计+1*/
            if(this.currentX==40||this.currentX==540)
                cnt++;
            /*根据cnt的奇偶性判断小球的运动方向*/
            if(cnt%2==1){
                /*在y轴上半轴运动*/
                /*算出该横坐标所对应的Y值，该值符合椭圆方程[在合理速度范围内的情况]*/
                this.tmpY=(float)(offsetYu-bLen/2*Math.sin(tmpAngle*Math.PI /180));
                /*将speedY进行映射处理，上半轴*/
                float tmpYu=offsetYu-(this.speedY-(MaxLimit-MinLimit)/2)*rateY;
                /*判断范围*/
                if(MinR<=this.speedY&&this.speedY<=MaxR)
                {   /*如果当前速度处于合理范围之内，Y坐标符合椭圆方程*/
                    //this.currentY=this.tmpY;
                    this.currentY=(float)(offsetYu-bLen/2*Math.sin(angleY*Math.PI /180));
                }else
                {
                    /*否则显示 根据传入的速度值映射后得到的结果*/
                    this.currentY=tmpYu;
                }
                /*在椭圆上*/
                //this.currentY=(float)(offsetYu-bLen/2*Math.sin(angleY*Math.PI /180));
                /*随机数决定y坐标*/
                //this.currentY= (float)Math.abs(Math.random()*1000%200);
            }else{
                /*在y轴下半轴运动*/
                 /*算出该横坐标所对应的Y值，该值符合椭圆方程[在合理速度范围内的情况]*/
                this.tmpY=(float)(offsetYd-bLen/2*Math.sin(tmpAngle*Math.PI /180));
                /*将speedY进行映射处理，下半轴*/
                float tmpYd=offsetYd-(this.speedY-(MaxLimit-MinLimit)/2)*rateY;
                /*随机数决定y坐标*/
                //this.currentY= (float)Math.abs(bLen+Math.random()*1000%200);
                /*判断范围*/
                if(MinR<=this.speedY&&this.speedY<=MaxR)
                {   /*如果当前速度处于合理范围之内，Y坐标符合椭圆方程*/
                    //this.currentY=this.tmpY;
                    this.currentY= (float)Math.abs(offsetYd-bLen/2*Math.sin(angleY*Math.PI /180));
                }else
                {
                    /*否则显示 根据传入的速度值映射后得到的结果*/
                    this.currentY=tmpYd;
                }
                /*如果速度在合理范围内，小球的xy坐标带入符合椭圆方程，下面是公式*/
                //this.currentY= (float)Math.abs(offsetYd-bLen/2*Math.sin(angleY*Math.PI /180));
            }
            /*打印实时坐标*/
            //Log.d(MyBallViewMuscle.ACTIVITY_TAG, "currentX:"+currentX+"  currentY:"+currentY);
            this.invalidate();
            angleX--;
            angleY++;
        }
        return cnt;
    }
}
