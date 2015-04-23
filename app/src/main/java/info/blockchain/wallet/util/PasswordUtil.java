package info.blockchain.wallet.util;

import android.util.Patterns;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class PasswordUtil {

	private static PasswordUtil instance = null;

	private static HashMap<Pattern,Double> patternsWeight = null;
	private static final double WEIGHT_BAD_PATTERN = 20.0;
	private static final double WEIGHT_COMMON_PATTERN = 40.0;

	private static HashMap<Pattern,Double> patternsQuality = null;
	private static final double QUALITY_POOR = 10.0;
	private static final double QUALITY_MEDIUM = 26.0;
	private static final double QUALITY_STRONG = 31.0;

	private PasswordUtil() { ; }

	public static PasswordUtil getInstance() {

		if(instance == null) {

			patternsWeight = new HashMap<Pattern,Double>();
			patternsWeight.put(Pattern.compile("^\\d+$"), WEIGHT_COMMON_PATTERN);							// all digits
			patternsWeight.put(Pattern.compile("^[a-z]+\\d$"), WEIGHT_COMMON_PATTERN);						// all lower then 1 digit
			patternsWeight.put(Pattern.compile("^[A-Z]+\\d$"), WEIGHT_COMMON_PATTERN);						// all upper then 1 digit
			patternsWeight.put(Pattern.compile("^[a-zA-Z]+\\d$"), WEIGHT_BAD_PATTERN);						// all alpha then 1 digit
			patternsWeight.put(Pattern.compile("^[a-z]+\\d+$"), WEIGHT_BAD_PATTERN);						// all lower then digits
			patternsWeight.put(Pattern.compile("^[a-z]+$"), WEIGHT_COMMON_PATTERN);							// all lower
			patternsWeight.put(Pattern.compile("^[A-Z]+$"), WEIGHT_COMMON_PATTERN);							// all upper
			patternsWeight.put(Pattern.compile("^[A-Z][a-z]+$"), WEIGHT_COMMON_PATTERN);					// only one upper at start
			patternsWeight.put(Pattern.compile("^[A-Z][a-z]+\\d$"), WEIGHT_BAD_PATTERN);					// only one upper at start followed by 1 digit
			patternsWeight.put(Pattern.compile("^[A-Z][a-z]+\\d+$"), WEIGHT_COMMON_PATTERN);				// only one upper at start followed by digits
			patternsWeight.put(Pattern.compile("^[a-z]+[._!\\- @*#]$"), WEIGHT_COMMON_PATTERN);				// all lower followed by 1 special character
			patternsWeight.put(Pattern.compile("^[A-Z]+[._!\\- @*#]$"), WEIGHT_COMMON_PATTERN);				// all upper followed by 1 special character
			patternsWeight.put(Pattern.compile("^[a-zA-Z]+[._!\\- @*#]$"), WEIGHT_BAD_PATTERN);				// all alpha followed by 1 special character
			patternsWeight.put(Patterns.EMAIL_ADDRESS, WEIGHT_COMMON_PATTERN);								// email address
			patternsWeight.put(Patterns.WEB_URL, WEIGHT_COMMON_PATTERN);									// web url

			patternsQuality = new HashMap<Pattern, Double>();
			patternsQuality.put(Pattern.compile(".*\\d.*"), QUALITY_POOR);									// contains at least one digit
			patternsQuality.put(Pattern.compile(".*[a-z].*"), QUALITY_MEDIUM);								// contains at least one lowercase
			patternsQuality.put(Pattern.compile(".*[A-Z].*"), QUALITY_MEDIUM);								// contains at least one uppercase
			patternsQuality.put(Pattern.compile(".*[^a-zA-Z0-9 ].*"), QUALITY_STRONG);						// contains at least one special char

			instance = new PasswordUtil();
		}

		return instance;
	}

	public double getStrength(String pw) {

		//1. Get Quality
		double quality = getQuality(pw);

		//2. Get entropy
		double entropy = log2(Math.pow(quality,pw.length()));

		//3. Average entropy with bad patternsWeight
		double entropyWeighted = getEntropyWeightedByBadPatterns(entropy, pw);

		//4. Weigh unique symbol count
		entropyWeighted = getEntropyWeightedByUniqueSymbolCount(entropyWeighted, pw);

		return Math.min(entropyWeighted,100.0);
	}

	private static double getQuality(String pw){

		double base = 1.0;

		Set<Map.Entry<Pattern, Double>> set = patternsQuality.entrySet();
		for(Map.Entry<Pattern, Double> item : set){
			if(item.getKey().matcher(pw).matches())
				base += item.getValue();
		}

		return base;
	}

	private static double log2(double a) {
		return Math.log(a) / Math.log(2);
	}

	private static double getEntropyWeightedByBadPatterns(double entropy, String pw) {

		Set<Map.Entry<Pattern, Double>> set = patternsWeight.entrySet();

		double weight = entropy;
		boolean isBadPattern = false;

		for (Map.Entry<Pattern, Double> item : set) {

			if (item.getKey().matcher(pw).matches()) {
				isBadPattern = true;
				weight = Math.min(weight,item.getValue());
			}
		}
		if(isBadPattern)
			return (weight+entropy)/2.0;
		else
			return entropy;
	}

	private static double getEntropyWeightedByUniqueSymbolCount(double entropy, String pw){

		HashSet<Character> hash = new HashSet<>();
		for (int i = 0; i < pw.length(); i++)
			hash.add(pw.charAt(i));

		int uniqueSymbols = hash.size();
		if(uniqueSymbols<=1)
			return entropy*0.1;
		else if(uniqueSymbols<=2)
			return entropy*0.25;
		else if(uniqueSymbols<=3)
			return entropy*0.5;
		else if(uniqueSymbols<=4)
			return entropy*0.75;
		else if(uniqueSymbols<=5)
			return entropy*0.9;
		else
			return entropy;
	}
}