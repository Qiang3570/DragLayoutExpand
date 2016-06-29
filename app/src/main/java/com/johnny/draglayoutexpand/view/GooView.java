package com.johnny.draglayoutexpand.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.johnny.draglayoutexpand.utils.GeometryUtil;
import com.johnny.draglayoutexpand.utils.Utils;

/**
 * This view should be added to WindowManager, so we can drag it to anywhere.
 */
public class GooView extends View {

    public interface OnDisappearListener {
        void onDisappear(PointF mDragCenter);
        void onReset(boolean isOutOfRange);
    }

    protected static final String TAG = "TAG";

    private PointF mInitCenter;
    private PointF mDragCenter;
    private PointF mStickCenter;
    float dragCircleRadius = 0;
    float stickCircleRadius = 0;
    float stickCircleMinRadius = 0;
    float stickCircleTempRadius = stickCircleRadius;
    float farest = 0;
    String text = "";

    private Paint mPaintRed;
    private Paint mTextPaint;
    private ValueAnimator mAnim;
    private boolean isOutOfRange = false;
    private boolean isDisappear = false;

    private OnDisappearListener mListener;
    private Rect rect;
    private int mStatusBarHeight;

    private float resetDistance;


    public GooView(Context context) {
        super(context);
        rect = new Rect(0, 0, 50, 50);
        stickCircleRadius = Utils.dip2Dimension(10.0f, context);
        dragCircleRadius = Utils.dip2Dimension(10.0f, context);
        stickCircleMinRadius = Utils.dip2Dimension(3.0f, context);
        farest = Utils.dip2Dimension(80.0f, context);
        resetDistance = Utils.dip2Dimension(40.0f, getContext());
        mPaintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintRed.setColor(Color.RED);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(dragCircleRadius * 1.2f);
    }

    /**
     * 设置固定圆的半径
     * @param r
     */
    public void setDargCircleRadius(float r){
        dragCircleRadius = r;
    }

    /**
     * 设置拖拽圆的半径
     * @param r
     */
    public void setStickCircleRadius(float r){
        stickCircleRadius = r;
    }

    /**
     * 设置数字
     * @param num
     */
    public void setNumber(int num){
        text = String.valueOf(num);
    }

    /**
     * 初始化圆的圆心坐标
     * @param x
     * @param y
     */
    public void initCenter(float x, float y){
        mDragCenter = new PointF(x, y);
        mStickCenter = new PointF(x, y);
        mInitCenter = new PointF(x, y);
        invalidate();
    }

    /**
     * 更新拖拽圆的圆心坐标，重绘View
     * @param x
     * @param y
     */
    private void updateDragCenter(float x, float y) {
        this.mDragCenter.x = x;
        this.mDragCenter.y = y;
        invalidate();
    }

