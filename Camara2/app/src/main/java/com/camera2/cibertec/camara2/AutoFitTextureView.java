package com.camera2.cibertec.camara2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by jcollantes on 13/02/2018.
 */

public class AutoFitTextureView extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public int mWidth = 0;
    public int mHeight = 0;


    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        mWidth=width;
        mHeight=height;

        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
                //Log.i(TAG, "onMeasure: " + width + "aa" + width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                //obtener el tamaño el alto ideal
               /* height=width * 4 / 3; //que la altuna la tome del formato 4/3
                width=height * mRatioWidth / mRatioHeight;
                setMeasuredDimension(width,height);
                Log.i(TAG, "onMeasure: " + height * mRatioWidth / mRatioHeight + "bb" + height);*/
            }
        }

        /*int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        Log.d(TAG, "[onMeasure] Before transforming: " + width + "x" + height);

        int rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getRotation();
        boolean isInHorizontal = Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation;

        int newWidth;
        int newHeight;

        Log.d(TAG, "[onMeasure] Get measured dimensions: " + getMeasuredWidth() + "x" + getMeasuredHeight());

        if (isInHorizontal) {
            newHeight = getMeasuredHeight();
            newWidth = getMeasuredHeight();
            if (1 == newWidth){

            }else{
                newWidth = (int) (newHeight * (9/16));
            }

        } else {
            newWidth = getMeasuredWidth();
            newHeight = getMeasuredWidth();
            //if (mAspectRatioOneOne) newHeight = getMeasuredWidth();
            //else newHeight = (int) (newWidth * mAspectRatio);
            if (1 == newHeight){

            }else{
                newHeight = (int) (newWidth * (4/3));
            }
        }

        setMeasuredDimension(newWidth, newHeight);
        Log.d(TAG, "[onMeasure] After transforming: " + getMeasuredWidth() + "x" + getMeasuredHeight());*/

    }

}
