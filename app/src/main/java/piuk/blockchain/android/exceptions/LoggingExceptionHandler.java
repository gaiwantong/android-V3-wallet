package piuk.blockchain.android.exceptions;

import com.google.common.hash.Hashing;

import android.os.Build;
import android.util.Log;

import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.WebUtil;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.inject.Inject;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.di.Injector;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by adambennett on 10/08/2016.
 */

public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String URL = "https://blockchain.info/exception_log?device=Android&message=";
    private static final String LINEBREAK = "\n";
    private static final String OPENING_TAG = "<pre>";
    private static final String ENCLOSING_TAG = "</pre>";
    private final Thread.UncaughtExceptionHandler mRootHandler;
    @SuppressWarnings("WeakerAccess")
    @Inject protected PrefsUtil mPrefsUtil;

    public LoggingExceptionHandler() {
        Injector.getInstance().getAppComponent().inject(this);
        mRootHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {

        sendException(throwable)
                .subscribeOn(Schedulers.io())
                .subscribe(s -> {
                    Log.d(LoggingExceptionHandler.class.getSimpleName(), "uncaughtException: ");
                }, Throwable::printStackTrace);

        // Re-throw the exception so that the system can fail as it normally would
        mRootHandler.uncaughtException(thread, throwable);
    }

    private String getMessageString(Throwable throwable) throws UnsupportedEncodingException {

        String message = OPENING_TAG
                + "App Version: " + BuildConfig.VERSION_NAME + " " + BuildConfig.VERSION_CODE
                + LINEBREAK + "System Name: " + Build.VERSION.SDK_INT
                + LINEBREAK + "Device: " + Build.MANUFACTURER + " " + Build.MODEL
                + LINEBREAK + "Language: " + Locale.getDefault().getDisplayLanguage()
                + LINEBREAK + "GUID Hash: " + getStringHash(mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""))
                + LINEBREAK + "Reason: " + throwable.getLocalizedMessage()
                + LINEBREAK
                + LINEBREAK + "Stacktrace: " + ExceptionUtils.getStackTrace(throwable)
                + ENCLOSING_TAG;

        return URLEncoder.encode(message, StandardCharsets.UTF_8.displayName());
    }

    private String getStringHash(String string) {
        return Hashing.sha256().hashString(string, Charset.defaultCharset()).toString();
    }

    private Observable<String> sendException(Throwable throwable) {
        return Observable.fromCallable(() -> WebUtil.getInstance().getURL(URL + getMessageString(throwable)));
    }
}
