package nyaschenko.oki;

import ru.ok.android.sdk.Odnoklassniki;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.nostra13.universalimageloader.core.ImageLoader;

public abstract class ThreadListFragment extends SherlockListFragment {
	
	protected ImageLoader pLoader;
	protected Odnoklassniki pOdnoklassniki;
	protected ApiFetcher<String> pBackgroundThread;
	protected Context pContext;
	protected BaseAdapter pAdapter;
	protected View pFooter;
	protected ProgressBar pFooterProgressBar;
	protected TextView pFooterTextView;
	
	public abstract BaseAdapter getAdapter();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		pLoader = ImageLoader.getInstance();
		pOdnoklassniki = Odnoklassniki.getInstance(getSherlockActivity());
		pBackgroundThread = new ApiFetcher<String>(getSherlockActivity(), new Handler());
        pBackgroundThread.start();
        pBackgroundThread.getLooper();
        pFooter = LayoutInflater.from(getSherlockActivity()).inflate(R.layout.footer_progress, null);
        pFooterProgressBar = (ProgressBar) pFooter.findViewById(R.id.progressBar);
        pFooterTextView = (TextView) pFooter.findViewById(R.id.textView);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().addFooterView(pFooter);
		getListView().setDividerHeight(getResources().getDimensionPixelOffset(R.dimen.listview_divider_height_zero));
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
	
	protected void setupAdapter() {
		if (pAdapter == null) {
			pAdapter = getAdapter();
			setListAdapter(pAdapter);
		}
	}
	
	protected void hideFooter() {
		pFooterProgressBar.setVisibility(View.INVISIBLE);
		pFooterTextView.setVisibility(View.INVISIBLE);
	}
	
	protected void showFooter() {
		pFooterProgressBar.setVisibility(View.VISIBLE);
		pFooterTextView.setVisibility(View.VISIBLE);
	}

}
