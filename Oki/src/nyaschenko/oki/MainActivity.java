package nyaschenko.oki;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Window;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;


public class MainActivity extends SlidingFragmentActivity
		implements LoginFragment.Callbacks,
				   PhotosFragment.Callbacks,
				   MenuFragment.Callbacks,
				   FriendsFragment.Callbacks {
	
	public static final String TAG = "MainActivity";

	private SlidingMenu menu;
	// TODO: extract from getCurrentUser response
	private static String mCurrentUserId = "561704502428";
	private static int mCurrentFragment;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminateVisibility(true);
        
        setContentView(R.layout.fragment_main);
        setBehindContentView(R.layout.fragment_menu);
        
        initFragments(savedInstanceState);
        initSlidingMenu();
        initUIL();
    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//outState.putInt(STATE_CURRENT_FRAGMENT, mCurrentFragment);
	}

	private void initFragments(Bundle savedInstanceState) {
		/*
		if (savedInstanceState == null) {
			menuFragment = new MenuFragment();
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.menuFragmentContainer, menuFragment)
					.commit();
		} else {
			menuFragment = (MenuFragment) getSupportFragmentManager()
					.findFragmentById(R.id.menuFragmentContainer);
		}*/
		
		FragmentManager manager = getSupportFragmentManager();
        SherlockFragment mainFragment = 
        		(SherlockFragment) manager.findFragmentById(R.id.fragmentContainerMain);
        SherlockListFragment menuFragment = 
        			(SherlockListFragment) manager.findFragmentById(R.id.fragmentContainerMenu);

        if (mainFragment == null) {
        	mainFragment = new LoginFragment();
            manager.beginTransaction()
                	.add(R.id.fragmentContainerMain, mainFragment)
                	.commit();
        }
        
        if (menuFragment == null) {
        	menuFragment = new MenuFragment();
        	manager.beginTransaction()
        			.add(R.id.fragmentContainerMenu, menuFragment)
        			.commit();
        }	
	}
	
	private void initSlidingMenu() {
		menu = getSlidingMenu();
		menu.setMode(SlidingMenu.LEFT);
		menu.setTouchModeBehind(SlidingMenu.TOUCHMODE_MARGIN);
		//menu.setShadowDrawable(R.drawable.slidemenu_shadowgradient);
		menu.setShadowWidth(50); //15
		menu.setFadeDegree(0.75f); //0.0f
		//menu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
		menu.setBehindOffset((int) getResources().getDimension(R.dimen.slidingmenu_offset)); //100
		menu.setSlidingEnabled(false);
	}
	
	private void initUIL() {
		/*
		 * TODO: check if display options are local or global for every .displayImage(...) call
		 * see here: https://github.com/nostra13/Android-Universal-Image-Loader/wiki/Display-Options
		 * 
		 */
		
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
        .cacheInMemory(true)
        .cacheOnDisc(true)
        //.displayer(new FadeInBitmapDisplayer(250))
        .build();
		
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
		.memoryCacheExtraOptions(640, 480) //large picture is 640x480
        .defaultDisplayImageOptions(defaultOptions)
        .build();
		
		ImageLoader.getInstance().init(config);
	}

	@Override
	public void onAuthComplete() {
		getSlidingMenu().setSlidingEnabled(true);
		setSupportProgressBarIndeterminateVisibility(false);
		
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction()
				.replace(R.id.fragmentContainerMain, new FeedFragment())
				.commitAllowingStateLoss();
		mCurrentFragment = 0;
	}

	@Override
	public void onMenuItemSelected(int index) {
		if (index == mCurrentFragment) {
			getSlidingMenu().showContent();
			return;
		}
		SherlockFragment newFragment = null;
		//newFragment = PhotosFragment.newInstance(mCurrentUserId);
		
		switch (index) {
			case 0:
				newFragment = new FeedFragment();
				break;
			case 1:
				// TODO: add id in Bundle
				//PhotosFragment f = PhotosFragment.newInstance(mCurrentUserId);
				newFragment = new PhotosFragment();
				//newFragment = new FeedFragment();
				break;
			case 2:
				newFragment = new FriendsFragment();
				break;
			case 3:
				// TODO: Settings Activity
				//newFragment = PhotosFragment.newInstance("564168749025");
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				//newFragment = new FeedFragment();
				//break;
				return;
			default:
				Log.e(TAG, "Unknown menu option index: " + index);
				return;
		}
		
		mCurrentFragment = index;
		if (newFragment != null) {
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction tr = fm.beginTransaction();
			tr.replace(R.id.fragmentContainerMain, newFragment).commit();
		}
		getSlidingMenu().showContent();
	}

	@Override
	public void onGetCurrentUser(String userId) {
		mCurrentUserId = userId;
	}

	@Override
	public void onFriendSelected(String id) {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction tr = fm.beginTransaction();
		tr.replace(R.id.fragmentContainerMain, PhotosFragment.newInstance(id));
		//tr.addToBackStack(null);
		tr.commit();
		mCurrentFragment = 5;
	}

}
