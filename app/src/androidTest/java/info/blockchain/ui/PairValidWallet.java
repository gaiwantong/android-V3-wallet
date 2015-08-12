package info.blockchain.ui;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.robotium.solo.Solo;

import java.util.ArrayList;

import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.PinEntryActivity;
import piuk.blockchain.android.R;

public class PairValidWallet extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    public PairValidWallet() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        //Press back button twice to exit app
        solo.goBack();
        solo.goBack();
    }

    public void testPairValidWallet()  throws AssertionError{

        solo.clickOnView(solo.getView(R.id.login));
        solo.clickOnView(solo.getView(R.id.command_manual));
        try{solo.sleep(1000);}catch (Exception e){}
        solo.enterText((EditText)solo.getView(R.id.wallet_id), solo.getString(R.string.qa_test_guid1));
        solo.enterText((EditText) solo.getView(R.id.wallet_pass), solo.getString(R.string.qa_test_password1));

        solo.clickOnText(solo.getString(R.string.dialog_continue));

        //Confirm email needs to be done manually by tester - so we'll make a sound to alert tester
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager!=null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(getActivity(), R.raw.alert);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                    mp.release();
                }

            });
            mp.start();
        }

        solo.waitForActivity(PinEntryActivity.class, 120000);
        try{solo.sleep(2000);}catch (Exception e){}
        enterPin();//create
        enterPin();//confirm
        try{solo.sleep(4000);}catch (Exception e){}
    }

    public void enterPin() throws AssertionError{

        String pin = solo.getString(R.string.qa_test_pin1);

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
    }
}
