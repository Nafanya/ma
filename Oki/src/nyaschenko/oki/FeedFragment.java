package nyaschenko.oki;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import nyaschenko.oki.utils.AlbumStorageDirFactory;
import nyaschenko.oki.utils.ApiRequest;
import nyaschenko.oki.utils.BaseAlbumDirFactory;
import nyaschenko.oki.utils.FeedItem;
import nyaschenko.oki.utils.FroyoAlbumDirFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import ru.ok.android.sdk.Odnoklassniki;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
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
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.aviary.android.feather.FeatherActivity;
import com.aviary.android.feather.library.Constants;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

public class FeedFragment extends SherlockFragment {
	public static final String TAG = "FeedFragment";
	
	private static Context mContext;
	
	private static final String REQUEST_GET_FEED = "REQUEST_GET_FEED";
	private static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
	
	static final int ACTION_IMAGE_CAPTURE = 1;
	static final int ACTION_IMAGE_EDIT = 2;
	static final int ACTION_IMAGE_PICK = 3;
	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";
	
	// mCurrentPhotoPath stores path to current processed image (new captured photo or picked from gallery)
	private String mCurrentPhotoPath;
	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
	
	// Aviary API key
	protected static final String API_SECRET = "85b3512939a63070"; 
	
	ImageLoader loader = ImageLoader.getInstance();
	ApiFetcher<String> mApiThread;
	
	protected Odnoklassniki mOdnoklassniki;
	
