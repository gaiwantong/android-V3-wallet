package info.blockchain.wallet.util;

import android.content.Context;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

public class HostnameVerifierUtil {

    private static HostnameVerifierUtil instance = null;
    private static Context context = null;

    private HostnameVerifierUtil() { ; }

    public static HostnameVerifierUtil getInstance() {

        if(instance == null) {
            instance = new HostnameVerifierUtil();
        }

        return instance;
    }

    public boolean isValid() {

        boolean ret = false;

        DefaultHttpClient client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        HostnameVerifier verifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
        socketFactory.setHostnameVerifier((X509HostnameVerifier)verifier);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
        DefaultHttpClient httpClient = new DefaultHttpClient(mgr, client.getParams());
        HttpsURLConnection.setDefaultHostnameVerifier(verifier);
        HttpPost httpPost = new HttpPost(WebUtil.VALIDATE_SSL_URL);
        try {
            HttpResponse response = httpClient.execute(httpPost);
            Log.i("HostnameVerifierUtil", "Hostname validated");
            ret = true;
        }
        catch(Exception e) {
            Log.i("HostnameVerifierUtil", "Hostname not validated");
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

}
