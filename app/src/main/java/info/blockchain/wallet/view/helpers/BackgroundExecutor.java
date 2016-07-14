package info.blockchain.wallet.view.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import piuk.blockchain.android.R;

public class BackgroundExecutor extends AsyncTask<Void, Void, Void>{

    ProgressDialog progress;
    Context context;
    Command command;
    String title;
    String message;

    public interface Command{
        void execute();
    }

    public BackgroundExecutor(Context context, Command command) {
        this.context = context;
        this.command = command;
        this.title = this.context.getString(R.string.app_name);
        this.message = this.context.getString(R.string.please_wait);
    }

    public BackgroundExecutor(Context context, Command command, String title, String message) {
        this.context = context;
        this.command = command;
        this.title = title;
        this.message = message;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progress = new ProgressDialog(context);
        progress.setTitle(this.title);
        progress.setMessage(this.message);
        progress.show();
    }

    @Override
    protected void onPostExecute(Void success) {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
        super.onPostExecute(success);
    }

    @Override
    protected Void doInBackground(Void... params) {
        this.command.execute();
        return null;
    }
}
