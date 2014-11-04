package com.example.yadiskviewer;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

public class DiskViewerFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<ListItem>> {

	public static final String CREDENTIALS_KEY = "yadiskviewer.credentials";
	public static final String IMAGES_LIST_KEY = "yadiskviewer.images.list";
	public static final String FIRST_TO_SHOW_KEY = "yadiskviewer.first.to.show";
	public static final String CURRENT_DIR_KEY = "yadiskviewer.current.dir";

	private static final String ROOT = "/";

	private static final String TAG = "DiskViewerFragment";

	private DiskViewerAdapter adapter;
	private Credentials credentials;
	private String currentDir;
	private MenuItem changeModeMenuItem;

	public static class DiskViewerAdapter extends ArrayAdapter<ListItem> {
		private final LayoutInflater inflater;

		public DiskViewerAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_2);
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;

			if (convertView == null) {
				view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
			} else {
				view = convertView;
			}

			ListItem item = getItem(position);
			((TextView) view.findViewById(android.R.id.text1)).setText(item.getDisplayName());
			((TextView) view.findViewById(android.R.id.text2)).setText(item.isCollection() ? "" : ""
					+ item.getContentLength());

			return view;
		}

		public void setData(List<ListItem> data) {
			clear();
			if (data != null) {
				addAll(data);
			}
		}
	}

	protected void changeDir(String dir) {
		Bundle args = new Bundle();
		args.putString(CURRENT_DIR_KEY, dir);

		DiskViewerFragment fragment = new DiskViewerFragment();
		fragment.setArguments(args);

		getFragmentManager().beginTransaction().replace(R.id.container, fragment, MainActivity.DISK_FRAGMENT_TAG)
				.addToBackStack(null).commit();
	}

	private void changeViewMode(ListItem itemToShow) {
		Log.d(TAG, "Change view mode to images");
		Bundle args = new Bundle();
		args.putParcelable(CREDENTIALS_KEY, credentials);
		args.putParcelable(FIRST_TO_SHOW_KEY, itemToShow);
		args.putString(CURRENT_DIR_KEY, currentDir);

		ImageViewerFragment fragment = new ImageViewerFragment();
		fragment.setArguments(args);

		getFragmentManager().beginTransaction().replace(R.id.container, fragment, MainActivity.IMAGE_FRAGMENT_TAG)
				.addToBackStack(null).commit();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setDefaultEmptyText();

		setHasOptionsMenu(true);

		registerForContextMenu(getListView());

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String username = preferences.getString(MainActivity.USERNAME, null);
		String token = preferences.getString(MainActivity.TOKEN, null);

		credentials = new Credentials(username, token);

		Bundle args = getArguments();
		if (args != null) {
			currentDir = args.getString(CURRENT_DIR_KEY);
		}
		if (currentDir == null) {
			currentDir = ROOT;
		}
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(!ROOT.equals(currentDir));

		adapter = new DiskViewerAdapter(getActivity());
		setListAdapter(adapter);
		setListShown(false);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<List<ListItem>> onCreateLoader(int i, Bundle bundle) {
		return new DiskViewerLoader(getActivity(), credentials, currentDir);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.disk_action_bar, menu);

		changeModeMenuItem = menu.findItem(R.id.view_current_folder_images);
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		ListItem item = (ListItem) getListAdapter().getItem(position);
		Log.d(TAG, "onListItemClick(): " + item);
		if (item.isCollection()) {
			changeDir(item.getFullPath());
		} else {
			if (item.getMediaType().equals(ImageViewerLoader.IMAGE_TYPE)) {
				changeViewMode(item);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<List<ListItem>> loader) {
		adapter.setData(null);
	}

	@Override
	public void onLoadFinished(final Loader<List<ListItem>> loader, List<ListItem> data) {
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
		if (data.isEmpty()) {
			Exception ex = ((DiskViewerLoader) loader).getException();
			if (ex != null) {
				setEmptyText(((DiskViewerLoader) loader).getException().getMessage());
			} else {
				setDefaultEmptyText();
			}
		} else {
			adapter.setData(data);
			changeModeMenuItem.setEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			getFragmentManager().popBackStack();
			break;
		case R.id.view_current_folder_images:
			changeViewMode(null);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void restartLoader() {
		getLoaderManager().restartLoader(0, null, this);
	}

	private void setDefaultEmptyText() {
		setEmptyText(getString(R.string.no_files));
	}
}
