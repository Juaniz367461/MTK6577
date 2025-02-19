
package com.mediatek.ngin3d.demo;

import android.os.Bundle;

import android.util.FloatMath;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Glo3D;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.android.StageActivity;

public class Glo3DScaleRotationDemo extends StageActivity {

    private Container mScenario;
    private GestureDetector mGestureDetector;
    private float mYaw;
    private float mRoll;
    private float mPitch;
    private int mMode = 0;
    private float mOldDist;
    private Scale mCurrentScale;
    private int mWidth;
    private int mHight;
    private Image mUpbutton;
    private Image mDownbutton;

    public class MyGestureDetector extends SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            mPitch = mScenario.getRotation().getEulerAngles()[0];
            mYaw = mScenario.getRotation().getEulerAngles()[1];
            mRoll = mScenario.getRotation().getEulerAngles()[2];

            if (e1.getAction() != MotionEvent.ACTION_POINTER_DOWN
                    || e1.getAction() != MotionEvent.ACTION_POINTER_UP) {
                if (distanceX < 10 || distanceX > 10) {
                    mRoll = mRoll + distanceX / 5;
                }

                if (distanceY > 10 || distanceY < 10) {
                    mPitch = mPitch - distanceY / 5;
                }
            }

            mScenario.setRotation(new Rotation(mPitch, mYaw, mRoll));
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent upEvent) {

            return true;
        }

    }

    private void zoom(float f) {
        mYaw = (mYaw + 0.1f) * f;
        mScenario.setRotation(new Rotation(mPitch, mYaw, mRoll));
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    public boolean onTouchEvent(MotionEvent event) {

        mYaw = mScenario.getRotation().getEulerAngles()[1];

        if (mUpbutton.hitTest(new Point(event.getX(), event.getY())) != null) {
            mCurrentScale = mScenario.getScale();
            if (mCurrentScale.x <= 30) {
                mCurrentScale.x *= 1.05;
                mCurrentScale.y *= 1.05;
                mCurrentScale.z *= 1.05;
            }

            mScenario.setScale(mCurrentScale);
        } else if (mDownbutton.hitTest(new Point(event.getX(), event.getY())) != null) {
            mCurrentScale = mScenario.getScale();
            if (mCurrentScale.x >= 10) {
                mCurrentScale.x *= 0.95;
                mCurrentScale.y *= 0.95;
                mCurrentScale.z *= 0.95;
            }
            mScenario.setScale(mCurrentScale);
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                mMode = 1;
                break;
            case MotionEvent.ACTION_UP:
                mMode = 0;

                break;
            case MotionEvent.ACTION_POINTER_UP:
                mMode -= 1;

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDist = spacing(event);
                mMode += 1;

                break;
            case MotionEvent.ACTION_MOVE:

                if (mMode >= 2) {
                    float newDist = spacing(event);
                    if (newDist > mOldDist + 1) {

                        zoom(newDist / mOldDist);
                        mOldDist = newDist;
                    } else if (newDist < mOldDist - 1) {

                        zoom(newDist / mOldDist);
                        mOldDist = newDist;
                    }
                    return true;
                }

        }

        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        return true;
    }

    private void getDisplaySize() {
        Display display = getWindowManager().getDefaultDisplay();
        mWidth = display.getWidth(); // deprecated
        mHight = display.getHeight(); // deprecated

    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getDisplaySize();
        mUpbutton = Image.createFromResource(getResources(), R.drawable.up_button);
        mDownbutton = Image.createFromResource(getResources(), R.drawable.down_button);

        final Container button = new Container();
        button.add(mUpbutton, mDownbutton);
        mUpbutton.setPosition(new Point(100, 700, 0));
        mDownbutton.setPosition(new Point(400, 700, 0));
        mGestureDetector = new GestureDetector(new MyGestureDetector());
        mScenario = new Container();

        final Glo3D landscape = Glo3D.createFromAsset("landscape.glo");
        mScenario.add(landscape);
        mScenario.setPosition(new Point(mWidth / 2, mHight / 2 + 50, 0));
        mScenario.setScale(new Scale(10, -10, 10));
        mStage.add(mScenario, button);
    }
}
