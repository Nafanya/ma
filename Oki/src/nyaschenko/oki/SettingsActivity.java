package nyaschenko.oki;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;

public class SettingsActivity extends SherlockActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_settings);
		
		getSupportActionBar().setTitle(getString(R.string.settings));
	}

}
