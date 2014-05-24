package nyaschenko.oki;

import ru.ok.android.sdk.Odnoklassniki;
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


public class MainActivity extends SlidingFragmentActivity
		implements LoginFragment.Callbacks,
				   MenuFragment.Callbacks,
				   FriendsListFragment.Callbacks {
	
	private static final String TAG = "MainActivity";
	
	
	private static final String APP_ID = "572588032";
	private static final String APP_SECRET = "31F3E52DE4ECD8D56AB6FE06";
	private static final String APP_KEY = "CBABACCDCBABABABA";
	private static final String STATE_CURRENT_FRAGMENT = "STATE_CURRENT_FRAGMENT";
	private static final int FRAGMENT_FEED = 0;
	private static final int FRAGMENT_PHOTOS = 1;
	private static final int FRAGMENT_FRIENDS = 2;
	private static final int ACTIVITY_SETTINGS = 3;
	
	private int mCurrentFragment;

	private SlidingMenu menu;
	private static String mCurrentUserId;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Odnoklassniki.createInstance(this, APP_ID, APP_SECRET, APP_KEY);
        while (!Odnoklassniki.hasInstance()) {}
        
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
		outState.putInt(STATE_CURRENT_FRAGMENT, mCurrentFragment);
	}

	private void initFragments(Bundle savedInstanceState) {
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
		menu.setFadeDegree(0.75f);
		menu.setBehindOffset((int) getResources().getDimension(R.dimen.slidingmenu_offset)); //100
		menu.setSlidingEnabled(false);
	}
	
	private void initUIL() {
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
        .cacheInMemory(true)
        .cacheOnDisc(true)
        .showStubImage(R.drawable.solid_white)
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
				.replace(R.id.fragmentContainerMain, new FeedListFragment())
				.commit();
		mCurrentFragment = 0;
		
	}

	@Override
	public void onMenuItemSelected(int index) {
		if (index == mCurrentFragment) {
			getSlidingMenu().showContent();
			return;
		}
		SherlockListFragment newListFragment = null;
		
		switch (index) {
			case FRAGMENT_FEED:
				newListFragment = new FeedListFragment();
				break;
			case FRAGMENT_PHOTOS:
				newListFragment = new PhotosListFragment();
				break;
			case FRAGMENT_FRIENDS:
				newListFragment = new FriendsListFragment();
				break;
			case ACTIVITY_SETTINGS:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return;
			default:
				Log.e(TAG, "Unknown menu option index: " + index);
				return;
		}
		
		mCurrentFragment = index;
		if (newListFragment != null) {
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction tr = fm.beginTransaction();
			tr.replace(R.id.fragmentContainerMain, newListFragment).commit();
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
		tr.replace(R.id.fragmentContainerMain, PhotosListFragment.newInstance(id));
		tr.addToBackStack(null);
		tr.commit();
		mCurrentFragment = 5;
	}

}
