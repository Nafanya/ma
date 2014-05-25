package nyaschenko.oki;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockActivity;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ScaleImageViewActivity extends SherlockActivity {
	
	private final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL"; 
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.scaledimage_activity);
        
        getSupportActionBar().setTitle(getString(R.string.photo));
        
        ImageLoader loader = ImageLoader.getInstance();
        Intent intent = getIntent();
        String imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL);
        ImageView imageView = (ImageView)findViewById(R.id.scaleImageView);
        loader.displayImage(imageUrl, imageView);
    }
}