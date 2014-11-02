package com.example.yadiskviewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static TextView emptyListText;

	public static final String LIST_ITEM_KEY = "LIST_ITEM_KEY";
	private static final String TAG = "ImagePagerAdapter";

	private static final LayoutParams VIEW_LAYOUT_PARAMS = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT);

	private final Credentials credentials;

	private final Fragment fragment;

	// private static final LayoutParams TEXT_LAYOUT_PARAMS = new
	// LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
	// LayoutParams.WRAP_CONTENT);

	private final boolean isEmpty;

	private final List<ListItem> list;

	private final Map<Integer, View> pages = new HashMap<>();

	public ImagePagerAdapter(ArrayList<ListItem> list, Credentials credentials, Fragment fragment) {
		super();

		this.list = list;
		this.credentials = credentials;
		this.isEmpty = list.isEmpty();
		this.fragment = fragment;
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
		return isEmpty ? 1 : list.size();
	}

	@Override
	public Object instantiateItem(View collection, int position) {
		Log.d(TAG, "Instantiate item " + position);

		ViewPager pager = (ViewPager) collection;

		// nothing to show
		if (isEmpty) {
			// create view only once
			if (emptyListText == null) {
				emptyListText = new TextView(fragment.getActivity());
				emptyListText.setText(R.string.no_images_in_dir);
				pager.addView(emptyListText, 0);
			}
			return emptyListText;
		}

		// already created view
		if (pages.containsKey(position)) {
			View v = pages.get(position);
			pager.addView(v, 0);
			return v;
		}

		// we have to create layout for every page
		LinearLayout layout = new LinearLayout(fragment.getActivity());
		layout.setGravity(Gravity.CENTER);
		layout.setLayoutParams(VIEW_LAYOUT_PARAMS);

		ImageView imageView = new ImageView(fragment.getActivity());
		imageView.setContentDescription(list.get(position).getDisplayName());

		new DownloadImageTask(list.get(position), fragment.getActivity(), credentials, imageView, layout).execute();

		// save the page
		pages.put(position, layout);
		pager.addView(layout);

		return layout;

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
}
