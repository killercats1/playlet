package com.killercats.playletmobile;

import android.content.Context;
import android.widget.ImageView;

/** An ImageView that is always 16:9, cropping the picture to fill it. */
public class AspectImageView extends ImageView {
    public AspectImageView(Context context) {
        super(context);
        setScaleType(ScaleType.CENTER_CROP);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width * 9 / 16);
    }
}
