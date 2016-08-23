package info.blockchain.wallet.datamanagers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.graphics.Bitmap;

import info.blockchain.wallet.rxjava.RxUtil;

import rx.Observable;

public class ReceiveDataManager {

    /**
     * Generates a QR code in Bitmap format from a given URI to specified dimensions, wrapped in
     * an Observable. Will throw an error if the Bitmap is null.
     *
     * @param uri           A string to be encoded
     * @param dimensions    The dimensions of the QR code to be returned
     * @return              An Observable wrapping the generate Bitmap operation
     */
    public Observable<Bitmap> generateQrCode(String uri, int dimensions) {
        return generateQrCodeObservable(uri, dimensions)
                .compose(RxUtil.applySchedulers());
    }

    private Observable<Bitmap> generateQrCodeObservable(String uri, int dimensions) {
        return Observable.defer(() -> Observable.create(subscriber -> {
            Bitmap bitmap = null;
            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), dimensions);
            try {
                bitmap = qrCodeEncoder.encodeAsBitmap();
            } catch (WriterException e) {
                subscriber.onError(e);
            }

            if (bitmap == null) {
                subscriber.onError(new Throwable("Bitmap was null"));
            } else {
                subscriber.onNext(bitmap);
                subscriber.onCompleted();
            }
        }));
    }

}
