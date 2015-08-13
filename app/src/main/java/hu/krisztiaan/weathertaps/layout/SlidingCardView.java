package hu.krisztiaan.weathertaps.layout;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

import com.pnikosis.materialishprogress.ProgressWheel;

public class SlidingCardView extends CardView {

    private float yFraction = 0;
    private ProgressWheel mProgressWheel;

    public SlidingCardView(Context context) {
        super(context);
        mProgressWheel = new ProgressWheel(context);
    }

    public SlidingCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private ViewTreeObserver.OnPreDrawListener preDrawListener = null;

    public void setYFraction(float fraction) {

        this.yFraction = fraction;

        if (getHeight() == 0) {
            if (preDrawListener == null) {
                preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                        setYFraction(yFraction);
                        return true;
                    }
                };
                getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            }
            return;
        }

        float translationY = getHeight() * fraction;
        setTranslationY(translationY);
    }

    public float getYFraction() {
        return this.yFraction;
    }
}