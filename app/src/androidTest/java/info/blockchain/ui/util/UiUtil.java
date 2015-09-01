package info.blockchain.ui.util;

import android.content.Context;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.robotium.solo.Solo;

import java.util.ArrayList;

import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.PrefsUtil;
import piuk.blockchain.android.R;

public class UiUtil {

    private static Context context = null;

    private static UiUtil instance = null;

    private UiUtil() { ; }

    public static UiUtil getInstance(Context ctx) {

        if(instance == null) {

            context = ctx;

            instance = new UiUtil();
        }

        return instance;
    }

    public void enterPin(Solo solo, String pin) {

        if(solo.searchText(context.getString(R.string.pin_entry)) || solo.searchText(context.getString(R.string.confirm_pin))|| solo.searchText(context.getString(R.string.create_pin))) {
            ArrayList<Integer> pinSequence = new ArrayList<>();
            pinSequence.add(Integer.parseInt(pin.substring(0, 1)));
            pinSequence.add(Integer.parseInt(pin.substring(1, 2)));
            pinSequence.add(Integer.parseInt(pin.substring(2, 3)));
            pinSequence.add(Integer.parseInt(pin.substring(3, 4)));

            for(int i : pinSequence){

                switch (i){
                    case 0:solo.clickOnView(solo.getView(R.id.button0));break;
                    case 1:solo.clickOnView(solo.getView(R.id.button1));break;
                    case 2:solo.clickOnView(solo.getView(R.id.button2));break;
                    case 3:solo.clickOnView(solo.getView(R.id.button3));break;
                    case 4:solo.clickOnView(solo.getView(R.id.button4));break;
                    case 5:solo.clickOnView(solo.getView(R.id.button5));break;
                    case 6:solo.clickOnView(solo.getView(R.id.button6));break;
                    case 7:solo.clickOnView(solo.getView(R.id.button7));break;
                    case 8:solo.clickOnView(solo.getView(R.id.button8));break;
                    case 9:solo.clickOnView(solo.getView(R.id.button9));break;
                }
                try{solo.sleep(500);}catch (Exception e){}
            }
            solo.waitForDialogToClose();
            try{solo.sleep(2000);}catch (Exception e){}
        }
    }

    public void soundAlert(){
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager!=null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(context, R.raw.alert);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                    mp.release();
                }

            });
            mp.start();
        }
    }

    public void wipeWallet(){
        HD_WalletFactory.getInstance(context).set(null);
        PayloadFactory.getInstance().wipe();
        PrefsUtil.getInstance(context).clear();
    }

    public void openNavigationDrawer(Solo solo) {
        Point deviceSize = new Point();
        solo.getCurrentActivity().getWindowManager().getDefaultDisplay().getSize(deviceSize);

        int screenWidth = deviceSize.x;
        int screenHeight = deviceSize.y;
        int fromX = 0;
        int toX = screenWidth / 2;
        int fromY = screenHeight / 2;
        int toY = fromY;

        solo.drag(fromX, toX, fromY, toY, 1);
    }
}
