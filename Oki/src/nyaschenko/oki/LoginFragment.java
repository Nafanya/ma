package nyaschenko.oki;

import nyaschenko.oki.utils.ApiRequest;
import ru.ok.android.sdk.OkTokenRequestListener;
import ru.ok.android.sdk.util.OkScope;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;


public class LoginFragment extends ThreadFragment {
	public static final String TAG = "LoginFragment";

	private static final String EXTRA_LOGGED_IN = "EXTRA_LOGGED_IN";
	private static final String REQUEST_CURRENT_USER = "REQUEST_CURRENT_USER";
	//private static final String PERMISSIONS = OkScope.VALUABLE_ACCESS + ";" + OkScope.PHOTO_CONTENT;
	private static final String PERMISSIONS = OkScope.VALUABLE_ACCESS + ";" + OkScope.PHOTO_CONTENT + ";" + "LIKE";
	
	Button loginButton;
	
	private Callbacks mCallbacks;
	
	public interface Callbacks {
		void onAuthComplete();
		void onGetCurrentUser(String userId);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		initOk();
		
		pBackgroundThread.setListener(new ApiFetcher.Listener<String>() {

			@Override
			public void onRequestComplete(String token, String result) {
				Log.i(TAG, "Recieved response for request[" + token + "]: " + result);
				
				onGetCurrentUser(result);
			}
        	
		});
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        loginButton = (Button) v.findViewById(R.id.buttonLogin);
        loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View view) {
				pOdnoklassniki.requestAuthorization(getSherlockActivity(), false, PERMISSIONS); 
			}
		});
        
        return v;
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ensureTokenValidity();
	}
	
	private void initOk() {
		pOdnoklassniki.setTokenRequestListener(new OkTokenRequestListener() {
			@Override
			public void onSuccess(String accessToken) {
				Log.i(TAG, "Recieved new token: " + accessToken);
				
				Intent intent = new Intent(getSherlockActivity(), MainActivity.class);
				intent.putExtra(EXTRA_LOGGED_IN, true);
				startActivity(intent);
				getSherlockActivity().finish();
				
				//mCallbacks.onAuthComplete();
				//((Callbacks) getSherlockActivity()).onAuthComplete();
			}

			@Override
			public void onCancel() {
				Log.i(TAG, "Authorization was canceled");
				
				loginButton.setVisibility(View.VISIBLE);
				getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
			}
			
			@Override
			public void onError() {
				Log.i(TAG, "Authorization error");
				
				loginButton.setVisibility(View.VISIBLE);
				getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
			}
		});
	}

	private void ensureTokenValidity() {
		boolean hasToken = pOdnoklassniki.hasAccessToken();
		if (hasToken) {
			pBackgroundThread.queueRequest(REQUEST_CURRENT_USER, new ApiRequest(ApiRequest.METHOD_GET_CURRENT_USER));
		} else {
			//pOdnoklassniki.clearTokens(pContext);
			pOdnoklassniki.requestAuthorization(pContext);
		}
	}
	
	public void onGetCurrentUser(String result) {
		pOdnoklassniki.refreshToken(getSherlockActivity());
		/*
		if (result.contains("PARAM_SESSION_EXPIRED") || true) {
			pOdnoklassniki.refreshToken(getSherlockActivity());
		} else {
			((Callbacks) getSherlockActivity()).onAuthComplete();
		}
		*/
	}
	
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
}
