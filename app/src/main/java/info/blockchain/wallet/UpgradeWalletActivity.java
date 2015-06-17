package info.blockchain.wallet;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import info.blockchain.wallet.util.AppUtil;

public class UpgradeWalletActivity extends Activity {

    private ViewPager mViewPager = null;
    private CustomPagerAdapter mCustomPagerAdapter = null;

    private TextView pageHeader = null;

    private TextView pageBox0 = null;
    private TextView pageBox1 = null;
    private TextView pageBox2 = null;
    private TextView pageBox3 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upgrade_wallet);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        pageHeader = (TextView) findViewById(R.id.upgrade_page_header);

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
        pageBox3 = (TextView)findViewById(R.id.pageBox3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(UpgradeWalletActivity.this).updatePinEntryTime();
    }

    public void upgradeClicked(View view) {
    }

    public void askLaterClicked(View view) {
    }

    class CustomPagerAdapter extends PagerAdapter {

        Context mContext;
        LayoutInflater mLayoutInflater;
        int[] mResources = {
                R.drawable.icon_accounthd,
                R.drawable.icon_imported,
                R.drawable.icon_send,
                R.drawable.icon_logout
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
        pageBox3.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_dark_blue));

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
            case 3:
                pageHeader.setText(getResources().getString(R.string.upgrade_page_4));
                pageBox3.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_upgrade_wallet_blue));break;
        }
    }
}