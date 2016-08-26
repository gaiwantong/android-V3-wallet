package info.blockchain.wallet.view.customviews;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import piuk.blockchain.android.R;

/**
 * This class intercepts "reveal" events when the wrapped EditText's input type is "textPassword".
 * It warns users once that revealing their password allows clipboard access, and passes a touch
 * event if a user allows it.
 */
public class BlockchainInputLayout extends TextInputLayout {

    private ImageButton mToggle;
    // Shared across all instances
    private static boolean mPasswordWarningSeen = false;

    public BlockchainInputLayout(Context context) {
        this(context, null);
    }

    public BlockchainInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlockchainInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initListener();
    }

    private void initListener() {
        if (isPasswordVisibilityToggleEnabled()) {
            mToggle = (ImageButton) this.findViewById(R.id.text_input_password_toggle);
            mToggle.setOnTouchListener((v, event) -> {
                if (event != null && event.getAction() == MotionEvent.ACTION_UP) {
                    if (!mPasswordWarningSeen
                            && getEditText() != null
                            && getEditText().getTransformationMethod() != null) {

                        showCopyWarningDialog(mToggle);
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            });
        }
    }

    private void showCopyWarningDialog(View toggle) {
        mPasswordWarningSeen = true;

        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setMessage("DANGER WILL ROBINSON")
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> mPasswordWarningSeen = false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> toggle.performClick())
                .create()
                .show();
    }
}
