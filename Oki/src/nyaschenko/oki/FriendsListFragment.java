package nyaschenko.oki;

import java.util.ArrayList;
import java.util.HashMap;

import nyaschenko.oki.utils.ApiRequest;
import nyaschenko.oki.utils.FriendItem;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

public class FriendsListFragment extends ThreadListFragment {
	private static final String TAG = "FriendsListFragment";
	private static final String REQUEST_GET_FRIENDS = "REQUEST_GET_FRIENDS";
	private static final String REQUEST_GET_USER_INFO = "REQUEST_GET_USER_INFO";
	
	public interface Callbacks {
		void onFriendSelected(String id);
	}
	
	private Callbacks mCallbacks;
	
	ArrayList<String> mFriendIds;
	ArrayList<FriendItem> mFriends;
	int mFriendsInfoLoaded = 0;
	
	boolean mHasMore = true;
	boolean mLoadingMore = false;
	boolean mFriendIdsLoaded = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		mFriends = new ArrayList<FriendItem>();
		mFriendIds = new ArrayList<String>();
		setListAdapter(null);
		
		pBackgroundThread.setListener(new ApiFetcher.Listener<String>() {
			
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
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		getListView().setDividerHeight(getResources().getDimensionPixelOffset(R.dimen.listview_divider_height));
		
		boolean pauseOnScroll = false;
		boolean pauseOnFling = false;
		PauseOnScrollListener listener = new PauseOnScrollListener(pLoader, pauseOnScroll, pauseOnFling) {
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
				
				final int lastItem = firstVisibleItem + visibleItemCount;
			       if (lastItem == totalItemCount) {
			    	   if (mFriendIdsLoaded) {
			    		   getUserInfo();
			    	   }
			       }
			}
		};
		getListView().setOnScrollListener(listener);
		getListView().setOnItemClickListener(new OnItemClickListener() {
		    @Override
		    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		    	mCallbacks.onFriendSelected(mFriendIds.get(position));
		    }
		});
		
		getFriends();
	}
	
	private class FriendAdapter extends ArrayAdapter<FriendItem> {
		
		private DisplayImageOptions options;

    	public FriendAdapter(ArrayList<FriendItem> items) {
    		super(getSherlockActivity(), 0, items);
    		
    		//TODO
    		options = new DisplayImageOptions.Builder()
			.showStubImage(R.drawable.ic_action_person)
			.cacheInMemory(true)
			.cacheOnDisc(true)
			//.considerExifParams(true)
			.displayer(new RoundedBitmapDisplayer(25))
			.build();
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		if (convertView == null) {
    			convertView = getActivity().getLayoutInflater()
    					.inflate(R.layout.friend_item, null);
    		}
    		
    		ImageView imageView = (ImageView) convertView.findViewById(R.id.friendItem_imageView);
    		TextView name = (TextView) convertView.findViewById(R.id.friendItem_textView);
    		
    		FriendItem item = getItem(position);
    		
    		name.setText(item.getName());
    		String photoUrl = item.getPhoto();
    		if (photoUrl.contains("stub")) {
    			photoUrl = "drawable://" + R.drawable.ic_action_person;
    		}
    		pLoader.displayImage(photoUrl, imageView, options);
    		return convertView;
    	}
    }
	
	private void getFriends() {
		if (!mFriendIdsLoaded) {
			ApiRequest request = new ApiRequest("friends.get");
			pBackgroundThread.queueRequest(REQUEST_GET_FRIENDS, request);
		}
	}
	
	private void parseFriends(String response) {
		Log.i(TAG, response);
		JSONArray json = (JSONArray) JSONValue.parse(response);
		for (int i = 0; i < json.size(); i++) {
			mFriendIds.add((String) json.get(i));
		}
		mFriendIds.add("561704502482");
		mFriendIdsLoaded = true;
		getUserInfo();
	}
	
	private void getUserInfo() {
		if (!mHasMore || mLoadingMore) {
			return;
		}
		showFooter();
		pFooter.setVisibility(View.VISIBLE);
		mLoadingMore = true;
		HashMap<String, String> params = new HashMap<String, String>();
		StringBuilder b = new StringBuilder();
		int to = Math.min(mFriendsInfoLoaded + 100, mFriendIds.size());
		int count = 0;
		for (int i = mFriendsInfoLoaded; i < to; i++) {
			count++;
			b.append(mFriendIds.get(i));
			if (i + 1 < Math.min(100, mFriendIds.size())) {
				b.append(",");
			}
		}
		if (count == 0) {
			mHasMore = false;
			mLoadingMore = false;
			hideFooter();
			pFooter.setVisibility(View.GONE);
			return;
		}
		mFriendsInfoLoaded += count;
		params.put("uids", b.toString());
		params.put(ApiRequest.PARAM_COUNT, Integer.toString(count));
		params.put("fields", "first_name,last_name,pic128x128");
		ApiRequest request = new ApiRequest("users.getInfo", params);
		pBackgroundThread.queueRequest(REQUEST_GET_USER_INFO, request);
	}
	
	private void parseUserInfo(String response) {
		ArrayList<FriendItem> items = new ArrayList<FriendItem>();
		JSONArray json = null;
		json = (JSONArray) JSONValue.parse(response);
		for (int i = 0; i < json.size(); i++) {
			JSONObject friend = (JSONObject) json.get(i);
			String uid = (String) friend.get("uid");
			String firstName = (String) friend.get("first_name");
			String lastName = (String) friend.get("last_name");
			String pic = (String) friend.get("pic128x128");
			FriendItem item = new FriendItem(uid, firstName, lastName, pic);
			items.add(item);
		}
		hideFooter();
		pFooter.setVisibility(View.GONE);
		setupAdapter();
		if (!items.isEmpty()) {
			mFriends.addAll(items);
			pAdapter.notifyDataSetChanged();
		}
		mLoadingMore = false;
	}

	@Override
	public BaseAdapter getAdapter() {
		return new FriendAdapter(mFriends);
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
