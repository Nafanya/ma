package nyaschenko.oki;

import java.util.ArrayList;
import java.util.HashMap;

import nyaschenko.oki.utils.ApiRequest;
import nyaschenko.oki.utils.PhotoItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.ok.android.sdk.Odnoklassniki;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

public class PhotosFragment extends SherlockFragment {
	public static final String TAG = "PhotosFragment";
	public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
	
	private static Context mContext;
	protected Odnoklassniki mOdnoklassniki;
	private Callbacks mCallbacks;
	
	public interface Callbacks {
	}
	
	private String mUserId;
	
	ListView mListView; 
	ArrayList<PhotoItem> mPhotoItems;
	int mListViewCurrentPosition = 0;
	PhotoItemAdapter mAdapter;
	String mAnchor = null;
	boolean mHasMore = true;
	boolean mLoadingMore = false;
	
	ImageLoader loader = ImageLoader.getInstance();
	
	ApiFetcher<String> mApiThread;
	
	private final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
	private final String REQUEST_GET_PHOTOS = "REQUEST_GET_PHOTOS";
	
	private static final String PHOTO_SMALL = "pic50x50";
	private static final String PHOTO_MEDIUM = "pic128x128";
	private static final String PHOTO_LARGE = "pic640x480";
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		mContext = getSherlockActivity();
		mOdnoklassniki = Odnoklassniki.getInstance(getSherlockActivity());
		
		initBackgroundLoader();
	}

	private void initBackgroundLoader() {
		mApiThread = new ApiFetcher<String>(mContext, new Handler());
        mApiThread.setListener(new ApiFetcher.Listener<String>() {

			public void onRequestComplete(String token, String result) {
				Log.i(TAG, "Recieved response for request[" + token + "]: " + result);
				if (isVisible()) {
					parsePhotos(result);
				}
				//onGetCurrentUser(result);
			}
        	
		});
        mApiThread.start();
        mApiThread.getLooper();
        Log.i(TAG, "Background thread started");
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
		if (getArguments() != null) {
			mUserId = getArguments().getString(EXTRA_USER_ID);
			getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.photos));
		} else {
			mUserId = null;
			getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.my_photos));
		}
        View v = inflater.inflate(R.layout.fragment_photos, container, false);
        
        mListView = (ListView) v.findViewById(R.id.listView);
        
        //TODO: check - "remove lags while scrolling"
        boolean pauseOnScroll = false;
        boolean pauseOnFling = false;
        PauseOnScrollListener listener = new PauseOnScrollListener(loader, pauseOnScroll, pauseOnFling) {
        	@Override
        	public void onScroll(AbsListView view, int firstVisibleItem,
        			int visibleItemCount, int totalItemCount) {
        		super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        		
        		final int lastItem = firstVisibleItem + visibleItemCount;
        	       if (lastItem == totalItemCount) {
        	           getUserPhotos();
        	       }
        	}
        };
        mListView.setOnScrollListener(listener);
        mListView.setOnItemClickListener(new OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            Intent intent = new Intent(getSherlockActivity(), ScaleImageViewActivity.class);
	            intent.putExtra(EXTRA_IMAGE_URL, mPhotoItems.get(position).getUrlLarge());
	            startActivity(intent);
	        	//Toast.makeText(getSherlockActivity(), "Image clicked at position: " + position, Toast.LENGTH_LONG).show();
	        }
	    });
        
		mPhotoItems = new ArrayList<PhotoItem>();
		mAdapter = null;
        mListView.setAdapter(null);
        
        return v;
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getUserPhotos();
	}
	
	void getUserPhotos() {
		if (!mHasMore || mLoadingMore) {
			return;
		}
		mLoadingMore = true;
		HashMap<String, String> params = new HashMap<String, String>();
		if (mUserId != null) {
			params.put(ApiRequest.PARAM_USER_ID, mUserId);
		}
		params.put(ApiRequest.PARAM_COUNT, "100");
		if (mAnchor != null) {
			params.put("anchor", mAnchor);
		}
		ApiRequest request = new ApiRequest(ApiRequest.METHOD_GET_PHOTOS, params);
		mApiThread.queueRequest(REQUEST_GET_PHOTOS, request);
	}

	public static PhotosFragment newInstance(String userId) {
		Bundle args = new Bundle();
		args.putString(EXTRA_USER_ID, userId);
		PhotosFragment fragment = new PhotosFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	private class PhotoItemAdapter extends ArrayAdapter<PhotoItem> {
    	
    	public PhotoItemAdapter(ArrayList<PhotoItem> items) {
    		super(getSherlockActivity(), 0, items);
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		if (convertView == null) {
    			convertView = getActivity().getLayoutInflater()
    					.inflate(R.layout.photo_item, parent, false);
    		}
    		
    		ImageView imageView = (ImageView)convertView
    				.findViewById(R.id.photo_item_imageView);
    		
    		TextView marks = (TextView) convertView
    				.findViewById(R.id.photo_item_likeCount);
    		TextView text = (TextView) convertView
    				.findViewById(R.id.photo_item_text);
    		TextView comments = (TextView) convertView
    				.findViewById(R.id.photo_item_commentCount);
    		
    		PhotoItem item = getItem(position);
    		
    		marks.setText(item.getMarksCount());
    		text.setText(item.getText());
    		comments.setText(item.getCommentsCount());
    		
    		loader.displayImage(item.getUrlLarge(), imageView);
    		return convertView;
    	}
    }
	
	private void parsePhotos(String response) {
		ArrayList<PhotoItem> items = new ArrayList<PhotoItem>();
		try {
			HashMap<String, String> params = new HashMap<String, String>();
			JSONObject json = new JSONObject(response);
			
			boolean error = json.has("error_code");
			if (error) {
				mHasMore = false;
				return;
			}
			
			mHasMore = json.getBoolean("hasMore");
			if (mHasMore) {
				mAnchor = json.getString("anchor");
			}
			
			JSONArray photos = json.getJSONArray("photos");
			for (int i = 0; i < photos.length(); i++) {
				try {
					JSONObject photo = photos.getJSONObject(i);
					String urlSmall = photo.getString(PHOTO_SMALL);
					String urlMedium = photo.getString(PHOTO_MEDIUM);
					String urlLarge = photo.getString(PHOTO_LARGE);
					String text = photo.getString("text");
					String commentsCount = photo.getString("comments_count");
					String markCount = photo.getString("mark_count");
					
					PhotoItem item = new PhotoItem();
					item.setUrlSmall(urlSmall);
					item.setUrlMedium(urlMedium);
					item.setUrlLarge(urlLarge);
					item.setCommentsCount(commentsCount);
					item.setMarksCount(markCount);
					item.setText(text);
					
					items.add(item);
					
				} catch (JSONException e) {
					Log.e(TAG, "JSON error: " + e.toString());
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "JSON parse error");
		}
		mPhotoItems.addAll(items);
		if (mAdapter == null) {
			mAdapter = new PhotoItemAdapter(mPhotoItems);
			mListView.setAdapter(mAdapter);
		} else {
			mAdapter.notifyDataSetChanged();
		}
		mLoadingMore = false;
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
	
	@Override
	public void onDestroy() {
		mApiThread.quit();
		mContext = null;
		super.onDestroy();
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mApiThread.clearQueue();
	}
	
}