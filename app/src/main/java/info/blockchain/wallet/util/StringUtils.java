package info.blockchain.wallet.util;

import android.content.Context;
import android.support.annotation.StringRes;

public class StringUtils {

    private Context mContext;

    public StringUtils(Context context) {
        mContext = context;
    }

    public String getString(@StringRes int stringId) {
        return mContext.getString(stringId);
    }
}
