package nyaschenko.oki;

import ru.ok.android.sdk.Odnoklassniki;
import ru.ok.android.sdk.OkTokenRequestListener;
import ru.ok.android.sdk.util.OkScope;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;

public class LoginActivity extends SherlockActivity 
		implements OkTokenRequestListener {
	
	private static final String EXTRA_SHOW = "EXTRA_SHOW";
	private static final String APP_ID = "572588032";
	private static final String APP_SECRET = "31F3E52DE4ECD8D56AB6FE06";
	private static final String APP_KEY = "CBABACCDCBABABABA";
	private static final String PERMISSIONS =
			OkScope.VALUABLE_ACCESS + ";" + OkScope.PHOTO_CONTENT + ";" + OkScope.SET_STATUS;
	private Odnoklassniki mOdnoklassniki;
	private Button loginButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminateVisibility(true);
		
		setContentView(R.layout.fragment_login);
		
		mOdnoklassniki = Odnoklassniki.createInstance(getApplicationContext(), APP_ID, APP_SECRET, APP_KEY);
		mOdnoklassniki.setTokenRequestListener(this);
		
		loginButton = (Button) findViewById(R.id.buttonLogin);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View view) {
				if (!checkInternet()) {
					return;
				}
				setSupportProgressBarIndeterminateVisibility(true);
				mOdnoklassniki.requestAuthorization(LoginActivity.this, false, PERMISSIONS); 
			}
		});
		
		Intent intent = getIntent();
		boolean showButton = intent.getBooleanExtra(EXTRA_SHOW, false);
		if (showButton) {
			setSupportProgressBarIndeterminateVisibility(false);
			loginButton.setVisibility(View.VISIBLE);
			return;
		}
		
		checkInternet();
		
		try {
			mOdnoklassniki.refreshToken(this);
		} catch (Exception e) {
			mOdnoklassniki.requestAuthorization(LoginActivity.this, false, PERMISSIONS);
		}
		
	}
	
	private boolean checkInternet() {
		if (!isInternetAvailable()) {
			Toast.makeText(LoginActivity.this, getString(R.string.check_internet_connection), Toast.LENGTH_SHORT).show();
			loginButton.setVisibility(View.VISIBLE);
			return false;
		}
		return true;
	}

	private boolean isInternetAvailable() {
		ConnectivityManager cm =
		        (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}
	
	@Override
	public void onSuccess(String accessToken) {
		Intent intent = new Intent(LoginActivity.this, MainActivity.class);
		setSupportProgressBarIndeterminateVisibility(false);
		startActivity(intent);
		finish();
	}

	@Override
	public void onError() {
		Toast.makeText(this, getString(R.string.error_during_connection), Toast.LENGTH_SHORT).show();
		loginButton.setVisibility(View.VISIBLE);
		setSupportProgressBarIndeterminateVisibility(false);
	}

	@Override
	public void onCancel() {
		loginButton.setVisibility(View.VISIBLE);
		setSupportProgressBarIndeterminateVisibility(false);
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

}
