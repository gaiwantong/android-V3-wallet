package info.blockchain.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.TypefaceUtil;

public class SupportActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_support);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar_general);
		toolbar.setTitle(getResources().getString(R.string.contact_support));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		TextView header = (TextView)findViewById(R.id.my_wallet_id_header);
		header.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());

		TextView walletId = (TextView)findViewById(R.id.wallet_id);
		walletId.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());

		final String guid = PayloadFactory.getInstance().get().getGuid();
		walletId.setText(guid);

		LinearLayout idContainer = (LinearLayout)findViewById(R.id.wallet_id_container);
		idContainer.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				new AlertDialog.Builder(SupportActivity.this)
						.setTitle(R.string.app_name)
						.setMessage(R.string.guid_to_clipboard)
						.setCancelable(false)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int whichButton) {
								android.content.ClipboardManager clipboard = (android.content.ClipboardManager) SupportActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
								android.content.ClipData clip = null;
								clip = android.content.ClipData.newPlainText("guid", guid);
								clipboard.setPrimaryClip(clip);
							}

						}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						;
					}
				}).show();
			}
		});

		TextView newSupportRequest = (TextView)findViewById(R.id.new_support_request);
		newSupportRequest.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());

		TextView emailAction = (TextView)findViewById(R.id.email_action);
		emailAction.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());

		emailAction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "support@blockchain.zendesk.com", null));
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.email_subject));
				emailIntent.putExtra(Intent.EXTRA_TEXT, "Your wallet id - "+guid+"\n\n");
				startActivity(Intent.createChooser(emailIntent, SupportActivity.this.getResources().getText(R.string.email_chooser)));
			}
		});
	}


    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(SupportActivity.this).updatePinEntryTime();
    }

}
