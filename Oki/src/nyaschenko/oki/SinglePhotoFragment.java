package nyaschenko.oki;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

public class SinglePhotoFragment extends SherlockFragment {
	private static final String TAG = "SinglePhotoFragment";
	
	private static final String EXTRA_URL = "EXTRA_URL";
	
	private ImageLoader mLoader;
	private ScaleImageView mScaleImageView;
	private String mUrl = null;
	private DisplayImageOptions options;
	private ProgressBar mProgressBar;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		if (args != null) {
			mUrl = (String) getArguments().get(EXTRA_URL);
		}
		mLoader = ImageLoader.getInstance();
		options = new DisplayImageOptions.Builder()
		.resetViewBeforeLoading(true)
		.cacheInMemory(true)
		.cacheOnDisc(true)
		.imageScaleType(ImageScaleType.EXACTLY)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.displayer(new FadeInBitmapDisplayer(300))
		.build();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_scaled_photo, container, false);
		
		mProgressBar = (ProgressBar) v.findViewById(R.id.loading);
		mScaleImageView = (ScaleImageView) v.findViewById(R.id.scaleImageView);
		mLoader.displayImage(mUrl, mScaleImageView, options, new SimpleImageLoadingListener() {
			@Override
			public void onLoadingStarted(String imageUri, View view) {
				mProgressBar.setVisibility(View.VISIBLE);
			}

			@Override
			public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
				mProgressBar.setVisibility(View.GONE);
			}
		});
		
		return v;
	}
	
	static SherlockFragment newInstance(String url) {
		Bundle args = new Bundle();
		args.putString(EXTRA_URL, url);
		SinglePhotoFragment fragment = new SinglePhotoFragment();
		fragment.setArguments(args);
		return fragment;
	}
}
