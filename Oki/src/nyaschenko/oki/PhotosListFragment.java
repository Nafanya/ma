package nyaschenko.oki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import nyaschenko.oki.utils.ApiRequest;
import nyaschenko.oki.utils.PhotoItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;


public class PhotosListFragment extends ThreadListFragment {
	private static final String TAG = "PhotosListFragment";
	private static final String EXTRA_COMMENT_FOCUS = "EXTRA_COMMENT_FOCUS";
	private static final String EXTRA_USER_ID = "EXTRA_USER_ID";
	private static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
	private static final String REQUEST_GET_PHOTOS = "REQUEST_GET_PHOTOS";
	private static final String REQUEST_LIKE = "REQUEST_LIKE";
	
	
	// TODO: move to PhotoItem
	private static final String PHOTO_SMALL = "pic50x50";
	private static final String PHOTO_MEDIUM = "pic128x128";
	private static final String PHOTO_LARGE = "pic640x480";
	
	private ArrayList<PhotoItem> mPhotos;
	private boolean mHasMore = true;
	private boolean mLoadingMore = false;
	private boolean mLikingNow = false;
	private int mLikeIndex;
	private String mAnchor = null;
	private String mCurrentUserId = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		mPhotos = new ArrayList<PhotoItem>();
		setListAdapter(null);
		
