package nyaschenko.oki;

import ru.ok.android.sdk.Odnoklassniki;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.ImageLoader;

public abstract class ThreadFragment extends SherlockFragment {
	protected ImageLoader pLoader;
	protected Odnoklassniki pOdnoklassniki;
	protected ApiFetcher<String> pBackgroundThread;
	protected Context pContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		pContext = getSherlockActivity();
		pLoader = ImageLoader.getInstance();
		pOdnoklassniki = Odnoklassniki.getInstance(getSherlockActivity());
		pBackgroundThread = new ApiFetcher<String>(getSherlockActivity(), new Handler());
        pBackgroundThread.start();
        pBackgroundThread.getLooper();
	}
	
	@Override
	public void onDestroy() {
		pBackgroundThread.quit();
		pContext = null;
		super.onDestroy();
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		pBackgroundThread.clearQueue();
	}

}

