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
import android.widget.Toast;

import com.google.bitcoin.uri.BitcoinURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

public class MyAccountsActivity extends Activity {

	public static String ACCOUNT_HEADER = "";
	public static String IMPORTED_HEADER = "";

	LinearLayoutManager layoutManager = null;
	RecyclerView mRecyclerView = null;
	private List<MyAccountItem> accountsAndImportedList = null;
	TextView myAccountsHeader;
	int minHeaderTranslation;
	public int toolbarHeight;

	ImageView backNav;
	ImageView menuImport;
	HashMap<View,Boolean> rowViewState;

	private ArrayList<Integer> headerPositions;
	int hdAccountsIdx;
	List<LegacyAddress> legacy = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_my_accounts);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		ACCOUNT_HEADER = getResources().getString(R.string.my_accounts);
		IMPORTED_HEADER = getResources().getString(R.string.imported_addresses);

		backNav = (ImageView)findViewById(R.id.back_nav);
		backNav.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		menuImport = (ImageView)findViewById(R.id.menu_import);
		menuImport.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(MyAccountsActivity.this,"Import Coming Soon",Toast.LENGTH_SHORT).show();
			}
		});

		myAccountsHeader = (TextView)findViewById(R.id.my_accounts_heading);
		minHeaderTranslation = myAccountsHeader.getHeight();

		mRecyclerView = (RecyclerView)findViewById(R.id.accountsList);
		layoutManager = new LinearLayoutManager(this);
		mRecyclerView.setLayoutManager(layoutManager);

		headerPositions = new ArrayList<Integer>();

		ArrayList<MyAccountItem> accountItems = new ArrayList<>();
		//First Header Position
		headerPositions.add(0);
		accountItems.add(new MyAccountItem(ACCOUNT_HEADER,"",getResources().getDrawable(R.drawable.icon_accounthd)));

		accountsAndImportedList = getAccounts();

		toolbarHeight = (int)getResources().getDimension(R.dimen.action_bar_height)+35;

		int index = 0;
		for(MyAccountItem item : accountsAndImportedList){
			accountItems.add(item);
			index++;
		}

		MyAccountsAdapter accountsAdapter = new MyAccountsAdapter(accountItems);
		mRecyclerView.setAdapter(accountsAdapter);

		rowViewState = new HashMap<View, Boolean>();
		mRecyclerView.addOnItemTouchListener(
				new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {

					private int originalHeight = 0;
					private int newHeight = 0;
					private int expandDuration = 200;
					private boolean mIsViewExpanded = false;

					@Override
					public void onItemClick(final View view, int position) {

						if (headerPositions.contains(position)) return;//headers unclickable

						try {
							mIsViewExpanded = rowViewState.get(view);
						} catch (Exception e) {
							mIsViewExpanded = false;
						}

						final ImageView qrTest = (ImageView) view.findViewById(R.id.qrr);
						final TextView addressView = (TextView)view.findViewById(R.id.my_account_row_address);

						//Receiving Address
						String currentSelectedAddress = null;

						if (position-2 >= hdAccountsIdx)//2 headers before imported
							currentSelectedAddress = legacy.get(position-2 - hdAccountsIdx).getAddress();
						else {
							ReceiveAddress currentSelectedReceiveAddress = null;
							try {
								currentSelectedReceiveAddress = HDPayloadBridge.getInstance(MyAccountsActivity.this).getReceiveAddress(position-1);//1 header before accounts
								currentSelectedAddress = currentSelectedReceiveAddress.getAddress();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						addressView.setText(currentSelectedAddress);

						//Receiving QR
						qrTest.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));

						if (originalHeight == 0) {
							originalHeight = view.getHeight();
						}

						newHeight = originalHeight + qrTest.getHeight() + (addressView.getHeight()*2)+(16*2);

						final String finalCurrentSelectedAddress = currentSelectedAddress;
						qrTest.setOnLongClickListener(new View.OnLongClickListener() {
							@Override
							public boolean onLongClick(View v) {

								android.content.ClipboardManager clipboard = (android.content.ClipboardManager)MyAccountsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
								android.content.ClipData clip = null;
								clip = android.content.ClipData.newPlainText("Send address", finalCurrentSelectedAddress);
								Toast.makeText(MyAccountsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();
								clipboard.setPrimaryClip(clip);

								return false;
							}
						});

						ValueAnimator valueAnimator;
						if (!mIsViewExpanded) {
							//Expanding

							//Fade QR in - expansion of row will create slide down effect
							qrTest.setVisibility(View.VISIBLE);
							qrTest.setAnimation(AnimationUtils.loadAnimation(MyAccountsActivity.this, R.anim.abc_fade_in));
							qrTest.setEnabled(true);

							addressView.setVisibility(View.VISIBLE);
							Animation aanim = AnimationUtils.loadAnimation(MyAccountsActivity.this, R.anim.abc_fade_in);
							aanim.setDuration(expandDuration);
							addressView.setAnimation(aanim);
							addressView.setEnabled(true);

							mIsViewExpanded = !mIsViewExpanded;
							view.findViewById(R.id.bottom_seperator).setVisibility(View.VISIBLE);
							view.findViewById(R.id.top_seperator).setVisibility(View.VISIBLE);
							valueAnimator = ValueAnimator.ofInt(originalHeight, newHeight);

						} else {
							//Collapsing
							view.findViewById(R.id.bottom_seperator).setVisibility(View.INVISIBLE);
							view.findViewById(R.id.top_seperator).setVisibility(View.INVISIBLE);
							mIsViewExpanded = !mIsViewExpanded;
							valueAnimator = ValueAnimator.ofInt(newHeight, originalHeight);

							//Slide QR away
							qrTest.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down));
							Animation aanim = AnimationUtils.loadAnimation(MyAccountsActivity.this, R.anim.abc_fade_out);
							aanim.setDuration(expandDuration/2);
							addressView.setAnimation(aanim);

							//Fade QR and hide when done
							Animation anim = new AlphaAnimation(1.00f, 0.00f);
							anim.setDuration(expandDuration/2);
							// Set a listener to the animation and configure onAnimationEnd
							anim.setAnimationListener(new Animation.AnimationListener() {
								@Override
								public void onAnimationStart(Animation animation) {

								}

								@Override
								public void onAnimationEnd(Animation animation) {
									qrTest.setVisibility(View.INVISIBLE);
									qrTest.setEnabled(false);

									addressView.setVisibility(View.INVISIBLE);
									addressView.setEnabled(false);
								}

								@Override
								public void onAnimationRepeat(Animation animation) {

								}
							});

							qrTest.startAnimation(anim);
							addressView.startAnimation(anim);
						}

						//Set and start row collapse/expand
						valueAnimator.setDuration(expandDuration);
						valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
						valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							public void onAnimationUpdate(ValueAnimator animation) {
								Integer value = (Integer) animation.getAnimatedValue();
								view.getLayoutParams().height = value.intValue();
								view.requestLayout();
							}
						});


						valueAnimator.start();
						rowViewState.put(view,mIsViewExpanded);
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

	private List<MyAccountItem> getAccounts() {

		List<MyAccountItem> accountList = new ArrayList<MyAccountItem>();
		ImportedAccount iAccount = null;

		List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
		if(PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
			iAccount = new ImportedAccount(getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
		}

		if(accounts.get(accounts.size() - 1) instanceof ImportedAccount) {
			accounts.remove(accounts.size() - 1);
		}
		hdAccountsIdx = accounts.size();

		int i = 0;
		for(; i < accounts.size(); i++) {

			String label = accounts.get(i).getLabel();
			if(label==null || label.length() == 0)label = "Account: " + (i + 1);

			accountList.add(new MyAccountItem(label,displayBalance(i), getResources().getDrawable(R.drawable.icon_accounthd)));
		}

		if(iAccount != null) {

			//Imported Header Position
			headerPositions.add(i+1);
			accountList.add(new MyAccountItem(IMPORTED_HEADER,"", getResources().getDrawable(R.drawable.icon_accounthd)));

			legacy = iAccount.getLegacyAddresses();
			for(int j = 0; j < legacy.size(); j++) {

				String label = legacy.get(j).getLabel();
				if(label==null || label.length() == 0)label = legacy.get(j).getAddress();

				accountList.add(new MyAccountItem(label,displayBalanceImported(j),getResources().getDrawable(R.drawable.icon_imported)));
			}
		}

		return accountList;
	}

	private String displayBalance(int index) {

		String address = HDPayloadBridge.getInstance(this).account2Xpub(index);
		Long amount = MultiAddrFactory.getInstance().getXpubAmounts().get(address);
		if(amount==null)amount = 0l;

		String unit = (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

		return getDisplayAmount(amount) + " " + unit;
	}

	private String displayBalanceImported(int index) {

		String address = legacy.get(index).getAddress();
		Long amount = MultiAddrFactory.getInstance().getLegacyBalance(address);
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