		pBackgroundThread.setListener(new ApiFetcher.Listener<String>() {
			
			public void onRequestComplete(String token, String result) {
				Log.i(TAG, "Recieved response for request[" + token + "]: " + result);
				if (isVisible()) {
					if (token.equals(REQUEST_GET_PHOTOS)) {
						parsePhotos(result);
					} else if (token.equals(REQUEST_LIKE)) {
						parseLike(result);
					}
				}
			}
			
		});
	}

	@Override
	public BaseAdapter getAdapter() {
		return new PhotoAdapter(mPhotos);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (getArguments() != null) {
			mCurrentUserId = getArguments().getString(EXTRA_USER_ID);
			getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.photos));
		} else {
			mCurrentUserId = null;
			getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.my_photos));
		}
		
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		getListView().setFooterDividersEnabled(false);
		
		boolean pauseOnScroll = false;
        boolean pauseOnFling = false;
        PauseOnScrollListener listener = new PauseOnScrollListener(pLoader, pauseOnScroll, pauseOnFling) {
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
        
        getListView().setOnScrollListener(listener);
        /*
        getListView().setOnItemClickListener(new OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            Intent intent = new Intent(getSherlockActivity(), ScaleImageViewActivity.class);
	            intent.putExtra(EXTRA_IMAGE_URL, mPhotos.get(position).getUrlLarge());
	            startActivity(intent);
	        }
	    });*/
        /*
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	public void onItemClick(AdapterView<?> list, View v, int pos, long id) {
        		int viewId = v.getId();
        		switch (viewId) {
        		case R.id.photo_item_imageView:
        			Toast.makeText(pContext, "Photo at pos: " + pos, Toast.LENGTH_SHORT).show();
        			break;
        		case R.id.buttonLike:
        			Toast.makeText(pContext, "Like at pos: " + pos, Toast.LENGTH_SHORT).show();
        			break;
        		case R.id.buttonComment:
        			Toast.makeText(pContext, "Comment at pos: " + pos, Toast.LENGTH_SHORT).show();
        			break;
        		}
        	}
        });*/
        
	}
	
	public static PhotosListFragment newInstance(String userId) {
		Bundle args = new Bundle();
		args.putString(EXTRA_USER_ID, userId);
		PhotosListFragment fragment = new PhotosListFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	private class PhotoAdapter extends ArrayAdapter<PhotoItem> {
		private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();
    	
    	public PhotoAdapter(ArrayList<PhotoItem> items) {
    		super(getSherlockActivity(), 0, items);
    	}
    	
    	@Override
    	public View getView(final int position, View convertView, ViewGroup parent) {
    		if (convertView == null) {
    			convertView = getActivity().getLayoutInflater()
    					.inflate(R.layout.photo_item_like, null);
    		}
    		
    		ImageView imageView = (ImageView) convertView.findViewById(R.id.photo_item_imageView);
    		Button like = (Button) convertView.findViewById(R.id.buttonLike);
    		Button comment = (Button) convertView.findViewById(R.id.buttonComment);
    		TextView text = (TextView) convertView.findViewById(R.id.photo_item_text);
    		
    		PhotoItem item = getItem(position);
    		
    		if (item.isLiked()) {
    			like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_good, 0, 0, 0);
    		} else {
    			like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_good, 0, 0, 0);
    		}
    		
    		imageView.setTag(item);
			text.setTag(item);
			like.setTag(item);
			comment.setTag(item);
    		
    		like.setText(item.getMarksCount());
    		text.setText(item.getText());
    		comment.setText(item.getCommentsCount());
    		
    		imageView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
		        	Intent intent = new Intent(getSherlockActivity(), ImagePagerActivity.class);
		        	intent.putExtra(EXTRA_IMAGE_URL, mPhotos.get(position).getUrlLarge());
		        	startActivity(intent);
					/*
					Intent intent = new Intent(getSherlockActivity(), ScaleImageViewActivity.class);
		            intent.putExtra(EXTRA_IMAGE_URL, mPhotos.get(position).getUrlLarge());
		            startActivity(intent);
		            */
				}
			});
			like.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					//Toast.makeText(getSherlockActivity(), "Like at pos: " + position, Toast.LENGTH_SHORT).show();
					addLike(mPhotos.get(position).getId(), position);
				}
			});
			comment.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getSherlockActivity(), ScaleImageViewActivity.class);
		            intent.putExtra(EXTRA_IMAGE_URL, mPhotos.get(position).getUrlLarge());
		            intent.putExtra(EXTRA_COMMENT_FOCUS, true);
		            startActivity(intent);
				}
			});
    		
    		pLoader.displayImage(item.getUrlLarge(), imageView, null, animateFirstListener);
    		return convertView;
    	}
    }
	
	private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {
		static final Set<String> displayedImages =
				Collections.synchronizedSet(new HashSet<String>());
	
		@Override
		public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
			if (loadedImage != null) {
				ImageView imageView = (ImageView) view;
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) {
					FadeInBitmapDisplayer.animate(imageView, 500);
					displayedImages.add(imageUri);
				}
			}
		}
	}
	
	private void getUserPhotos() {
		if (!mHasMore || mLoadingMore) {
			return;
		}
		showFooter();
		mLoadingMore = true;
		HashMap<String, String> params = new HashMap<String, String>();
		if (mCurrentUserId != null) {
			params.put(ApiRequest.PARAM_USER_ID, mCurrentUserId);
		}
		params.put(ApiRequest.PARAM_COUNT, "100");
		if (mAnchor != null) {
			params.put("anchor", mAnchor);
		}
		ApiRequest request = new ApiRequest(ApiRequest.METHOD_GET_PHOTOS, params);
		pBackgroundThread.queueRequest(REQUEST_GET_PHOTOS, request);
	}
	
	private void parsePhotos(String response) {
		ArrayList<PhotoItem> items = new ArrayList<PhotoItem>();
		try {
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
					String id = photo.getString("id");
					String urlSmall = photo.getString(PHOTO_SMALL);
					String urlMedium = photo.getString(PHOTO_MEDIUM);
					String urlLarge = photo.getString(PHOTO_LARGE);
					String text;
					try {
						text = photo.getString("text");
					} catch (JSONException e) {
						text = "";
					}
					String commentsCount = photo.getString("comments_count");
					String markCount = photo.getString("mark_count");
					
					PhotoItem item = new PhotoItem();
					item.setId(id);
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
		hideFooter();
		setupAdapter();
		if (!items.isEmpty()) {
			mPhotos.addAll(items);
			pAdapter.notifyDataSetChanged();
		}
		mLoadingMore = false;
	}
	
	private void addLike(String id, int position) {
		if (!mLikingNow) {
			mLikingNow = true;
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("photo_id", id);
			ApiRequest request = new ApiRequest("photos.addPhotoLike", params);
			pBackgroundThread.queueRequest(REQUEST_LIKE, request);
		}
	}
	
	private void parseLike(String response) {
		if (!response.contains("error_code")) {
			//"error.internalLike.selfLike"
			int marks = Integer.parseInt(mPhotos.get(mLikeIndex).getMarksCount()) + 1; 
			mPhotos.get(mLikeIndex).setMarksCount(Integer.toString(marks));
			mPhotos.get(mLikeIndex).setLiked(true);
			pAdapter.notifyDataSetChanged();
		}
		mLikingNow = false;
	}
}
