package nyaschenko.oki.utils;

import java.util.ArrayList;

import android.content.Context;

public class PhotoKeeper {
	private static PhotoKeeper sPhotoKeeper;
	private Context mAppContext;
	
	private ArrayList<PhotoItem> mPhotos;
	
	private PhotoKeeper(Context context) {
		mAppContext = context;
		mPhotos = new ArrayList<PhotoItem>();
	}
	
	public static PhotoKeeper get(Context context) {
		if (sPhotoKeeper == null) {
			sPhotoKeeper = new PhotoKeeper(context.getApplicationContext());
		}
		return sPhotoKeeper;
	}
	
	public ArrayList<PhotoItem> getPhotos() {
		return mPhotos;
	}
	
	public void addPhoto(PhotoItem photo) {
		mPhotos.add(photo);
	}
}
