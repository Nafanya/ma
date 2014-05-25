package nyaschenko.oki;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nyaschenko.oki.utils.ApiRequest;
import ru.ok.android.sdk.Odnoklassniki;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class ApiFetcher<Token> extends HandlerThread {
	private static final String TAG = "ApiFetcher";
	
	private static final int MESSAGE_API_REQUEST = 0;
	
	Odnoklassniki mOdnoklassniki;
	Handler mResponseHandler;
	Handler mHandler;
	Context mContext;
	Map<Token, ApiRequest> requestMap =
			Collections.synchronizedMap(new HashMap<Token, ApiRequest>());
	Listener<Token> mListener;
	
	public interface Listener<Token> {
		void onRequestComplete(Token token, String result);
	}
	
	public void setListener(Listener<Token> listener) {
		mListener = listener;
	}
	
	public ApiFetcher(Context context, Handler responseHandler) {
		super(TAG);
		mContext = context;
		mOdnoklassniki = Odnoklassniki.getInstance(context);
		mResponseHandler = responseHandler;
	}

	public void queueRequest(Token token, ApiRequest request) {
		Log.i(TAG, "Got a new request: " + request);
		while (mHandler == null) {
			// As fragment was just created, need a little more time for mHandler to set
		}
		requestMap.put(token, request);
		mHandler.obtainMessage(MESSAGE_API_REQUEST, token).sendToTarget();
		
	}
	
	@Override
	protected void onLooperPrepared() {
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MESSAGE_API_REQUEST) {
					@SuppressWarnings("unchecked")
					Token token = (Token)msg.obj;
					Log.i(TAG, "Got request in handler: " + requestMap.get(token));
					handleRequest(token);
				}
			}
		};
	}
	
	public void clearQueue() {
		mHandler.removeMessages(MESSAGE_API_REQUEST);
		requestMap.clear();
	}
	
	private void handleRequest(final Token token) {
		try {
			final ApiRequest request = requestMap.get(token);
			if (request == null)
				return;
		
			
			String tempResponse = mOdnoklassniki.request(request.getMethod(), request.getParams(), "get");
			for (int attempt = 0; attempt < 3; attempt++) {
				if (tempResponse == null || tempResponse.contains("PARAM_SESSION_EXPIRED")) {
					mOdnoklassniki.refreshToken(mContext);
					tempResponse = mOdnoklassniki.request(request.getMethod(), request.getParams(), "get");
				} else {
					break;
				}
			}
			final String response = tempResponse;
			
			mResponseHandler.post(new Runnable() {
				public void run() {
					if (!requestMap.get(token).equals(request)) {
						return;
					}
					/*
					if (request.getMethod().equals("users.getInfo")) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}*/
					requestMap.remove(token);
					mListener.onRequestComplete(token, response);
				}
			});
		} catch (IOException e) {
			Log.e(TAG, "Failed api request");
		}
	}
	
	private boolean isInternetAvailable() {
		ConnectivityManager cm =
		        (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}
	

}
