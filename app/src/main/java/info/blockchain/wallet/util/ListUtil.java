package info.blockchain.wallet.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public class ListUtil {

    public static <E> void addAllIfNotNull(@NonNull List<E> list,
                                           @Nullable Collection<? extends E> collection) {
        if (collection != null) {
            list.addAll(collection);
        }
    }

}
