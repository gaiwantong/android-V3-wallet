package info.blockchain.wallet.view;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.annotations.Thunk;

import static info.blockchain.wallet.view.TransactionDetailActivity.KEY_TRANSACTION_URL;

public class TransactionDetailWebViewActivity extends BaseAuthActivity {

    private WebView mWebView;
    @Thunk ProgressBar mProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail_webview);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.transaction_detail_tab_title));
        setSupportActionBar(toolbar);

        mWebView = (WebView) findViewById(R.id.webView);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mWebView.setWebChromeClient(new DetailsWebViewClient());

        if (getIntent().hasExtra(KEY_TRANSACTION_URL)) {
            mWebView.loadUrl(getIntent().getStringExtra(KEY_TRANSACTION_URL));
        } else {
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, call super
        return super.onKeyDown(keyCode, event);
    }

    private class DetailsWebViewClient extends WebChromeClient {

        DetailsWebViewClient() {
            super();
        }

        @Override
        public void onProgressChanged(WebView view, int progress) {
            if (progress < 100 && mProgressBar.getVisibility() == ProgressBar.GONE) {
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
            }

            mProgressBar.setProgress(progress);
            if (progress == 100) {
                mProgressBar.setVisibility(ProgressBar.GONE);
            }
        }
    }
}
