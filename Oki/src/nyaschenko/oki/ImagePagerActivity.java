package nyaschenko.oki;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ImagePagerActivity extends SherlockFragmentActivity {
	private static final String TAG = "ImagePagerActivity";
	
	private static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
	
	private ViewPager mViewPager;
	private ArrayList<String> mPhotos;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mViewPager = new ViewPager(this);
		mViewPager.setId(R.id.viewPager);
		setContentView(mViewPager);
		
		Intent intent = getIntent();
		String urls[] = intent.getStringExtra(EXTRA_IMAGE_URL).split("\\$");
		mPhotos = new ArrayList<String>();
		for (String s : urls) {
			mPhotos.add(s);
		}
		
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int index) {
				getSupportActionBar().setTitle(getCountString(index + 1));
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
		
		FragmentManager fm = getSupportFragmentManager();
		mViewPager.setAdapter(new FragmentStatePagerAdapter(fm) {
			
			@Override
			public int getCount() {
				return mPhotos.size();
			}
			
			@Override
			public Fragment getItem(int position) {
				return SinglePhotoFragment.newInstance(mPhotos.get(position));
			}
		});
	
		getSupportActionBar().setTitle(getCountString(1));
	}
	
	private String getCountString(int index) {
		return getString(R.string.photo) + " "
				+ index + " "
				+ getString(R.string.from) + " "
				+ mPhotos.size();
	}

	
}
