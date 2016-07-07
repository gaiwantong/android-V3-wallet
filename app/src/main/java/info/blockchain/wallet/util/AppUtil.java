package info.blockchain.wallet.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.view.MotionEvent;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.ui.MainActivity;
import info.blockchain.wallet.ui.helpers.ToastCustom;

import java.io.File;
import java.security.Security;

import piuk.blockchain.android.R;

public class AppUtil {

    private static final String REGEX_UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private Context context = null;
    private AlertDialog alertDialog = null;
    private PrefsUtil prefs;
    private String receiveQRFileName;

    public AppUtil(Context context) {
        this.context = context;
        this.prefs = new PrefsUtil(context);
        this.receiveQRFileName = context.getExternalCacheDir() + File.separator + "qr.png";
    }

    public void clearCredentials() {
        PayloadFactory.getInstance().wipe();
        prefs.clear();
    }

    public void clearCredentialsAndRestart() {
        clearCredentials();
        restartApp();
    }

    public void restartApp() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void restartApp(String name, boolean value) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (name != null) {
            intent.putExtra(name, value);
        }
        context.startActivity(intent);
    }

    public String getReceiveQRFilename() {
        return receiveQRFileName;
    }

    public void deleteQR() {
        File file = new File(receiveQRFileName);
        if (file.exists()) {
            file.delete();
        }
    }

    public void setUpgradeReminder(long ts) {
        prefs.setValue(PrefsUtil.KEY_HD_UPGRADE_LAST_REMINDER, ts);
    }

    public boolean isNewlyCreated() {
        return prefs.getValue(PrefsUtil.KEY_NEWLY_CREATED_WALLET, false);
    }

    public void setNewlyCreated(boolean newlyCreated) {
        prefs.setValue(PrefsUtil.KEY_NEWLY_CREATED_WALLET, newlyCreated);
    }

    public boolean isSane() {
        String guid = prefs.getValue(PrefsUtil.KEY_GUID, "");

        if (!guid.matches(REGEX_UUID)) {
            return false;
        }

        String encryptedPassword = prefs.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");
        String pinID = prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");

        if (encryptedPassword.length() == 0 || pinID.length() == 0) {
            return false;
        }

        return true;
    }

    public boolean isCameraOpen() {
        Camera camera = null;

        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            return true;
        } finally {
            if (camera != null) {
                camera.release();
            }
        }

        return false;
    }

    public String getSharedKey() {
        return prefs.getValue(PrefsUtil.KEY_SHARED_KEY, "");
    }

    public void setSharedKey(String sharedKey) {
        prefs.setValue(PrefsUtil.KEY_SHARED_KEY, sharedKey);
    }

    //TODO - remove this
    public boolean isNotUpgraded() {
        return PayloadFactory.getInstance().get() != null && !PayloadFactory.getInstance().get().isUpgraded();
    }

    public void applyPRNGFixes() {
        try {
            PRNGFixes.apply();
        } catch (Exception e0) {
            //
            // some Android 4.0 devices throw an exception when PRNGFixes is re-applied
            // removing provider before apply() is a workaround
            //
            Security.removeProvider("LinuxPRNG");
            try {
                PRNGFixes.apply();
            } catch (Exception e1) {
                ToastCustom.makeText(context, context.getString(R.string.cannot_launch_app), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                AccessState.getInstance(context).logout();
            }
        }
    }

    public boolean detectObscuredWindow(MotionEvent event){
        //Detect if touch events are being obscured by hidden overlays - These could be used for tapjacking
        if ((!prefs.getValue("OVERLAY_TRUSTED",false)) && (event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0) {

            //Prevent multiple popups
            if(alertDialog != null)
                alertDialog.dismiss();

            alertDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.screen_overlay_warning)
                    .setMessage(R.string.screen_overlay_note)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            prefs.setValue("OVERLAY_TRUSTED", true);
                            dialog.dismiss();
                        }
                    }).setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            ((Activity) context).finish();
                        }
                    }).show();
                return true;//consume event
        }else {
            return false;
        }
    }
}
