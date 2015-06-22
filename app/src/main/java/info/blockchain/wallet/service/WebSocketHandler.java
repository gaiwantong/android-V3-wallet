package info.blockchain.wallet.service;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import de.tavendo.autobahn.WebSocketConnection;
import info.blockchain.wallet.EventListeners;
import info.blockchain.wallet.HDPayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.NotificationsFactory;

import piuk.blockchain.android.R;

public class WebSocketHandler {

	private int nfailures = 0;
	private boolean isRunning = true;
	private long lastConnectAttempt = 0;
	private static final long nextConnectDelay = 30000L;
	private WebSocketConnection mConnection = new WebSocketConnection();

	private static String guid = null;
	private static String[] xpubs = null;
	private static String[] addrs = null;
	
	private static Context context = null;
			
	final private EventListeners.EventListener walletEventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "Websocket Listener";
		}

		@Override
		public void onWalletDidChange() {
			try {
				if(isRunning) {
					start();
				} else if(isConnected()) {
					// Disconnect and reconnect
					// To resubscribe
					subscribe();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	public WebSocketHandler(Context ctx, String guid, String[] xpubs, String[] addrs) {
		this.context = ctx;
		this.guid = guid;
		this.xpubs = xpubs;
		this.addrs = addrs;
	}

	public void send(String message) {
		try {
			if(mConnection.isConnected()) {
				mConnection.sendTextMessage(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void subscribe() {

		if(guid == null) {
			return;
		}

		Log.i("WebSocketHandler", "Websocket subscribe");

		send("{\"op\":\"wallet_sub\",\"guid\":\"" + guid + "\"}");

		for(int i = 0; i < xpubs.length; i++) {
			if(xpubs[i] != null && xpubs[i].length() > 0) {
				send("{\"op\":\"xpub_sub\", \"xpub\":\"" + xpubs[i] + "\"}");
			}
		}

		for(int i = 0; i < addrs.length; i++) {
			if(addrs[i] != null && addrs[i].length() > 0) {
				send("{\"op\":\"addr_sub\", \"addr\":\"" + addrs[i] + "\"}");
			}
		}

	}

	public boolean isConnected() {
		return  mConnection != null && mConnection.isConnected();
	}

	public void stop() {
		if(mConnection.isConnected()) {
			mConnection.disconnect();
		}

		EventListeners.removeEventListener(walletEventListener);
		
		this.isRunning = false;
	}

	public void connect() throws URISyntaxException, InterruptedException {

		mConnection = new WebSocketConnection();

		final WebSocketHandler handler = this;
		
		if(guid == null) {
			return;
		}

		try {
			mConnection.connect(new URI("wss://ws.blockchain.info/inv"), new de.tavendo.autobahn.WebSocketConnectionHandler() {
				@Override
				public void onOpen() {
					handler.subscribe();
					handler.nfailures = 0;				
				}

				@Override
				public void onTextMessage(String message) {
					if(guid == null) {
						return;
					}

					try {
//						Map<String, Object> top = (Map<String, Object>) JSONValue.parse(message);
						JSONObject jsonObject = null;
				        try {
				            jsonObject = new JSONObject(message);
				        }
				        catch(JSONException je) {
				            jsonObject = null;
				        }

						if(jsonObject == null) {
							return;
						}

						String op = (String)jsonObject.get("op");
						if(op.equals("utx") && jsonObject.has("x")) {

							JSONObject objX = (JSONObject)jsonObject.get("x");

		            		long value = 0L;
		            		long total_value = 0L;
		            		long ts = 0L;
		            		String in_addr = null;

							if(objX.has("time")) {
								ts = objX.getLong("time");
							}

		                    if(objX.has("inputs"))  {
		                    	JSONArray inputArray = (JSONArray)objX.get("inputs");
		                    	JSONObject inputObj = null;
		                    	for(int j = 0; j < inputArray.length(); j++)  {
		                    		inputObj = (JSONObject)inputArray.get(j);
		                            if(inputObj.has("prev_out"))  {
		                            	JSONObject prevOutObj = (JSONObject)inputObj.get("prev_out");
		                                if(prevOutObj.has("value"))  {
		                                	value = prevOutObj.getLong("value");
		                                }
		                                if(prevOutObj.has("xpub"))  {
		                                	total_value -= value;
		                                }
		                                else if(prevOutObj.has("addr"))  {
			                                if(PayloadFactory.getInstance().get().containsLegacyAddress((String)prevOutObj.get("addr")))  {
			                                	total_value -= value;
			                                }
			                                else if(in_addr == null)  {
				                                	in_addr = (String)prevOutObj.get("addr");
			                                }
			                                else  {
			                                	;
			                                }
		                                }
		                                else  {
		                                	;
		                                }
		                            }
		                    	}
		                    }

		                    if(objX.has("out"))  {
		                    	JSONArray outArray = (JSONArray)objX.get("out");
		                    	JSONObject outObj = null;
		                    	for(int j = 0; j < outArray.length(); j++)  {
		                    		outObj = (JSONObject)outArray.get(j);
	                                if(outObj.has("value"))  {
	                                	value = outObj.getLong("value");
	                                }
	                                if(outObj.has("xpub"))  {
	                                	total_value += value;
	                                }
	                                else if(outObj.has("addr"))  {
		                                if(PayloadFactory.getInstance().get().containsLegacyAddress((String)outObj.get("addr")))  {
		                                	total_value += value;
		                                }
	                                }
	                                else  {
	                                	;
	                                }
		                    	}
	                    	}
							
		                    String title = context.getString(R.string.app_name);
		                    if(total_value > 0L)  {
			                    String marquee = context.getString(R.string.received_bitcoin) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double)total_value / 1e8) + "BTC";
			                    String text = marquee;
			                    if(total_value > 0)  {
			                    	text += " from " + in_addr;
			                    }

								NotificationsFactory.getInstance(context).setNotification(title, marquee, text, R.drawable.ic_notification_transparent, R.drawable.ic_launcher, info.blockchain.wallet.MainActivity.class, 1000);
		                    }

		                	new Thread()	{
		                	    public void run() {
		                	    	
		                	    	Looper.prepare();

		         					try {
		             		        	HDPayloadBridge.getInstance().getBalances();
		         					}
		         		        	catch(JSONException je) {
		         		        		je.printStackTrace();
		         		        	}
		         		        	catch(IOException ioe) {
		         		        		ioe.printStackTrace();
		         		        	}
		         		        	catch(DecoderException de) {
		         		        		de.printStackTrace();
		         		        	}
		         		        	catch(AddressFormatException afe) {
		         		        		afe.printStackTrace();
		         		        	}
		         		        	catch(MnemonicException.MnemonicLengthException mle) {
		         		        		mle.printStackTrace();
		         		        	}
		         		        	catch(MnemonicException.MnemonicChecksumException mce) {
		         		        		mce.printStackTrace();
		         		        	}
		         		        	catch(MnemonicException.MnemonicWordException mwe) {
		         		        		mwe.printStackTrace();
		         		        	}

				                    Intent intent = new Intent("info.blockchain.wallet.BalanceFragment.REFRESH");
				        		    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

		                	    	Looper.loop();
		                	    }
		                	}.start();

						}
						else if(op.equals("on_change")) {
							;
						}
						else if(op.equals("block")) {
							;
						}
						else {
							;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}

//				@Override
				public void onClose(int code, String reason) {
					++handler.nfailures;
					Log.i("WebSocketHandler", "failure:" + reason);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();

			++handler.nfailures;
		}

		lastConnectAttempt = System.currentTimeMillis();

		Log.i("WebSocketHandler", "Websocket connect()");

		EventListeners.addEventListener(walletEventListener);
	}

	public void start() {

		if(lastConnectAttempt > System.currentTimeMillis() - nextConnectDelay) {
			return; 
		}

		this.isRunning = true;

		try {
			stop();
			connect();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}