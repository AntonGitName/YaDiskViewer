package com.example.yadiskviewer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;
import com.yandex.disk.client.ProgressListener;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.WebdavException;

public class ImagePagerAdapter extends PagerAdapter {

	private static final String TAG = "ImagePagerAdapter";

	public static final String LIST_ITEM_KEY = "LIST_ITEM_KEY";

	private final List<ListItem> list;
	private final Map<Integer, View> pages = new HashMap<>();

	private final Credentials credentials;
	private static TextView emptyListText;

	private final boolean isEmpty;

	private final Fragment fragment;
	
	private static final LayoutParams LAYOUT_PARAMS = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

	public ImagePagerAdapter(ArrayList<ListItem> list, Credentials credentials, Fragment fragment) {
		super();

		this.list = list;
		this.credentials = credentials;
		this.isEmpty = list.isEmpty();
		this.fragment = fragment;
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
		layout.setLayoutParams(LAYOUT_PARAMS);
		
		ImageView imageView = new ImageView(fragment.getActivity());
		new DownloadTask(list.get(position), fragment.getActivity(), credentials, imageView, layout).execute();
		
		// save the page
		pages.put(position, layout);
		pager.addView(layout);
		
		return layout;

	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view.equals(object);
	}

	@Override
	public void finishUpdate(View arg0) {
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

	private static class DownloadTask extends AsyncTask<Void, Void, Bitmap> implements ProgressListener {

		@Override
		protected void onPreExecute() {
			m_progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
			layout.addView(m_progressBar, LAYOUT_PARAMS);
		}

		public DownloadTask(ListItem item, FragmentActivity activity, Credentials credentials, ImageView m_imageView, LinearLayout layout) {
			super();

			WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();

			this.item = item;
			this.context = activity;
			this.credentials = credentials;
			this.reqWidth = display.getWidth();
			this.reqHeght = display.getHeight();
			this.m_imageView = m_imageView;
			this.layout = layout;
		}

		private final ListItem item;
		private final Context context;
		private final Credentials credentials;
		private final int reqWidth;
		private final int reqHeght;
		private final ImageView m_imageView;
		private final LinearLayout layout;
		private ProgressBar m_progressBar;

		@Override
		protected void onPostExecute(Bitmap result) {
			m_imageView.setImageBitmap(result);
			layout.removeView(m_progressBar);
			layout.addView(m_imageView, LAYOUT_PARAMS);
			Log.d(TAG, "Loading finished (AsyncTask)");
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			Log.d(TAG, "Starting load image");
			File fileToSave = new File(context.getFilesDir(), new File(item.getFullPath()).getName());
			TransportClient client = null;

			try {
				client = TransportClient.getInstance(context, credentials);
				client.downloadFile(item.getFullPath(), fileToSave, this);
			} catch (IOException ex) {
				Log.d(TAG, "loadFile", ex);
			} catch (WebdavException ex) {
				Log.d(TAG, "loadFile", ex);
			} finally {
				if (client != null) {
					client.shutdown();
				}
			}
			return loadBitmap(fileToSave.getAbsolutePath(), reqWidth, reqHeght);
		}

		@Override
		public void updateProgress(long loaded, long total) {
			m_progressBar.setMax((int) total);
			m_progressBar.setProgress((int) loaded);
		}

		@Override
		public boolean hasCancelled() {
			return false;
		}
	}

	private static Bitmap loadBitmap(String filename, int reqWidth, int reqHeight) {
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(filename, options);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2
			// and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}
}
