package nyaschenko.oki;

import ru.ok.android.sdk.Odnoklassniki;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class SettingsActivity extends SherlockActivity {
	
	private static final String EXTRA_SHOW = "EXTRA_SHOW";
	
	private Button mExit;
	private Odnoklassniki mOdnoklassniki;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_settings);
		
		mOdnoklassniki = Odnoklassniki.getInstance(getApplicationContext());
		
		mExit = (Button) findViewById(R.id.buttonExit);
		mExit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog();
			}
		});
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(getString(R.string.settings));
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	void showDialog() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(SettingsActivity.this);
		alertDialog.setTitle("Выйти?");
	 
		alertDialog.setMessage("Вы действительно хотите выйти?");
	  
		alertDialog.setPositiveButton("Да", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (mOdnoklassniki.hasAccessToken()) {
					mOdnoklassniki.clearTokens(getApplicationContext());
					Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
					intent.putExtra(EXTRA_SHOW, true);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK |
								    Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startActivity(intent);
					finish();
				}
			}
		});
	  
		alertDialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
	
		alertDialog.show();
	}

}