    /**
     * 通过绘制Path构建一个ShapeDrawable，用来绘制到画布Canvas上
     * @return
     */
    private ShapeDrawable drawGooView() {
        Path path = new Path();
        float distance = (float) GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);/*根据当前两圆圆心的距离计算出固定圆的半径*/
        stickCircleTempRadius = getCurrentRadius(distance);
        float xDiff = mStickCenter.x - mDragCenter.x;/*计算出经过两圆圆心连线的垂线的dragLineK（对边比临边）。求出四个交点坐标*/
        Double dragLineK = null;
        if(xDiff != 0){
            dragLineK = (double) ((mStickCenter.y - mDragCenter.y) / xDiff);
        }
        /*分别获得经过两圆圆心连线的垂线与圆的交点（两条垂线平行，所以dragLineK相等）。*/
        PointF[] dragPoints = GeometryUtil.getIntersectionPoints(mDragCenter, dragCircleRadius, dragLineK);
        PointF[] stickPoints = GeometryUtil.getIntersectionPoints(mStickCenter, stickCircleTempRadius, dragLineK);
        //*以两圆连线的0.618处作为 贝塞尔曲线 的控制点。（选一个中间点附近的控制点）*/
        PointF pointByPercent = GeometryUtil.getPointByPercent(mDragCenter, mStickCenter, 0.618f);
        /*绘制两圆连接,此处参见示意图{@link https://github.com/PoplarTang/DragGooView*/
        path.moveTo((float)stickPoints[0].x, (float)stickPoints[0].y);
        path.quadTo((float)pointByPercent.x, (float)pointByPercent.y,
                (float)dragPoints[0].x, (float)dragPoints[0].y);
        path.lineTo((float)dragPoints[1].x, (float)dragPoints[1].y);
        path.quadTo((float)pointByPercent.x, (float)pointByPercent.y,
                (float)stickPoints[1].x, (float)stickPoints[1].y);
        path.close();
        //将四个交点画到屏幕上
//		path.addCircle((float)dragPoints[0].x, (float)dragPoints[0].y, 5, Direction.CW);
//		path.addCircle((float)dragPoints[1].x, (float)dragPoints[1].y, 5, Direction.CW);
//		path.addCircle((float)stickPoints[0].x, (float)stickPoints[0].y, 5, Direction.CW);
//		path.addCircle((float)stickPoints[1].x, (float)stickPoints[1].y, 5, Direction.CW);
        /*构建ShapeDrawable*/
        ShapeDrawable shapeDrawable = new ShapeDrawable(new PathShape(path, 50f, 50f));
        shapeDrawable.getPaint().setColor(Color.RED);
        return shapeDrawable;
    }

    /**
     * 根据距离获得当前固定圆的半径
     * @param distance
     * @return
     */
    private float getCurrentRadius(float distance) {
        distance = Math.min(distance, farest);
        float fraction = 0.2f + 0.8f * distance / farest;
        float evaluateValue = (float) GeometryUtil.evaluateValue(fraction, stickCircleRadius, stickCircleMinRadius);
        return evaluateValue;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(isAnimRunning()){
            return false;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionMasked = MotionEventCompat.getActionMasked(event);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:{
                if(isAnimRunning()){
                    return false;
                }
                isDisappear = false;
                isOutOfRange = false;
                updateDragCenter(event.getRawX() , event.getRawY());
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                /*如果两圆间距大于最大距离farest，执行拖拽结束动画*/
                PointF p0 = new PointF(mDragCenter.x, mDragCenter.y);
                PointF p1 = new PointF(mStickCenter.x, mStickCenter.y);
                if (GeometryUtil.getDistanceBetween2Points(p0, p1) > farest) {
                    isOutOfRange = true;
                    updateDragCenter(event.getRawX(), event.getRawY());
                    return false;
                }
                updateDragCenter(event.getRawX() , event.getRawY());
                break;
            }
            case MotionEvent.ACTION_UP:{
                handleActionUp();
                break;
            }
            default:{
                isOutOfRange = false;
                break;
            }
        }
        return true;
    }

    private boolean isAnimRunning() {
        if(mAnim != null && mAnim.isRunning()){
            return true;
        }
        return false;
    }

    /**
     * 清除
     */
    private void disappeared() {
        isDisappear = true;
        invalidate();
        if(mListener != null){
            mListener.onDisappear(mDragCenter);
        }
    }

    private void handleActionUp() {
        if(isOutOfRange){
            /*When user drag it back, we should call onReset().*/
            if(GeometryUtil.getDistanceBetween2Points(mDragCenter, mInitCenter) < resetDistance){
                if(mListener != null)
                    mListener.onReset(isOutOfRange);
                return;
            }
            /*Otherwise*/
            disappeared();
        }else {
            /*手指抬起时，弹回动画*/
            mAnim = ValueAnimator.ofFloat(1.0f);
            mAnim.setInterpolator(new OvershootInterpolator(4.0f));
            final PointF startPoint = new PointF(mDragCenter.x, mDragCenter.y);
            final PointF endPoint = new PointF(mStickCenter.x, mStickCenter.y);
            mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = animation.getAnimatedFraction();
                    PointF pointByPercent = GeometryUtil.getPointByPercent(startPoint, endPoint, fraction);
                    updateDragCenter((float)pointByPercent.x, (float)pointByPercent.y);
                }
            });
            mAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if(mListener != null)
                        mListener.onReset(isOutOfRange);
                }
            });
            if(GeometryUtil.getDistanceBetween2Points(startPoint,endPoint) < 10)	{
                mAnim.setDuration(10);
            }else {
                mAnim.setDuration(500);
            }
            mAnim.start();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(0, -mStatusBarHeight);/*去除状态栏高度偏差*/
        if(!isDisappear){
            if(!isOutOfRange){
                /*画两圆连接处*/
                ShapeDrawable drawGooView = drawGooView();
                drawGooView.setBounds(rect);
                drawGooView.draw(canvas);
                /*画固定圆*/
                canvas.drawCircle(mStickCenter.x, mStickCenter.y, stickCircleTempRadius, mPaintRed);
            }
            /*画拖拽圆*/
            canvas.drawCircle(mDragCenter.x , mDragCenter.y, dragCircleRadius, mPaintRed);
            /*画数字*/
            canvas.drawText(text, mDragCenter.x , mDragCenter.y + dragCircleRadius /2f, mTextPaint);
        }
        canvas.restore();
    }

    public OnDisappearListener getOnDisappearListener() {
        return mListener;
    }

    public void setOnDisappearListener(OnDisappearListener mListener) {
        this.mListener = mListener;
    }

    public void setStatusBarHeight(int statusBarHeight) {
        this.mStatusBarHeight = statusBarHeight;
    }
}