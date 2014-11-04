package com.example.yadiskviewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

public class ImagePagerAdapter extends PagerAdapter {

	public static final String LIST_ITEM_KEY = "LIST_ITEM_KEY";
	private static final String TAG = "ImagePagerAdapter";

	private static final LayoutParams VIEW_LAYOUT_PARAMS = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT);

	private final Credentials 					m_credentials;
	private final Fragment 						m_viewerFragment;
	private final List<DownloadImageTask> 		m_tasks;

	private TextView 							m_emptyListText;
	private boolean 							m_onlyText;
	private String 								m_text;
	private List<ListItem> 						m_data;

	public ImagePagerAdapter(List<ListItem> data, Credentials credentials, Fragment fragment) {
		super();

		this.m_data = data;
		this.m_credentials = credentials;
		this.m_viewerFragment = fragment;
		this.m_onlyText = false;
		this.m_tasks = Collections.synchronizedList(new ArrayList<DownloadImageTask>());
	}

	public ImagePagerAdapter(String text, Credentials credentials, Fragment fragment) {
		super();

		this.m_text = text;
		this.m_credentials = credentials;
		this.m_viewerFragment = fragment;
		this.m_onlyText = true;
		this.m_tasks = Collections.synchronizedList(new ArrayList<DownloadImageTask>());
	}

	private void cancelTasks(int exception) {
		synchronized (m_tasks) {
			updateTasks();
			Iterator<DownloadImageTask> it = m_tasks.iterator();
			DownloadImageTask task;
			while (it.hasNext()) {
				task = it.next();
				if (task.getPageNumber() != exception) {
					task.cancel(true);
					it.remove();
				}
			}
		}
	}

	private void cancelTasks() {
		cancelTasks(-1);
	}

	private void updateTasks() {
		synchronized (m_tasks) {
			Iterator<DownloadImageTask> it = m_tasks.iterator();
			DownloadImageTask task;
			while (it.hasNext()) {
				task = it.next();
				if (task.getStatus() == AsyncTask.Status.FINISHED) {
					it.remove();
				}
			}
		}
	}

	public void cancelUnusedTasks(int currenrPage) {
		synchronized (m_tasks) {
			updateTasks();
			Iterator<DownloadImageTask> it = m_tasks.iterator();
			DownloadImageTask task;
			while (it.hasNext()) {
				task = it.next();
				if (Math.abs(task.getPageNumber() - currenrPage) > 1) {
					task.cancel(true);
					it.remove();
				}
			}
		}
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public void finishUpdate(View arg0) {
	}

	@Override
	public int getCount() {
		return m_onlyText ? 1 : m_data.size();
	}

	@Override
	public Object instantiateItem(View collection, int position) {
		Log.d(TAG, "Instantiate item " + position + " of " + m_data.size());

		ViewPager pager = (ViewPager) collection;

		// nothing to show
		if (m_onlyText) {
			// create view only once
			if (m_emptyListText == null) {
				m_emptyListText = new TextView(m_viewerFragment.getActivity());
				m_emptyListText.setText(m_text);
				pager.addView(m_emptyListText);
			}
			return m_emptyListText;
		}

		// we have to create layout for every page
		LinearLayout layout = new LinearLayout(m_viewerFragment.getActivity());
		layout.setGravity(Gravity.CENTER);
		layout.setLayoutParams(VIEW_LAYOUT_PARAMS);

		ImageView imageView = new ImageView(m_viewerFragment.getActivity());
		imageView.setContentDescription(m_data.get(position).getDisplayName());

		DownloadImageTask task = new DownloadImageTask(m_data.get(position), m_viewerFragment.getActivity(),
				m_credentials, imageView, layout, position);
		m_tasks.add(task);
		task.execute();

		pager.addView(layout);

		return layout;
	}

	public List<DownloadImageTask> getTasks() {
		return m_tasks;
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view.equals(object);
	}

	@Override
	public void restoreState(Parcelable arg0, ClassLoader arg1) {
	}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void startUpdate(View arg0) {
	}

	public void setData(List<ListItem> data, int currentPage) {
		cancelUnusedTasks(currentPage);
		this.m_data = data;
		notifyDataSetChanged();
	}

	public List<ListItem> getData() {
		return m_data;
	}

	public void resetData() {
		cancelTasks();
		m_data.clear();
		m_onlyText = true;
		notifyDataSetChanged();
	}

	public void setText(String message) {
		m_text = message;
		resetData();
	}
	
	/*
	 * Run only for pages that are known to be instatiated
	 */
	public boolean isPageReady(int page) {
		synchronized (m_tasks) {
			updateTasks();
			for (DownloadImageTask task : m_tasks) {
				if (task.getPageNumber() == page && task.getStatus() != AsyncTask.Status.FINISHED) {
					return false;
				}
			}
			return true;
		}
	}
}
