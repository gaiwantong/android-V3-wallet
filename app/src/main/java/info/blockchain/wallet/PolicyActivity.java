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
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.DeviceUtil;
import piuk.blockchain.android.R;

public class PolicyActivity extends Activity	{

    private WebView webview = null;
    private ProgressBar progressBar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setContentView(R.layout.activity_policy);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        webview = (WebView)findViewById(R.id.webView);
        webview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            return event.getAction() == MotionEvent.ACTION_UP;
            }
        });
        progressBar = (ProgressBar)findViewById(R.id.progressSpinner);

        String uri = this.getIntent().getStringExtra("uri");
        if(uri.contains(".pdf")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(uri));
            startActivity(intent);
            finish();
        }

        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setTextZoom(80);
        webview.setWebViewClient(new Browser());
        webview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webview.loadUrl(uri);

        progressBar.setVisibility(View.VISIBLE);
    }

    private class Browser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
//            webview.loadUrl("javascript:MyApp.resize(document.body.getBoundingClientRect().height)");
            progressBar.setVisibility(View.GONE);
            webview.setMinimumHeight(DeviceUtil.getInstance(PolicyActivity.this).getHeight());
        }

        @JavascriptInterface
        public void resize(final float height) {
            PolicyActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webview.setLayoutParams(new LinearLayout.LayoutParams(getResources().getDisplayMetrics().widthPixels, (int) (height * getResources().getDisplayMetrics().density)));
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).setIsBackgrounded(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppUtil.getInstance(this).setIsBackgrounded(true);
    }
}
