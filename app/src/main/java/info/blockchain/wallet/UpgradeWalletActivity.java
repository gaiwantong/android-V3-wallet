package info.blockchain.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;

import java.io.IOException;

import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import piuk.blockchain.android.R;

public class UpgradeWalletActivity extends Activity {

    private AlertDialog alertDialog = null;
    private ViewPager mViewPager = null;
    private CustomPagerAdapter mCustomPagerAdapter = null;

    private TextSwitcher pageHeader = null;

    private TextView pageBox0 = null;
    private TextView pageBox1 = null;
    private TextView pageBox2 = null;

    private TextView heading = null;
    private TextView info = null;
    private ProgressBar progressBar = null;
    private TextView confirmCancel = null;
    private TextView confirmUpgrade = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upgrade_wallet);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        pageHeader = (TextSwitcher) findViewById(R.id.upgrade_page_header);
        pageHeader.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                TextView myText = new TextView(UpgradeWalletActivity.this);
                myText.setGravity(Gravity.CENTER);
                myText.setTextSize(14);
                myText.setTextColor(Color.WHITE);
                return myText;
            }
        });
        pageHeader.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.abc_fade_in));
        pageHeader.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.abc_fade_out));
        pageHeader.setText(getResources().getString(R.string.upgrade_page_1));

        mCustomPagerAdapter = new CustomPagerAdapter(this);
        mViewPager.setAdapter(mCustomPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setSelectedPage(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        pageBox0 = (TextView)findViewById(R.id.pageBox0);
        pageBox1 = (TextView)findViewById(R.id.pageBox1);
        pageBox2 = (TextView)findViewById(R.id.pageBox2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(UpgradeWalletActivity.this).updatePinEntryTime();
    }

    public void upgradeClicked(View view) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_upgrade_wallet_confirm, null);
        dialogBuilder.setView(dialogView);

        alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);

        heading = (TextView)dialogView.findViewById(R.id.heading_tv);
        info = (TextView)dialogView.findViewById(R.id.upgrade_info_tv);
        progressBar = (ProgressBar)dialogView.findViewById(R.id.progressBar3);

        confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
        confirmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();
            }
        });

        confirmUpgrade = (TextView) dialogView.findViewById(R.id.confirm_upgrade);
        confirmUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onUpgradeStart();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        try {

                            HDPayloadBridge.getInstance(getApplicationContext()).init(PayloadFactory.getInstance().getTempPassword());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        } catch (AddressFormatException e) {
                            e.printStackTrace();
                        } catch (MnemonicException.MnemonicLengthException e) {
                            e.printStackTrace();
                        } catch (MnemonicException.MnemonicChecksumException e) {
                            e.printStackTrace();
                        } catch (MnemonicException.MnemonicWordException e) {
                            e.printStackTrace();
                        }

                        onUpgradeCompleted();
                        Looper.loop();

                    }
                }).start();
            }
        });

        alertDialog.show();
    }

    private void onUpgradeStart(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heading.setText(getString(R.string.upgrading));
                info.setText(getString(R.string.upgrading_started_info));
                progressBar.setVisibility(View.VISIBLE);
                confirmCancel.setVisibility(View.INVISIBLE);
                confirmUpgrade.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void onUpgradeCompleted(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heading.setText(getString(R.string.upgrade_success_heading));
                info.setText(getString(R.string.upgrade_success_info));
                progressBar.setVisibility(View.GONE);
                confirmUpgrade.setVisibility(View.VISIBLE);
                confirmUpgrade.setText(getString(R.string.CLOSE));
                confirmUpgrade.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();

                        AppUtil.getInstance(UpgradeWalletActivity.this).restartApp();
                    }
                });
            }
        });
    }

    public void askLaterClicked(View view) {
        AppUtil.getInstance(this).setUpgradeReminder(System.currentTimeMillis());
        finish();
    }

    class CustomPagerAdapter extends PagerAdapter {

        Context mContext;
        LayoutInflater mLayoutInflater;
        int[] mResources = {
                R.drawable.upgrade_myaccounts_hilite,
                R.drawable.upgrade_backup_hilite,
                R.drawable.upgrade_tx_list_hilite
        };

        public CustomPagerAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mResources.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((LinearLayout) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = mLayoutInflater.inflate(R.layout.activity_upgrade_wallet_pager_item, container, false);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.imageView);
            imageView.setImageResource(mResources[position]);

            container.addView(itemView);

            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }

    private void setSelectedPage(int position){

        pageBox0.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_dark_blue));
        pageBox1.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_dark_blue));
        pageBox2.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_dark_blue));

        switch (position){
            case 0:
                pageHeader.setText(getResources().getString(R.string.upgrade_page_1));
                pageBox0.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_upgrade_wallet_blue));break;
            case 1:
                pageHeader.setText(getResources().getString(R.string.upgrade_page_2));
                pageBox1.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_upgrade_wallet_blue));break;
            case 2:
                pageHeader.setText(getResources().getString(R.string.upgrade_page_3));
                pageBox2.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_upgrade_wallet_blue));break;
        }
    }
}