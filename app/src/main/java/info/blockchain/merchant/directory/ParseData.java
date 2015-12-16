package info.blockchain.merchant.directory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

//import android.util.Log;

public class ParseData {

    public static ArrayList<BTCBusiness> parse(String data) {

        ArrayList<BTCBusiness> btcb = new ArrayList<BTCBusiness>();

        try {
            JSONArray jsonArray = new JSONArray(data);

            if (jsonArray != null && jsonArray.length() > 0) {

                for (int i = 0; i < jsonArray.length(); i++) {
                    BTCBusiness business = new BTCBusiness();
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    if (jsonObj.has("id")) {
                        business.id = jsonObj.getString("id");
                    }
                    if (jsonObj.has("name")) {
                        business.name = jsonObj.getString("name");
                    }
                    if (jsonObj.has("address")) {
                        business.address = jsonObj.getString("address");
                    }
                    if (jsonObj.has("city")) {
                        business.city = jsonObj.getString("city");
                    }
                    if (jsonObj.has("pcode")) {
                        business.pcode = jsonObj.getString("pcode");
                    }
                    if (jsonObj.has("tel")) {
                        business.tel = jsonObj.getString("tel");
                    }
                    if (jsonObj.has("web")) {
                        business.web = jsonObj.getString("web");
                    }
                    if (jsonObj.has("lat")) {
                        business.lat = jsonObj.getString("lat");
                    }
                    if (jsonObj.has("lon")) {
                        business.lon = jsonObj.getString("lon");
                    }
                    if (jsonObj.has("flag")) {
                        business.flag = jsonObj.getString("flag");
                    }
                    if (jsonObj.has("desc")) {
                        business.desc = jsonObj.getString("desc");
                    }
                    if (jsonObj.has("distance")) {
                        business.distance = jsonObj.getString("distance");
                    }
                    if (jsonObj.has("hc")) {
                        business.hc = jsonObj.getString("hc");
                    }

                    btcb.add(business);
                }

            }
        } catch (JSONException je) {
            je.printStackTrace();
        }

        return btcb;

    }

}