	ListView mListView; 
	ArrayList<FeedItem> mFeedItems;
	FeedItemAdapter mAdapter = null;
	String mAnchor = null;
	boolean mHasMore = true;
	boolean mLoadingMore = false;
	boolean mUpdatingFeed = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.feed));
		mContext = getSherlockActivity();
		mOdnoklassniki = Odnoklassniki.getInstance(mContext);
		
		setupAlbumDir();
		initBackgroundLoader();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
        View v = inflater.inflate(R.layout.fragment_feed, container, false);
        
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
        	           getFeed();
        	       }
        	}
        };
        mListView.setOnScrollListener(listener);
        mListView.setOnItemClickListener(new OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            Intent intent = new Intent(getSherlockActivity(), ScaleImageViewActivity.class);
	            intent.putExtra(EXTRA_IMAGE_URL, mFeedItems.get(position).getLargePhotos().get(0));
	            startActivity(intent);
	        	//Toast.makeText(getSherlockActivity(), "Image clicked at position: " + position, Toast.LENGTH_LONG).show();
	        }
	    });
        
		mFeedItems = new ArrayList<FeedItem>();
		mAdapter = null;
        mListView.setAdapter(null);
        
        return v;
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getFeed();
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
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_refresh:
				//Toast.makeText(getSherlockActivity(), "Refresh clicked", Toast.LENGTH_SHORT).show();
				if (!mLoadingMore) {
					mAnchor = null;
					mHasMore = true;
					mUpdatingFeed = true;
					getFeed();
					getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
				}
				return true;
			case R.id.submenu_action_takePhoto:
				dispatchTakePictureIntent();
				//Toast.makeText(getSherlockActivity(), "Take photo clicked", Toast.LENGTH_SHORT).show();
				return true;
			case R.id.submenu_action_pickFromGallery:
				//Toast.makeText(getSherlockActivity(), "Pick image from gallery", Toast.LENGTH_SHORT).show();
				dispatchPickPictureFromGalleryIntent();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	void setupAlbumDir() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
	}
	
	/* ============================== INTENTS ============================== */
	
	private void dispatchPhotoEditorIntent() {
		if (mCurrentPhotoPath == null) {
			return;
		}
		Intent newIntent = new Intent(getSherlockActivity(), FeatherActivity.class);
		newIntent.setData(Uri.parse(mCurrentPhotoPath));
		newIntent.putExtra(Constants.EXTRA_IN_API_KEY_SECRET, API_SECRET);
		newIntent.putExtra(Constants.EXTRA_OUTPUT, Uri.parse(mCurrentPhotoPath));
		startActivityForResult(newIntent, ACTION_IMAGE_EDIT); 
	}
	
	private void dispatchTakePictureIntent() { 
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File f = null;
		try {
			f = setUpPhotoFile();
			mCurrentPhotoPath = f.getAbsolutePath();
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
		} catch (IOException e) {
			// TODO: string resources
			Toast.makeText(getSherlockActivity(), "Не удалось создать файл для снимка", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			f = null;
			mCurrentPhotoPath = null;
			return;
		}
		startActivityForResult(takePictureIntent, ACTION_IMAGE_CAPTURE);
	}
	
	private void dispatchPickPictureFromGalleryIntent() {
		Intent intent = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, ACTION_IMAGE_PICK);
		/*
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		*/
		//startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), ACTION_IMAGE_PICK);
	}
	
	/* ============================== CAMERA ============================== */

	/* Photo album for this application */
	private String getAlbumName() {
		return getString(R.string.album_name);
	}

	private File getAlbumDir() {
		File storageDir = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d(TAG, "failed to create directory");
						// TODO: string resources
						Toast.makeText(getSherlockActivity(), "SD карта не доступна", Toast.LENGTH_SHORT).show();
						return null;
					}
				}
			}
		} else {
			Toast.makeText(getSherlockActivity(), "SD карта не доступна", Toast.LENGTH_SHORT).show();
			Log.v(TAG, "External storage is not mounted READ/WRITE.");
		}
		
		return storageDir;
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		return imageF;
	}

	private File setUpPhotoFile() throws IOException {
		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();
		return f;
	}

	private void galleryAddPic(Uri contentUri) {
		Log.d(TAG, contentUri.getPath());
		Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		mediaScanIntent.setData(contentUri);
		getSherlockActivity().sendBroadcast(mediaScanIntent);
	}

	/* ============================== *** ============================== */
	
	private void initBackgroundLoader() {
		mApiThread = new ApiFetcher<String>(mContext, new Handler());
        mApiThread.setListener(new ApiFetcher.Listener<String>() {

			public void onRequestComplete(String token, String result) {
				Log.i(TAG, "Recieved response for request[" + token + "]: " + result);
				if (isVisible()) {
					if (token == REQUEST_GET_FEED) {
						parseFeed(result);
					}
				}
			}
        	
		});
        mApiThread.start();
        mApiThread.getLooper();
        Log.i(TAG, "Background thread started");
	}

	private void getFeed() {
		if (!mHasMore || mLoadingMore) {
			return;
		}
		mLoadingMore = true;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("patterns", "PHOTO");
		params.put("count", "100");
		params.put("fields", "feed.photo_refs,feed.message,feed.type,feed.date,photo.pic640x480,group_photo.pic640x480");
		if (mAnchor != null) {
			params.put("anchor", mAnchor);
		}
		ApiRequest request = new ApiRequest(ApiRequest.METHOD_GET_STREAM, params);
		mApiThread.queueRequest(REQUEST_GET_FEED, request);
	}

	private void parseFeed(String response) {
		if (response.equals("{}")) {
			mHasMore = false;
			return;
		}
		ArrayList<FeedItem> feedItems = new ArrayList<FeedItem>();
		
		JSONParser parser = new JSONParser();
		JSONObject json = null;
		try {
	        json = (JSONObject) parser.parse(response);
		} catch (org.json.simple.parser.ParseException e) {
        	Log.e(TAG, "JSON Parse error: " + e.getMessage());
        }
		try {
			mAnchor = (String) json.get("anchor");
		
	        // Entities elements
	        JSONObject entities = (JSONObject) json.get("entities");
	        JSONArray _photos = (JSONArray) entities.get("photos");
	        JSONArray _groupPhotos = (JSONArray) entities.get("group_photos");
	        
	        HashMap<String, String> photos = new HashMap<String, String>();
	        HashMap<String, String> groupPhotos = new HashMap<String, String>();
	        
	        if (_photos != null) {
		        for (int i = 0; i < _photos.size(); i++) {
		        	JSONObject cur = (JSONObject) _photos.get(i);
		        	String ref = (String) cur.get("ref");
		        	String pic = (String) cur.get("pic640x480");
		        	photos.put(ref, pic);
		        }
	        }
	        
	        if (_groupPhotos != null) {
		        for (int i = 0; i < _groupPhotos.size(); i++) {
		        	JSONObject cur = (JSONObject) _groupPhotos.get(i);
		        	String ref = (String) cur.get("ref");
		        	String pic = (String) cur.get("pic640x480");
		        	groupPhotos.put(ref, pic);
		        }
	        }
			
			
			// Feed elements
	        JSONArray feed = (JSONArray) json.get("feeds");
	        for (int i = 0; i < feed.size(); i++) {
	        	JSONObject feedEntry = (JSONObject) feed.get(i);
	        	String pattern = (String) feedEntry.get("pattern");
	        	if (!pattern.equals("PHOTO")) {
	        		continue;
	        	}
	        	JSONArray photoRefs = (JSONArray) feedEntry.get("photo_refs");
	        	String date = (String) feedEntry.get("date");
	        	String message = (String) feedEntry.get("message");
	        	
	        	if (date != null) {
	        		Log.i(TAG, date);
	        	}
	        	if (message != null) {
	        		Log.i(TAG, "" + message.length());
	        	}
	        	if (photoRefs != null) {
	        		Log.i(TAG, photoRefs.toString());
	        	}
	        	ArrayList<String> feedItemPhotos = new ArrayList<String>();
	        	
	        	if (photoRefs != null) {
	        		for (int j = 0; j < photoRefs.size(); j++) {
	        			String photoRef = (String) photoRefs.get(j);
	        			String photoUrl = null;
	        			if (photoRef.startsWith("group")) {
	        				photoUrl = groupPhotos.get(photoRef);
	        			} else {
	        				photoUrl = photos.get(photoRef);
	        			}
	        			feedItemPhotos.add(photoUrl);
	        		}
	        	}
	        	
	        	FeedItem item = new FeedItem(message, date, feedItemPhotos);
	        	feedItems.add(item);
	        	Log.i(TAG, "new feed item: " + item);
	        }
	        
		} catch (Exception e) {
			Log.e(TAG, "Parse failed, " + e.toString());
		}
		
		if (mUpdatingFeed) {
			mFeedItems.clear();
			mUpdatingFeed = false;
		}
        
		mFeedItems.addAll(feedItems);
        if (mAdapter == null) {
			mAdapter = new FeedItemAdapter(mFeedItems);
			mListView.setAdapter(mAdapter);
		} else {
			mAdapter.notifyDataSetChanged();
		}
		mLoadingMore = false;
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
		
	}
	
	private class FeedItemAdapter extends ArrayAdapter<FeedItem> {
    	
    	public FeedItemAdapter(ArrayList<FeedItem> items) {
    		super(getSherlockActivity(), 0, items);
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		if (convertView == null) {
    			convertView = getActivity().getLayoutInflater()
    					.inflate(R.layout.feed_item, parent, false);
    		}
    		
    		ImageView imageView = (ImageView) convertView.findViewById(R.id.feedItem_imageView);
    		TextView message = (TextView) convertView.findViewById(R.id.feedItem_textViewMessage);
    		TextView date = (TextView) convertView.findViewById(R.id.feedItem_textViewDate);
    		
    		FeedItem item = getItem(position);
    		
    		message.setText(item.getMessage());
    		date.setText(item.getDate());
    		
    		if (!item.getLargePhotos().isEmpty()) {
    			loader.displayImage(item.getLargePhotos().get(0), imageView);
    		} else {
    			Log.e(TAG, "No photos");
    		}
    		return convertView;
    	}
    }
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ACTION_IMAGE_CAPTURE:
			if (resultCode == SherlockActivity.RESULT_OK) {
				galleryAddPic(Uri.fromFile(new File(mCurrentPhotoPath)));
				
				// mCurrentPhotoPath contains path to photo
				dispatchPhotoEditorIntent();
			}
			break;
		case ACTION_IMAGE_PICK:
			if (requestCode == ACTION_IMAGE_PICK && resultCode == SherlockActivity.RESULT_OK && data != null) {
		         Uri selectedImage = data.getData();
		         String[] filePathColumn = { MediaStore.Images.Media.DATA };
		 
		         Cursor cursor = getSherlockActivity().getContentResolver().query(selectedImage,
		                 filePathColumn, null, null, null);
		         cursor.moveToFirst();
		 
		         int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		         String picturePath = cursor.getString(columnIndex);
		         cursor.close();
		         
		         mCurrentPhotoPath = picturePath;
		         dispatchPhotoEditorIntent();
		     }
			break;
		case ACTION_IMAGE_EDIT:
			if (resultCode == SherlockActivity.RESULT_OK) {
				Uri imageUri = data.getData();
                Bundle extra = data.getExtras();
                if (extra != null) {
                    // image has been changed by the user?
                    boolean changed = extra.getBoolean(Constants.EXTRA_OUT_BITMAP_CHANGED);
                    if (changed) {
                    	galleryAddPic(imageUri);
                    }
                    new UploadPhotoTask(getSherlockActivity()).execute(new Void[0]);
                }
			}
		default:
			break;
		} 
	}
	
	protected final class UploadPhotoTask extends AsyncTask<Void, Void, Boolean> {
		private final static String TAG = "UploadPhotoTask";
		
		private SherlockFragmentActivity activity;
		private ProgressDialog dialog;
		
		public UploadPhotoTask(SherlockFragmentActivity activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// prepare UI before execution
			
			dialog = new ProgressDialog(activity);
			dialog.setMessage(getString(R.string.photo_uploading));
			dialog.setCancelable(false);
			dialog.show();
			
			//setSupportProgressBarIndeterminateVisibility(true);
		}
		
		@Override
		protected Boolean doInBackground(Void... arg) {
			String uploadUrl = getUploadUrl();
			if (uploadUrl == null) {
				return false;
			}
			String commitData = uploadPhoto(uploadUrl);
			if (commitData == null) {
				return false;
			}
			String commitResponse = commitUpload(commitData);
			// TODO: better response handle 
			return commitResponse != null;
		}
		
		@Override 
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
			}
			// upload successful or not
			if (result) {
				Toast.makeText(getSherlockActivity(), 
						getString(R.string.photo_upload_successful), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getSherlockActivity(), 
						getString(R.string.photo_upload_failed), Toast.LENGTH_SHORT).show();
			}
			mCurrentPhotoPath = null;
			//setSupportProgressBarIndeterminateVisibility(true);
		}
		
		private String getUploadUrl() {
			String response;
			try {
				response = mOdnoklassniki.request("photosV2.getUploadUrl", null, "get");
			} catch (IOException e) {
				Log.e(TAG, "Failed to get UploadUrl");
				e.printStackTrace();
				return null;
			}
			Log.e(TAG, "photosV2.getUploadUrl response: " + response);
			JSONObject json;
			String uploadUrl = null;
			try {
				json = (JSONObject) new JSONParser().parse(response);
				uploadUrl = json.get("upload_url").toString();
			} catch (org.json.simple.parser.ParseException e) {
				Log.e(TAG, "JSON parse error: " + e.getMessage());
				e.printStackTrace();
			} 
			Log.d(TAG, "Upload url: " + uploadUrl);
			return uploadUrl;
		}
		
		private String uploadPhoto(String uploadUrl) {
			HttpClient httpclient = new DefaultHttpClient();
	        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

	        HttpPost httppost = new HttpPost(uploadUrl);
	        File file = new File(mCurrentPhotoPath);
	        FileBody fb = new FileBody(file);
	        
	        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
	        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
	        
	        builder.addPart("photo", fb);  
	        final HttpEntity entity = builder.build();
	        Log.d(TAG, "Content length: " + entity.getContentLength());
	        
	        httppost.setEntity(entity);
	        
	        Log.d(TAG, "Executing request: " + httppost.getRequestLine());
	        
	        HttpResponse response = null;
			try {
				response = httpclient.execute(httppost);
			} catch (ClientProtocolException e) {
				Log.e(TAG, "HTTP Post execuion error: " + e.getMessage());
				return null;
			} catch (IOException e) {
				Log.e(TAG, "HTTP Post execuion error: " + e.getMessage());
				return null;
			}
	        HttpEntity resEntity = response.getEntity();
	        Log.d(TAG, "" + response.getStatusLine());
	        
	        String responseString = null;
	        if (resEntity != null) {
	            try {
	            	responseString = EntityUtils.toString(resEntity);
					Log.d(TAG, responseString);
					
				} catch (ParseException e) {
					Log.e(TAG, "HTTP Entity parse error: " + e.getMessage());
					e.printStackTrace();
					return null;
				} catch (IOException e) {
					Log.e(TAG, "HTTP Entity IO error: " + e.getMessage());
					e.printStackTrace();
					return null;
				}
	        }
	        httpclient.getConnectionManager().shutdown();
	        
	        
	        JSONParser parser = new JSONParser();
	        JSONObject json = null;
	        try {
	            json = (JSONObject) parser.parse(responseString);
	        } catch (org.json.simple.parser.ParseException e) {
	        	Log.e(TAG, "JSON Parse error: " + e.getMessage());
	            e.printStackTrace();
	            return null;
	        }
	        JSONObject photos = (JSONObject) json.get("photos");
	        Set<?> x = photos.keySet();
	        Iterator<?> it = x.iterator();
	        JSONArray commit = new JSONArray();
	        while (it.hasNext()) {
	            String photoId = it.next().toString();
	            String token = ((JSONObject) photos.get(photoId)).get("token").toString();
	            JSONObject obj = new JSONObject();
	            obj.put("photo_id", photoId);
	            obj.put("token", token);
	            commit.add(obj);
	        }
	        
	        Log.d(TAG, commit.toString());
	        return commit.toString();
		}
	
		private String commitUpload(String commitData) {
			Map<String, String> photos = new TreeMap<String, String>();
			photos.put("photos", commitData);
			String response = null;
			try {
				response = mOdnoklassniki.request("photosV2.commit", photos, "get");
				Log.d(TAG, "photosV2.commit response: " + response);
			} catch (Exception e) {
				Log.e(TAG, "Failed to commit photos" + e.getMessage());
				e.printStackTrace();
			}
			return response;
		}
	}
}
