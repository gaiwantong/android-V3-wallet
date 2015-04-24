package info.blockchain.wallet;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

public class SelectedSpinner extends Spinner {

    private int lastSelected = 0;

    public SelectedSpinner(Context context) {
    	super(context);
    }

    public SelectedSpinner(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }

    public SelectedSpinner(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

    	if(this.lastSelected == this.getSelectedItemPosition() && getOnItemSelectedListener() != null) {
            getOnItemSelectedListener().onItemSelected(this, getSelectedView(), this.getSelectedItemPosition(), getSelectedItemId());
        }

        if(!changed) {
            lastSelected = this.getSelectedItemPosition();
        }

        super.onLayout(changed, left, top, right, bottom);
    } 
}
