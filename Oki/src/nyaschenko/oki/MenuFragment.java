package nyaschenko.oki;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public class MenuFragment extends SherlockListFragment {
	public static final String TAG = "MenuFragment";
	
	private Callbacks mCallbacks;
	
	public interface Callbacks {
		void onMenuItemSelected(int index);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ArrayList<MenuEntry> entries = new ArrayList<MenuEntry>();
		entries.add(new MenuEntry(getString(R.string.feed), R.drawable.ic_action_chat_white));
		entries.add(new MenuEntry(getString(R.string.my_photos), R.drawable.ic_action_camera_white));
		entries.add(new MenuEntry(getString(R.string.friends), R.drawable.ic_action_group_white));
		entries.add(new MenuEntry(getString(R.string.settings), R.drawable.ic_action_settings_white));
		
		MenuAdapter adapter = new MenuAdapter(entries);
		setListAdapter(adapter);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.menu_list, container, false);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		mCallbacks.onMenuItemSelected(position);
	}
	
	private class MenuAdapter extends ArrayAdapter<MenuEntry> {
		
		public MenuAdapter(ArrayList<MenuEntry> menu) {
			super(getSherlockActivity(), 0, menu);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getSherlockActivity().getLayoutInflater()
						.inflate(R.layout.menu_sliding_entry, null);
			}
			
			TextView text = (TextView) convertView.findViewById(R.id.menu_sliding_entry_text);
			ImageView image = (ImageView) convertView.findViewById(R.id.menu_sliding_entry_icon);
			
			MenuEntry entry = getItem(position);
			
			text.setText(entry.getEntry());
			image.setImageResource(entry.getIconResource());
			
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
	
	private class MenuEntry {
		
		private final String entry;
		private final int iconResource;
		
		MenuEntry(String entry, int iconResource) {
			this.entry = entry;
			this.iconResource = iconResource; 
		}
		
		public String getEntry() {
			return entry;
		}

		public int getIconResource() {
			return iconResource;
		}

	}

}

