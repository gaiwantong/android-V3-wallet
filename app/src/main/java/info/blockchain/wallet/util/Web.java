package info.blockchain.wallet.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class Web	{

    public static final String SPEND_DOMAIN = "https://blockchain.info/";
    public static final String PAYLOAD_DOMAIN = "https://blockchain.info/";
    public static final String PAIRING_DOMAIN = "https://blockchain.info/";
    public static final String MULTIADDR_DOMAIN = "https://blockchain.info/";
	public static final String EXCHANGE_URL = "https://blockchain.info/ticker";
    public static final String ACCESS_URL = "https://blockchain.info/pin-store";
    public static final String UNSPENT_OUTPUTS_DOMAIN = "https://blockchain.info/";

    private static final int DefaultRequestRetry = 2;
    private static final int DefaultRequestTimeout = 60000;

    public static String postURL(String request, String urlParameters) throws Exception {

        String error = null;

        for (int ii = 0; ii < DefaultRequestRetry; ++ii) {
            URL url = new URL(request);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

                connection.setUseCaches (false);

                connection.setConnectTimeout(DefaultRequestTimeout);
                connection.setReadTimeout(DefaultRequestTimeout);

                connection.connect();

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                connection.setInstanceFollowRedirects(false);

                if (connection.getResponseCode() == 200) {
//					Log.d("postURL", "return code 200");
                    return IOUtils.toString(connection.getInputStream(), "UTF-8");
                }
                else {
                    error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
//					Log.d("postURL", "return code " + error);
                }

                Thread.sleep(5000);
            } finally {
                connection.disconnect();
            }
        }

        throw new Exception("Invalid Response " + error);
    }

    public static String getURL(String URL) throws Exception {

        URL url = new URL(URL);

        String error = null;

        for (int ii = 0; ii < DefaultRequestRetry; ++ii) {

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            try {
                connection.setRequestMethod("GET");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

                connection.setConnectTimeout(DefaultRequestTimeout);
                connection.setReadTimeout(DefaultRequestTimeout);

                connection.setInstanceFollowRedirects(false);

                connection.connect();

                if (connection.getResponseCode() == 200)
                    return IOUtils.toString(connection.getInputStream(), "UTF-8");
                else
                    error = IOUtils.toString(connection.getErrorStream(), "UTF-8");

                Thread.sleep(5000);
            } finally {
                connection.disconnect();
            }
        }

        return error;
    }
/*
	public String securePost(String url, Map<Object, Object> data) throws Exception {  
		Map<Object, Object> params = new HashMap<Object, Object>(data);

		if (! data.containsKey("sharedKey")) {
			serverTimeOffset = 500; //TODO dont hard code serverTimeOffset

			String sharedKey = getSharedKey().toLowerCase();
			long now = new Date().getTime();

			long timestamp = (now - serverTimeOffset) / 10000;

			String text = sharedKey + Long.toString(timestamp);
			String SKHashHex = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"))));
			int i = 0;
			String tSKUID = SKHashHex.substring(i, i+=8)+"-"+SKHashHex.substring(i, i+=4)+"-"+SKHashHex.substring(i, i+=4)+"-"+SKHashHex.substring(i, i+=4)+"-"+SKHashHex.substring(i, i+=12);

			params.put("sharedKey", tSKUID);
			params.put("sKTimestamp", Long.toString(timestamp));
			params.put("sKDebugHexHash", SKHashHex);
			params.put("sKDebugTimeOffset", Long.toString(serverTimeOffset));
			params.put("sKDebugOriginalClientTime", Long.toString(now));
			params.put("sKDebugOriginalSharedKey", sharedKey);

			if (! params.containsKey("guid"))
				params.put("guid", this.getGUID());

			if (! params.containsKey("format"))
				params.put("format", "plain");	
		}

		String response = WalletUtils.postURLWithParams(url, params);
		return response;
	}
*/
}
