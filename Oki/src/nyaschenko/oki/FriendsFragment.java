package nyaschenko.oki;

import java.util.ArrayList;
import java.util.HashMap;

import nyaschenko.oki.utils.ApiRequest;
import nyaschenko.oki.utils.FriendItem;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import ru.ok.android.sdk.Odnoklassniki;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

public class FriendsFragment extends SherlockFragment {
	public static final String TAG = "FriendsFragment";
	
	private static final String REQUEST_GET_FRIENDS = "REQUEST_GET_FRIENDS";
	private static final String REQUEST_GET_USER_INFO = "REQUEST_GET_USER_INFO";
	
	private Callbacks mCallbacks;
	
	public interface Callbacks {
		void onFriendSelected(String id);
	}
	
	private static Context mContext;
	ImageLoader loader = ImageLoader.getInstance();
	ApiFetcher<String> mApiThread;
	
	protected Odnoklassniki mOdnoklassniki;
	
	ArrayList<String> mFriendsIds = new ArrayList<String>();
	int mFriendsInfoLoaded = 0;
	
	ListView mListView; 
	ArrayList<FriendItem> mFriendItems;
	FriendsItemAdapter mAdapter = null;
	String mAnchor = null;
	boolean mHasMore = true;
	boolean mLoadingMore = false;
	boolean mFriendsIdsLoaded = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.friends));
		mContext = getSherlockActivity();
		mOdnoklassniki = Odnoklassniki.getInstance(mContext);
		
		initBackgroundLoader();
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_friends, container, false);
		
		mListView = (ListView) v.findViewById(R.id.listView);
        
        boolean pauseOnScroll = false;
        boolean pauseOnFling = false;
        PauseOnScrollListener listener = new PauseOnScrollListener(loader, pauseOnScroll, pauseOnFling) {
        	@Override
        	public void onScroll(AbsListView view, int firstVisibleItem,
        			int visibleItemCount, int totalItemCount) {
        		super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        		
        		final int lastItem = firstVisibleItem + visibleItemCount;
        	       if (lastItem == totalItemCount) {
        	    	   if (mFriendsIdsLoaded) {
        	    		   getUserInfo();
        	    	   }
        	       }
        	}
        };
        mListView.setOnScrollListener(listener);
        mListView.setOnItemClickListener(new OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	        	mCallbacks.onFriendSelected(mFriendsIds.get(position));
	        }
	    });
        
		mFriendItems = new ArrayList<FriendItem>();
		mAdapter = null;
        mListView.setAdapter(null);
		
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getFriends();
	}
	
	private void getFriends() {
		if (!mHasMore || mLoadingMore) {
			return;
		}
		mLoadingMore = true;
		ApiRequest request = new ApiRequest("friends.get");
		mApiThread.queueRequest(REQUEST_GET_FRIENDS, request);
	}
	
	private void getUserInfo() {
		if (!mHasMore || mLoadingMore) {
			return;
		}
		mLoadingMore = true;
		HashMap<String, String> params = new HashMap<String, String>();
		StringBuilder b = new StringBuilder();
		int to = Math.min(mFriendsInfoLoaded + 100, mFriendsIds.size());
		int count = 0;
		for (int i = mFriendsInfoLoaded; i < to; i++) {
			count++;
			b.append(mFriendsIds.get(i));
			if (i + 1 < Math.min(100, mFriendsIds.size())) {
				b.append(",");
			}
		}
		mFriendsInfoLoaded += count;
		if (count == 0) {
			mHasMore = false;
			mLoadingMore = false;
			return;
		}
		params.put("uids", b.toString());
		params.put("count", Integer.toString(count));
		params.put("fields", "first_name,last_name,pic128x128");
		ApiRequest request = new ApiRequest("users.getInfo", params);
		mApiThread.queueRequest(REQUEST_GET_USER_INFO, request);
	}

	private void initBackgroundLoader() {
		mApiThread = new ApiFetcher<String>(mContext, new Handler());
        mApiThread.setListener(new ApiFetcher.Listener<String>() {

			public void onRequestComplete(String token, String result) {
				Log.i(TAG, "Recieved response for request[" + token + "]: " + result);
				if (isVisible()) {
					if (token.equals(REQUEST_GET_FRIENDS)) {
						parseFriends(result);
					} else if (token.equals(REQUEST_GET_USER_INFO)) {
						parseUserInfo(result);
					}
				}
			}
        	
		});
        mApiThread.start();
        mApiThread.getLooper();
        Log.i(TAG, "Background thread started");
	}
	
	private void parseUserInfo(String response) {
		JSONArray json = null;
		json = (JSONArray) JSONValue.parse(response);
		for (int i = 0; i < json.size(); i++) {
			JSONObject friend = (JSONObject) json.get(i);
			String uid = (String) friend.get("uid");
			String firstName = (String) friend.get("first_name");
			String lastName = (String) friend.get("last_name");
			String pic = (String) friend.get("pic128x128");
			FriendItem item = new FriendItem(uid, firstName, lastName, pic);
			mFriendItems.add(item);
		}
		
        if (mAdapter == null) {
			mAdapter = new FriendsItemAdapter(mFriendItems);
			mListView.setAdapter(mAdapter);
		} else {
			mAdapter.notifyDataSetChanged();
		}
		mLoadingMore = false;
		
	}

	private void parseFriends(String response) {
		if (mFriendsIdsLoaded) {
			return;
		}
		Log.i(TAG, response);
		JSONArray json = null;
		json = (JSONArray) JSONValue.parse(response);
		for (int i = 0; i < json.size(); i++) {
			mFriendsIds.add((String) json.get(i));
		}
		mLoadingMore = false;
		mFriendsIdsLoaded = true;
		getUserInfo();
	}
	
	private class FriendsItemAdapter extends ArrayAdapter<FriendItem> {
    	
    	public FriendsItemAdapter(ArrayList<FriendItem> items) {
    		super(getSherlockActivity(), 0, items);
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		if (convertView == null) {
    			convertView = getActivity().getLayoutInflater()
    					.inflate(R.layout.friend_item, parent, false);
    		}
    		
    		ImageView imageView = (ImageView) convertView.findViewById(R.id.friendItem_imageView);
    		TextView name = (TextView) convertView.findViewById(R.id.friendItem_textView);
    		
    		FriendItem item = getItem(position);
    		
    		name.setText(item.getName());
    		loader.displayImage(item.getPhoto(), imageView);
    		return convertView;
    	}
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
