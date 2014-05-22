package nyaschenko.oki;

import nyaschenko.oki.utils.ApiRequest;
import ru.ok.android.sdk.Odnoklassniki;
import ru.ok.android.sdk.OkTokenRequestListener;
import ru.ok.android.sdk.util.OkScope;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class LoginFragment extends SherlockFragment {
	public static final String TAG = "LoginFragment";
	
	private static Context mContext;
	
	protected static final String APP_ID = "572588032";
	protected static final String APP_SECRET = "31F3E52DE4ECD8D56AB6FE06";
	protected static final String APP_KEY = "CBABACCDCBABABABA";
	protected Odnoklassniki mOdnoklassniki;
	protected static final String PERMISSIONS = OkScope.VALUABLE_ACCESS + ";" + OkScope.PHOTO_CONTENT;
	
	private static final String TOKEN_CURRENT_USER = "TOKEN_CURRENT_USER";
	
	Button loginButton;
	
	private Callbacks mCallbacks;
	
	public interface Callbacks {
		void onAuthComplete();
		void onGetCurrentUser(String userId);
	}
	
	ApiFetcher<String> mApiThread;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCallbacks = (Callbacks) activity;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		mContext = getSherlockActivity();
		initOk();
		initBackgroundLoader();
        
	}
	
	@Override
	public void onDestroy() {
		mContext = null;
		mApiThread.quit();
		super.onDestroy();
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mApiThread.clearQueue();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        loginButton = (Button) v.findViewById(R.id.buttonLogin);
        loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View view) {
				mOdnoklassniki.requestAuthorization(getSherlockActivity(), false, PERMISSIONS); 
			}
		});
        
        return v;
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ensureTokenValidity();
	}
	
	private void initBackgroundLoader() {
		mApiThread = new ApiFetcher<String>(mContext, new Handler());
        mApiThread.setListener(new ApiFetcher.Listener<String>() {

			@Override
			public void onRequestComplete(String token, String result) {
				Log.i(TAG, "Recieved response for request[" + token + "]: " + result);
				
				onGetCurrentUser(result);
			}
        	
		});
        mApiThread.start();
        mApiThread.getLooper();
        Log.i(TAG, "Background thread started");
	}
	
	private void initOk() {
		mOdnoklassniki = Odnoklassniki.createInstance(mContext, APP_ID, APP_SECRET, APP_KEY);
		//mOdnoklassniki = Odnoklassniki.getInstance(mContext);
		mOdnoklassniki.setTokenRequestListener(new OkTokenRequestListener() {
			@Override
			public void onSuccess(String accessToken) {
				//Toast.makeText(mContext, "Recieved new token : " + accessToken, Toast.LENGTH_LONG).show();
				Log.i(TAG, "Recieved new token: " + accessToken);
				//mCallbacks.onAuthComplete();
				Intent intent = new Intent(getSherlockActivity(), MainActivity.class);
				startActivity(intent);
				getSherlockActivity().finish();
			}

			@Override
			public void onCancel() {
				//Toast.makeText(mContext, "Authorization was canceled", Toast.LENGTH_LONG).show();
				Log.i(TAG, "Authorization was canceled");
				
				loginButton.setVisibility(View.VISIBLE);
				getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
			}
			
			@Override
			public void onError() {
				//Toast.makeText(mContext, "Error getting token", Toast.LENGTH_LONG).show();
				Log.i(TAG, "Authorization error");
				
				loginButton.setVisibility(View.VISIBLE);
				getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
			}
		});
		//mOdnoklassniki.refreshToken(mContext);
	}
	
	private void ensureTokenValidity() {
		boolean hasToken = mOdnoklassniki.hasAccessToken();
		if (hasToken) {
			//new GetCurrentUserTask(getSherlockActivity()).execute();
			mApiThread.queueRequest(TOKEN_CURRENT_USER, new ApiRequest(ApiRequest.METHOD_GET_CURRENT_USER));
		} else {
			// get new token
			mOdnoklassniki.clearTokens(mContext);
			mOdnoklassniki.requestAuthorization(mContext);
		}
	}
	
	public void onGetCurrentUser(String result) {
		if (result.contains("PARAM_SESSION_EXPIRED")) {
			mOdnoklassniki.refreshToken(mContext);
			// TODO: parse real user id
			onGetCurrentUser(getUserId(result));
		} else {
			// token is valid
			//startFeedActivity();
			//Toast.makeText(mContext, "Launch feed", Toast.LENGTH_SHORT).show();
			mCallbacks.onAuthComplete();
			
		}
	}
	
	private String getUserId(String response) {
		return "561704502428"; //me
		//return "564168749025"; //serg
	}
	
	/*
	//<Params, Progress, Result>	
	protected final class GetCurrentUserTask extends AsyncTask<Void, Void, String> {
		private SherlockFragmentActivity activity;
		//private ProgressDialog dialog;
		
		public GetCurrentUserTask(SherlockFragmentActivity activity) {
			this.activity = activity;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// prepare UI before execution
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected String doInBackground(Void... params) {
			String response = null;
			try {
				response = mOdnoklassniki.request("users.getCurrentUser", null, "get");
			} catch (Exception exc) {
				Log.e(TAG, "Failed to get current user info", exc);
			}
			return response;
		}
		
		@Override 
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			onGetCurrentUser(result);
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
		}
		
	}
	*/
	

}
