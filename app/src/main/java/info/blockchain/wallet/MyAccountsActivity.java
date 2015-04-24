package info.blockchain.wallet;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import org.apache.commons.codec.DecoderException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

public class MyAccountsActivity extends Activity {

	LinearLayoutManager layoutManager = null;
	RecyclerView mRecyclerView = null;
	private List<Account> accounts = null;
	TextView myAccountsHeader;
	int minHeaderTranslation;
	public int toolbarHeight;

	ImageView backNav;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_my_accounts);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		backNav = (ImageView)findViewById(R.id.back_nav);
		backNav.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		myAccountsHeader = (TextView)findViewById(R.id.my_accounts_heading);
		minHeaderTranslation = myAccountsHeader.getHeight();

		mRecyclerView = (RecyclerView)findViewById(R.id.accountsList);
		layoutManager = new LinearLayoutManager(this);
		mRecyclerView.setLayoutManager(layoutManager);

		ArrayList<MyAccountItem> accountItems = new ArrayList<>();
		accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();

		toolbarHeight = (int)getResources().getDimension(R.dimen.action_bar_height)+35;

		int index = 0;
		for(Account item : accounts){
			accountItems.add(new MyAccountItem(item.getLabel(),displayBalance(index)));
			index++;
		}

		MyAccountsAdapter accountsAdapter = new MyAccountsAdapter(accountItems);
		mRecyclerView.setAdapter(accountsAdapter);

		mRecyclerView.addOnItemTouchListener(
				new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {

					//works
//					private int mOriginalHeight = 0;
//					private boolean mIsViewExpanded = false;
//					private int qrSize = 260;
					//works

					private int originalHeight = 0;
					private boolean mIsViewExpanded = false;

					@Override
					public void onItemClick(final View view, int position) {

						final ImageView qrTest = (ImageView)view.findViewById(R.id.qrr);

						//QR receiving
						String currentSelectedAddress = null;
						ReceiveAddress currentSelectedReceiveAddress = null;
						try {
							currentSelectedReceiveAddress = HDPayloadBridge.getInstance(MyAccountsActivity.this).getReceiveAddress(position);
							currentSelectedAddress = currentSelectedReceiveAddress.getAddress();
						} catch (DecoderException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (MnemonicException.MnemonicWordException e) {
							e.printStackTrace();
						} catch (MnemonicException.MnemonicChecksumException e) {
							e.printStackTrace();
						} catch (MnemonicException.MnemonicLengthException e) {
							e.printStackTrace();
						} catch (AddressFormatException e) {
							e.printStackTrace();
						}

						qrTest.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));

						// If the originalHeight is 0 then find the height of the View being used
						// This would be the height of the cardview
						if (originalHeight == 0) {
							originalHeight = view.getHeight();
						}

						// Declare a ValueAnimator object
						ValueAnimator valueAnimator;
						if (!mIsViewExpanded) {
							qrTest.setVisibility(View.VISIBLE);
							qrTest.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down));
							qrTest.setEnabled(true);
							mIsViewExpanded = true;
							valueAnimator = ValueAnimator.ofInt(originalHeight, originalHeight + qrTest.getHeight()); // These values in this method can be changed to expand however much you like
						} else {
							mIsViewExpanded = false;
							valueAnimator = ValueAnimator.ofInt(originalHeight + qrTest.getHeight(), originalHeight);

							Animation a = new AlphaAnimation(1.00f, 0.00f); // Fade out

							a.setDuration(200);
							// Set a listener to the animation and configure onAnimationEnd
							a.setAnimationListener(new Animation.AnimationListener() {
								@Override
								public void onAnimationStart(Animation animation) {

								}

								@Override
								public void onAnimationEnd(Animation animation) {
									qrTest.setVisibility(View.INVISIBLE);
									qrTest.setEnabled(false);
								}

								@Override
								public void onAnimationRepeat(Animation animation) {

								}
							});

							// Set the animation on the custom view
							qrTest.startAnimation(a);
						}
						valueAnimator.setDuration(200);
						valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
						valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							public void onAnimationUpdate(ValueAnimator animation) {
								Integer value = (Integer) animation.getAnimatedValue();
								view.getLayoutParams().height = value.intValue();
								view.requestLayout();
							}
						});


						valueAnimator.start();

					}
				})
		);

		mRecyclerView.setOnScrollListener(new CollapseActionbarScrollListener() {
			@Override
			public void onMoved(int distance) {
				myAccountsHeader.setTranslationY(-distance);
			}
		});
	}

	private String displayBalance(int index) {

		String address = HDPayloadBridge.getInstance(this).account2Xpub(index);
		Long amount = MultiAddrFactory.getInstance().getXpubAmounts().get(address);
		if(amount==null)amount = 0l;

		String unit = (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

		return getDisplayAmount(amount) + " " + unit;
	}

	private String getDisplayAmount(double value) {

		String strAmount = "";

		int unit = PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
		switch(unit) {
			case MonetaryUtil.MICRO_BTC:
				strAmount = Double.toString((value * 1000000.0) / 1e8);
				break;
			case MonetaryUtil.MILLI_BTC:
				strAmount = Double.toString((value * 1000.0) / 1e8);
				break;
			default:
				strAmount = MonetaryUtil.getInstance().getBTCFormat().format(value / 1e8);
				break;
		}

		return strAmount;
	}

	public abstract class CollapseActionbarScrollListener extends RecyclerView.OnScrollListener {

		private int mToolbarOffset = 0;

		public CollapseActionbarScrollListener() {
		}

		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			super.onScrolled(recyclerView, dx, dy);

			clipToolbarOffset();
			onMoved(mToolbarOffset);

			if((mToolbarOffset <toolbarHeight && dy>0) || (mToolbarOffset >0 && dy<0)) {
				mToolbarOffset += dy;
			}
		}

		private void clipToolbarOffset() {
			if(mToolbarOffset > toolbarHeight) {
				mToolbarOffset = toolbarHeight;
			} else if(mToolbarOffset < 0) {
				mToolbarOffset = 0;
			}
		}

		public abstract void onMoved(int distance);
	}

	private Bitmap generateQRCode(String uri) {

		Bitmap bitmap = null;
		int qrCodeDimension = 260;

		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

		try {
			bitmap = qrCodeEncoder.encodeAsBitmap();
		} catch (WriterException e) {
			e.printStackTrace();
		}

		return bitmap;
	}
}


//QR receiving
//						Account account = accounts.get(position);
//						String currentSelectedAddress = null;
//
//						ReceiveAddress currentSelectedReceiveAddress = null;
//						try {
//							currentSelectedReceiveAddress = HDPayloadBridge.getInstance(MyAccountsActivity.this).getReceiveAddress(position);
//							currentSelectedAddress = currentSelectedReceiveAddress.getAddress();
//						} catch (DecoderException e) {
//							e.printStackTrace();
//						} catch (IOException e) {
//							e.printStackTrace();
//						} catch (MnemonicException.MnemonicWordException e) {
//							e.printStackTrace();
//						} catch (MnemonicException.MnemonicChecksumException e) {
//							e.printStackTrace();
//						} catch (MnemonicException.MnemonicLengthException e) {
//							e.printStackTrace();
//						} catch (AddressFormatException e) {
//							e.printStackTrace();
//						}
//
//
//						ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));