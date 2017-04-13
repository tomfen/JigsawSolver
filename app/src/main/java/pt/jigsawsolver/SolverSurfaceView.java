package pt.jigsawsolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * Created by dominikaczarnecka on 13.04.2017.
 */

public class SolverSurfaceView extends SurfaceView {

    private Bitmap image;

    public SolverSurfaceView(Context context) {
        super(context);
    }

    public SolverSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SolverSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setImage(Bitmap image){
        this.image = image;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(image != null) {
            canvas.drawBitmap(image, 0, 0, null);
        }else{
            super.onDraw(canvas);
        }
    }
}
