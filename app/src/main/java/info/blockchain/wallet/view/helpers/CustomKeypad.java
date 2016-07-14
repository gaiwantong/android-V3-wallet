package info.blockchain.wallet.view.helpers;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TableLayout;

import java.util.ArrayList;

import piuk.blockchain.android.R;

public class CustomKeypad implements View.OnClickListener{

    private Context context = null;

    private TableLayout numpad;
    private ArrayList<EditText> viewList;
    private String decimalSeparator = ".";

    public CustomKeypad(Context ctx, TableLayout pnumpad) {

        context = ctx;

        numpad = pnumpad;
        numpad.setVisibility(View.GONE);

        numpad.findViewById(R.id.button1).setOnClickListener(this);
        numpad.findViewById(R.id.button2).setOnClickListener(this);
        numpad.findViewById(R.id.button3).setOnClickListener(this);
        numpad.findViewById(R.id.button4).setOnClickListener(this);
        numpad.findViewById(R.id.button5).setOnClickListener(this);
        numpad.findViewById(R.id.button6).setOnClickListener(this);
        numpad.findViewById(R.id.button7).setOnClickListener(this);
        numpad.findViewById(R.id.button8).setOnClickListener(this);
        numpad.findViewById(R.id.button9).setOnClickListener(this);
        numpad.findViewById(R.id.button10).setOnClickListener(this);
        numpad.findViewById(R.id.button0).setOnClickListener(this);
        numpad.findViewById(R.id.buttonDeleteBack).setOnClickListener(this);
        numpad.findViewById(R.id.buttonDone).setOnClickListener(this);

        viewList = new ArrayList<>();
    }

    public void enableOnView(final EditText view){

        if(!viewList.contains(view))viewList.add(view);

        view.setTextIsSelectable(true);
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View view = ((Activity)context).getCurrentFocus();
                    if (view != null) {
                        InputMethodManager inputManager = (InputMethodManager) ((Activity)context).getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                    numpad.setVisibility(View.VISIBLE);
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numpad.setVisibility(View.VISIBLE);
            }
        });
    }

    public void setNumpadVisibility(int visibility){
        numpad.setVisibility(visibility);
    }

    @Override
    public void onClick(View v) {

        String pad = "";
        switch (v.getId()) {
            case R.id.button10:
                pad = decimalSeparator;
                break;
            case R.id.buttonDeleteBack:

                deleteFromFocussedView();
                return;

            case R.id.buttonDone:
                numpad.setVisibility(View.GONE);
                break;
            default:
                pad = v.getTag().toString().substring(0, 1);
                break;
        }

        // Append tapped #
        if (pad != null) {
            appendToFocussedView(pad);
        }
    }

    private void appendToFocussedView(String pad){

        for(final EditText view : viewList) {

            if (view.hasFocus()) {

                int startSelection=view.getSelectionStart();
                int endSelection=view.getSelectionEnd();
                if(endSelection - startSelection > 0) {
                    String selectedText = view.getText().toString().substring(startSelection, endSelection);
                    view.setText(view.getText().toString().replace(selectedText, pad));
                }else {
                    view.append(pad);
                }

                if (view.getText().length() > 0) {
                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            view.setSelection(view.getText().toString().length());
                        }
                    });
                }
            }
        }
    }

    private void deleteFromFocussedView(){

        for(final EditText view : viewList) {
            if (view.hasFocus() && view.getText().length() > 0) {
                view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
        }
    }

    public void setDecimalSeparator(String passedDecimalSeparator) {
        decimalSeparator = passedDecimalSeparator;
    }

    public boolean isVisible(){
        return (numpad.getVisibility() == View.VISIBLE);
    }
}
