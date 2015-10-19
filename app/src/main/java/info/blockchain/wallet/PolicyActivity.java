package info.blockchain.wallet;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import info.blockchain.wallet.util.AppUtil;

public class PolicyActivity extends Activity	{

    private WebView webview = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        webview = new WebView(this);
        webview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            return event.getAction() == MotionEvent.ACTION_UP;
            }
        });

        String uri = this.getIntent().getStringExtra("uri");
        if(uri.contains(".pdf")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(uri));
            startActivity(intent);
            finish();
        }else {

            webview.getSettings().setTextZoom(60);
            webview.loadUrl(uri);
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }

            });

            this.setContentView(webview);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(this).stopLockTimer();

        if(AppUtil.getInstance(this).isTimedOut() && !AppUtil.getInstance(this).isLocked()) {
            Intent i = new Intent(this, PinEntryActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(i);
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        AppUtil.getInstance(this).updateUserInteractionTime();
    }

    @Override
    protected void onPause() {
        AppUtil.getInstance(this).startLockTimer();
        super.onPause();
    }
}
