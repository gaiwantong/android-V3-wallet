package info.blockchain.wallet.util;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import android.util.Patterns;
//import android.util.Log;

public class PasswordUtil {

	private static List<Pattern> patterns = null;

	private static PasswordUtil instance = null;
	
	private PasswordUtil() { ; }

	public static PasswordUtil getInstance() {

		if(instance == null) {

			patterns = new ArrayList<Pattern>();
			
			patterns.add(Pattern.compile("^\\d+$"));										// all digits
			patterns.add(Pattern.compile("^[a-z]+\\d$"));									// all lower then 1 digit
			patterns.add(Pattern.compile("^[A-Z]+\\d$"));									// all upper then 1 digit
			patterns.add(Pattern.compile("^[a-zA-Z]+\\d$"));								// all alpha then 1 digit
			patterns.add(Pattern.compile("^[a-z]+\\d+$"));									// all lower then digits
			patterns.add(Pattern.compile("^[a-z]+$"));										// all lower
			patterns.add(Pattern.compile("^[A-Z]+$"));										// all upper
			patterns.add(Pattern.compile("^[A-Z][a-z]+$"));									// only one upper at start
			patterns.add(Pattern.compile("^[A-Z][a-z]+\\d$"));								// only one upper at start followed by 1 digit
			patterns.add(Pattern.compile("^[A-Z][a-z]+\\d+$"));								// only one upper at start followed by digits
			patterns.add(Pattern.compile("^[a-z]+[._!\\- @*#]$"));							// all lower followed by 1 special character
			patterns.add(Pattern.compile("^[A-Z]+[._!\\- @*#]$"));							// all upper followed by 1 special character
			patterns.add(Pattern.compile("^[a-zA-Z]+[._!\\- @*#]$"));						// all alpha followed by 1 special character
//			patterns.add(Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]+$"));// email address
			patterns.add(Patterns.EMAIL_ADDRESS);											// email address
//			patterns.add(Pattern.compile("^[a-z\\-ZA-Z0-9.-]+$"));							// web url
			patterns.add(Patterns.WEB_URL);													// web url

			instance = new PasswordUtil();
		}

		return instance;
	}

	public boolean isWeak(String pw) {

		for(Pattern p : patterns) {
			if(p.matcher(pw).matches()) {
				return true;
			}
		}

		return false;
	}

}
