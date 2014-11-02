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
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
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
		if (isEmpty) {
			if (emptyListText == null) {
				emptyListText = new TextView(fragment.getActivity());
				emptyListText.setText(R.string.no_images_in_dir);
				pager.addView(emptyListText, 0);
			}
			return emptyListText;
		}
		if (pages.containsKey(position)) {
			View v = pages.get(position);
			pager.addView(v, 0);
			return v;
		}

		ProgressBar progressBar = new ProgressBar(fragment.getActivity(), null, android.R.attr.progressBarStyleHorizontal);
		progressBar.setVisibility(ProgressBar.VISIBLE);
		pager.addView(progressBar);

		WindowManager wm = (WindowManager) fragment.getActivity().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		size.x = display.getWidth();
		size.y = display.getHeight();

		ImageView imageView = new ImageView(fragment.getActivity());
		Log.d(TAG, "layout size: " + size.x + ", " + size.y);

		new DownloadTask(list.get(position), fragment.getActivity(), credentials, size, pager, imageView, progressBar)
				.execute();

		pages.put(position, imageView);

		return imageView;

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

	private class DownloadTask extends AsyncTask<Void, Void, Bitmap> implements ProgressListener {

		public DownloadTask(ListItem item, Context context, Credentials credentials, Point size, ViewPager pager,
				ImageView m_imageView, ProgressBar m_progressBar) {
			super();
			this.item = item;
			this.context = context;
			this.credentials = credentials;
			this.pager = pager;
			this.reqWidth = size.x;
			this.reqHeght = size.y;
			this.m_imageView = m_imageView;
			this.m_progressBar = m_progressBar;
		}

		private final ListItem item;
		private final Context context;
		private final Credentials credentials;
		private final ViewPager pager;
		private final int reqWidth;
		private final int reqHeght;
		private final ImageView m_imageView;
		private ProgressBar m_progressBar;

		@Override
		protected void onPostExecute(Bitmap result) {
			m_imageView.setImageBitmap(result);
			pager.removeView(m_progressBar);
			pager.addView(m_imageView);
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
