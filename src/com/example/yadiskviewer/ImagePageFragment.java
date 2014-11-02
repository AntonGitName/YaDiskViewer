package com.example.yadiskviewer;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;
import com.yandex.disk.client.ProgressListener;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.WebdavException;

public class ImagePageFragment extends Fragment implements ProgressListener {
	
	private static final String TAG = "ImagePageFragment";
	
	private static final String BITMAP_KEY = "BITMAP_KEY";
	
	private Bitmap m_bitmap;
	private ProgressBar m_progressBar;
	private ImageView m_imageView;
	private LinearLayout layout;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		
		layout = new LinearLayout(inflater.getContext());
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		layout.setGravity(Gravity.CENTER);
		return layout;
	}
	 
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		m_imageView = new ImageView(getActivity());
		layout.addView(m_imageView);
		
		if (savedInstanceState == null) {
			Log.d(TAG, "Loading image in AsyncTask...");
			
			Bundle args = getArguments();
			final ListItem item = args.getParcelable(ImagePagerAdapter.LIST_ITEM_KEY);
			final Credentials credentials = args.getParcelable(DiskViewerFragment.CREDENTIALS);
			
			m_progressBar = new ProgressBar(getActivity());
			layout.addView(m_progressBar);
			
			WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point size = new Point();
			size.x = display.getWidth();
			size.y = display.getHeight();
			
			Log.d(TAG, "layout size: " + size.x + ", " + size.y);
			new DownloadTask(item, getActivity(), credentials, size.x, size.y).execute();			
		} else {
			Log.d(TAG, "Loading image from Bundle..."); 
			
			m_bitmap = savedInstanceState.getParcelable(BITMAP_KEY);
			m_imageView.setImageBitmap(m_bitmap);
			
			Log.d(TAG, "Loading finished (bundle)");
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putParcelable(BITMAP_KEY, m_bitmap);
	}

	private class DownloadTask extends AsyncTask<Void, Void, Bitmap> {

		public DownloadTask(ListItem item, Context context, Credentials credentials, int reqWidth, int reqHeght) {
			super();
			this.item = item;
			this.context = context;
			this.credentials = credentials;
			this.reqWidth = reqWidth;
			this.reqHeght = reqHeght;
		}

		private final ListItem item;
		private final Context context;
		private final Credentials credentials;
		private final int reqWidth;
		private final int reqHeght;
		
		@Override
		protected void onPostExecute(Bitmap result) {
			m_bitmap = result;
			m_imageView.setImageBitmap(m_bitmap);
			layout.removeView(m_progressBar);
			//m_progressBar.setVisibility(ProgressBar.INVISIBLE);
			Log.d(TAG, "Loading finished (AsyncTask): " + m_bitmap);
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			File fileToSave = new File(context.getFilesDir(), new File(item.getFullPath()).getName());
			TransportClient client = null;
			
			try {
				client = TransportClient.getInstance(context, credentials);
				client.downloadFile(item.getFullPath(), fileToSave, ImagePageFragment.this);
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
