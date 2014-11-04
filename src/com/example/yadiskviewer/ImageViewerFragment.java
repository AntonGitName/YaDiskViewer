package com.example.yadiskviewer;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

public class ImageViewerFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ListItem>>{

	private static final String ITEM_TO_SHOW = "ITEM_TO_SHOW";

	private static String TAG = "ImageViewFragment";

	private ViewPager pager;
	private ImagePagerAdapter adapter;

	private MenuItem startSlideshowMenuItem;
	private MenuItem pauseSlideshowMenuItem;
	
	private Credentials credentials;
	private String currentDir;

	private ListItem itemToShow;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		registerForContextMenu(getView());

		Bundle args = getArguments();
		
		pager = (ViewPager) getActivity().findViewById(R.id.view_pager);

		credentials = args.getParcelable(DiskViewerFragment.CREDENTIALS_KEY);
		currentDir = args.getString(DiskViewerFragment.CURRENT_DIR_KEY);
		itemToShow = args.getParcelable(DiskViewerFragment.FIRST_TO_SHOW_KEY);
		
		ArrayList<ListItem> adapterData;
		if (savedInstanceState != null) {
			adapterData = savedInstanceState.getParcelableArrayList(DiskViewerFragment.IMAGES_LIST_KEY);
			itemToShow = savedInstanceState.getParcelable(ITEM_TO_SHOW);
		} else {
			adapterData = new ArrayList<ListItem>();
		}
		adapter = new ImagePagerAdapter(adapterData, credentials, this);
		pager.setAdapter(adapter);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putParcelableArrayList(DiskViewerFragment.IMAGES_LIST_KEY, (ArrayList<ListItem>) adapter.getData());
		outState.putParcelable(ITEM_TO_SHOW, itemToShow);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.view_action_bar, menu);
		
		startSlideshowMenuItem = menu.findItem(R.id.action_start);
		pauseSlideshowMenuItem = menu.findItem(R.id.action_pause);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		
		return inflater.inflate(R.layout.image_pager_fragment, container, false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			getFragmentManager().popBackStack();
			break;
		case R.id.view_current_folder_all:
			Log.d(TAG, "Change view mode to disk");
			getFragmentManager().popBackStack();
			break;
		case R.id.action_start:
			Log.d(TAG, "Start slideshow");
			startSlideshowMenuItem.setEnabled(false);
			pauseSlideshowMenuItem.setEnabled(true);
			break;
		case R.id.action_pause:
			Log.d(TAG, "Pause slideshow");
			startSlideshowMenuItem.setEnabled(true);
			pauseSlideshowMenuItem.setEnabled(false);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public Loader<List<ListItem>> onCreateLoader(int i, Bundle bundle) {
		return new ImageViewerLoader(getActivity(), credentials, currentDir);
	}
	
	@Override
	public void onLoaderReset(Loader<List<ListItem>> loader) {
		adapter.resetData();
	}

	@Override
	public void onLoadFinished(final Loader<List<ListItem>> loader, List<ListItem> data) {
		if (data.isEmpty()) {
			Exception ex = ((ImageViewerLoader) loader).getException();
			if (ex != null) {
				setEmptyText(((ImageViewerLoader) loader).getException().getMessage());
			} else {
				setDefaultEmptyText();
			}
		} else {			
			// not really sure that it works okay and that these lines are necessary...
			// This is done in order to save in bundle active image after screen rotation
			if (data.size() > adapter.getData().size()) {
				adapter.setData(data);
			}
			
			if (itemToShow != null) {
				int index = 0;
				List<ListItem> adapterData = adapter.getData();
				for (; index < adapterData.size(); ++index) {
					if (itemToShow.getEtag().equals(adapterData.get(index).getEtag()))
						break;
				}
				if (index != adapterData.size()) {
					pager.setCurrentItem(index);
					itemToShow = null;
				}
			}
		}
	}

	private void setEmptyText(String message) {
		adapter.setText(message);
	}

	private void setDefaultEmptyText() {
		setEmptyText(getString(R.string.no_image_files));
	}
}
