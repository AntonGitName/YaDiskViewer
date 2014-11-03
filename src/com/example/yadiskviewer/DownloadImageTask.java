package com.example.yadiskviewer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;
import com.yandex.disk.client.ProgressListener;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.WebdavException;

public final class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> implements ProgressListener {

	private static final String TAG = "DownloadImageTask";

	private static final LayoutParams VIEW_LAYOUT_PARAMS = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT);

	private final Context context;
	private final Credentials credentials;

	private final ListItem item;
	private final LinearLayout layout;
	private final ImageView m_imageView;
	private ProgressBar m_progressBar;
	private final int reqHeght;
	private final int reqWidth;
	private static MessageDigest md;

	public DownloadImageTask(ListItem item, FragmentActivity activity, Credentials credentials, ImageView m_imageView,
			LinearLayout layout) {
		super();

		WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		this.item = item;
		this.context = activity;
		this.credentials = credentials;
		// TODO: solve it!
		this.reqWidth = display.getWidth(); // add what can I do with this?
		this.reqHeght = display.getHeight(); // except suppressing?..
		this.m_imageView = m_imageView;
		this.layout = layout;

		if (md == null) {
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				Log.d(TAG, "MD5 initialization", e);
				e.printStackTrace();
			}
		}
	}

	@Override
	protected Bitmap doInBackground(Void... params) {

		File fileToSave = new File(context.getFilesDir(), new File(item.getFullPath()).getName());

		if (!fileToSave.exists() || !checkFileConsistency(fileToSave, item)) {

			Log.d(TAG, "Image has not been found (or wrong md5): downloading");

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
			Log.d(TAG, "Image has been downloaded from Ya.Disk");
		} else {
			Log.d(TAG, "Image has been found.");
		}

		return loadBitmap(fileToSave.getAbsolutePath(), reqWidth, reqHeght);
	}

	@Override
	public boolean hasCancelled() {
		return false;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		m_imageView.setImageBitmap(result);
		layout.removeView(m_progressBar);
		layout.addView(m_imageView, 0, VIEW_LAYOUT_PARAMS);
		Log.d(TAG, "Image has been loaded from file");
	}

	@Override
	protected void onPreExecute() {
		m_progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
		layout.addView(m_progressBar, VIEW_LAYOUT_PARAMS);
		// TextView textView = new TextView(context, null);
		// textView.setText(item.getDisplayName());
		// layout.addView(textView, TEXT_LAYOUT_PARAMS);
	}

	@Override
	public void updateProgress(long loaded, long total) {
		m_progressBar.setMax((int) total);
		m_progressBar.setProgress((int) loaded);
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

	private static Bitmap loadBitmap(String filename, int reqWidth, int reqHeight) {

		long startTime = System.currentTimeMillis();
		
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap intermediateBitmap = BitmapFactory.decodeFile(filename, options);
		
		final float sizeFactor = (float) intermediateBitmap.getWidth() / (float) intermediateBitmap.getHeight();
		
		Bitmap result = Bitmap.createScaledBitmap(intermediateBitmap, (int) Math.min(reqWidth, reqHeight * sizeFactor),
				(int) Math.min(reqHeight, reqWidth / sizeFactor), false);
		
		Log.d(TAG, "Loading bitmap time: " + (System.currentTimeMillis() - startTime));
		
		return result;
	}

	private static boolean checkFileConsistency(File file, ListItem item) {
		
		long startTime = System.currentTimeMillis();
		
		String originalMD5 = item.getEtag();
		InputStream is = null;
		
		try {
			is = new BufferedInputStream(new FileInputStream(file));
			DigestInputStream dis = new DigestInputStream(is, md);
			byte[] bytes = new byte[4096];
			while (dis.read(bytes) != -1)
				;
			dis.close();
		} catch (IOException e) {
			Log.d(TAG, "Checking md5 error", e);
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				Log.d(TAG, "Checking md5 error", e);
			}
		}
		boolean result = originalMD5.equals(md5ToHex(md.digest()));
		
		Log.d(TAG, "Checking sums, time: " + (System.currentTimeMillis() - startTime));
		
		return result;
	}

	private static String md5ToHex(byte[] md5) {
		StringBuffer hexString = new StringBuffer();
		String hex;
		for (int i = 0; i < md5.length; i++) {
			hex = Integer.toHexString(0xFF & md5[i]);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
