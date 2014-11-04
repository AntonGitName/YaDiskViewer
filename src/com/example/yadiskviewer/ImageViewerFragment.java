package com.example.yadiskviewer;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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

public class ImageViewerFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ListItem>> {

	private static final String ITEM_TO_SHOW = "ITEM_TO_SHOW";
	private static final String SLIDESHOW_FLAG = "SLIDESHOW_FLAG";
	private static final String TAG = "ImageViewFragment";
	private static final int CHANGE_SLIDE_TIME = 2000;

	private ViewPager 				m_viewPager;
	private ImagePagerAdapter 		m_viewPagerAdapter;
	private MenuItem 				m_startSlideshowMenuItem;
	private MenuItem 				m_pauseSlideshowMenuItem;
	private Credentials 			m_credentials;
	private String 					m_currentDir;
	private ListItem 				m_itemToShow;
	private int 					m_currentPageNumber;
	private NextSlideTask 			m_nextSlideTask;
	private boolean 				m_slideshowFlag;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		registerForContextMenu(getView());

		Bundle args = getArguments();

		m_viewPager = (ViewPager) getActivity().findViewById(R.id.view_pager);

		m_viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int arg0) {
				m_currentPageNumber = arg0;
				m_viewPagerAdapter.cancelUnusedTasks(m_currentPageNumber);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

		m_credentials = args.getParcelable(DiskViewerFragment.CREDENTIALS_KEY);
		m_currentDir = args.getString(DiskViewerFragment.CURRENT_DIR_KEY);
		m_itemToShow = args.getParcelable(DiskViewerFragment.FIRST_TO_SHOW_KEY);

		if (m_itemToShow != null) {
			m_startSlideshowMenuItem.setEnabled(false);
		}

		ArrayList<ListItem> adapterData;
		if (savedInstanceState != null) {
			adapterData = savedInstanceState.getParcelableArrayList(DiskViewerFragment.IMAGES_LIST_KEY);
			m_itemToShow = savedInstanceState.getParcelable(ITEM_TO_SHOW);
			m_slideshowFlag = savedInstanceState.getBoolean(SLIDESHOW_FLAG);
		} else {
			adapterData = new ArrayList<ListItem>();
			m_currentPageNumber = 0;
			m_slideshowFlag = true;
		}
		m_viewPagerAdapter = new ImagePagerAdapter(adapterData, m_credentials, this);
		m_viewPager.setAdapter(m_viewPagerAdapter);

		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!m_slideshowFlag) {
			startSlideshow();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		m_nextSlideTask.cancel(true);
		
		outState.putParcelableArrayList(DiskViewerFragment.IMAGES_LIST_KEY,
				(ArrayList<ListItem>) m_viewPagerAdapter.getData());
		outState.putParcelable(ITEM_TO_SHOW, m_itemToShow);
		outState.putBoolean(SLIDESHOW_FLAG, m_startSlideshowMenuItem.isEnabled());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.view_action_bar, menu);

		m_startSlideshowMenuItem = menu.findItem(R.id.action_start);
		m_pauseSlideshowMenuItem = menu.findItem(R.id.action_pause);
		
		m_startSlideshowMenuItem.setEnabled(m_slideshowFlag);
		m_pauseSlideshowMenuItem.setEnabled(!m_slideshowFlag);
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
			m_startSlideshowMenuItem.setEnabled(false);
			m_pauseSlideshowMenuItem.setEnabled(true);
			startSlideshow();
			break;
		case R.id.action_pause:
			Log.d(TAG, "Pause slideshow");
			m_startSlideshowMenuItem.setEnabled(true);
			m_pauseSlideshowMenuItem.setEnabled(false);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public Loader<List<ListItem>> onCreateLoader(int i, Bundle bundle) {
		return new ImageViewerLoader(getActivity(), m_credentials, m_currentDir);
	}

	@Override
	public void onLoaderReset(Loader<List<ListItem>> loader) {
		m_viewPagerAdapter.resetData();
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
			// not really sure that it works okay and that these lines are
			// necessary...
			// This is done in order to save in bundle active image after screen
			// rotation
			if (data.size() > m_viewPagerAdapter.getData().size()) {
				m_viewPagerAdapter.setData(data, m_currentPageNumber);
			}

			if (m_itemToShow != null) {
				int index = 0;
				List<ListItem> adapterData = m_viewPagerAdapter.getData();
				for (; index < adapterData.size(); ++index) {
					if (m_itemToShow.getEtag().equals(adapterData.get(index).getEtag()))
						break;
				}
				if (index != adapterData.size()) {
					m_viewPager.setCurrentItem(index);
					m_currentPageNumber = index;
					// m_viewPagerAdapter.cancelUnusedTasks(m_currentPageNumber);
					m_startSlideshowMenuItem.setEnabled(true);
					m_itemToShow = null;
				}
			}
		}
	}

	private void setEmptyText(String message) {
		m_viewPagerAdapter.setText(message);
	}

	private void setDefaultEmptyText() {
		setEmptyText(getString(R.string.no_image_files));
	}

	private void startSlideshow() {
		new NextSlideTask().execute();
	}

	private final class NextSlideTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled() && !m_viewPagerAdapter.isPageReady(m_currentPageNumber))
				;
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// m_viewPagerAdapter.cancelUnusedTasks(m_currentPageNumber);
			if (!m_startSlideshowMenuItem.isEnabled()) {
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (m_viewPagerAdapter.getCount() == 0) {
							return;
						}
						m_currentPageNumber = (m_currentPageNumber + 1) % m_viewPagerAdapter.getCount();
						m_viewPager.setCurrentItem(m_currentPageNumber);
						m_nextSlideTask = new NextSlideTask();
						m_nextSlideTask.execute();
					}
				}, CHANGE_SLIDE_TIME);
			}
		}
	}
}
