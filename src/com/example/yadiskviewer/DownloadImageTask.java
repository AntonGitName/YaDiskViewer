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
	private static final int BYTE_BUFFER_SIZE = 4096;

	private final Context 			m_context;
	private final Credentials 		m_credentials;
	private final ListItem 			m_itemToDownload;
	private final LinearLayout 		m_pageLayout;
	private final ImageView 		m_pageImageView;
	private final int 				m_screenHeght;
	private final int 				m_screenWidth;
	private final int 				m_pageNumber;

	private ProgressBar 			m_pageProgressBar;

	private static MessageDigest m_messageDigest;

	@SuppressWarnings("deprecation")
	public DownloadImageTask(ListItem item, FragmentActivity activity, Credentials credentials, ImageView m_imageView,
			LinearLayout layout, int page_number) {
		super();

		WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		this.m_itemToDownload = item;
		this.m_context = activity;
		this.m_credentials = credentials;
		// TODO: remove damn warnings!
		this.m_screenWidth = display.getWidth(); // add what can I do with this?
		this.m_screenHeght = display.getHeight(); // except suppressing?..
		this.m_pageImageView = m_imageView;
		this.m_pageLayout = layout;
		this.m_pageNumber = page_number;

		if (m_messageDigest == null) {
			try {
				m_messageDigest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				Log.d(TAG, "MD5 initialization", e);
				e.printStackTrace();
			}
		}
	}

	public int getPageNumber() {
		return m_pageNumber;
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		Log.d(TAG, String.format("Cancled loading an image (file: %s, page: %d)", m_itemToDownload.getDisplayName(),
				m_pageNumber));
	}

	@Override
	protected Bitmap doInBackground(Void... params) {

		File fileToSave = new File(m_context.getFilesDir(), new File(m_itemToDownload.getFullPath()).getName());

		if (!fileToSave.exists() || !checkFileConsistency(fileToSave, m_itemToDownload)) {

			Log.d(TAG, String.format("Image has not been found (or wrong md5): downloading (file:%s, page: %d)",
					m_itemToDownload.getDisplayName(), m_pageNumber));

			TransportClient client = null;

			try {
				client = TransportClient.getInstance(m_context, m_credentials);
				client.downloadFile(m_itemToDownload.getFullPath(), fileToSave, this);
				Log.d(TAG,
						String.format("Image has been downloaded (file: %s, page: %d)",
								m_itemToDownload.getDisplayName(), m_pageNumber));
			} catch (IOException ex) {
				Log.d(TAG, "loadFile", ex);
			} catch (WebdavException ex) {
				Log.d(TAG, "loadFile", ex);
			} finally {
				if (client != null) {
					client.shutdown();
				}
			}
		} else {
			Log.d(TAG, String.format("Image has been found (file: %s, page: %d)", m_itemToDownload.getDisplayName(),
					m_pageNumber));
		}

		return loadBitmap(fileToSave.getAbsolutePath(), m_screenWidth, m_screenHeght);
	}

	@Override
	public boolean hasCancelled() {
		return isCancelled();
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		m_pageImageView.setImageBitmap(result);
		m_pageLayout.removeView(m_pageProgressBar);
		m_pageLayout.addView(m_pageImageView, 0, VIEW_LAYOUT_PARAMS);
		Log.d(TAG,
				String.format("Image loaded without being canceled (file: %s, page: %d)",
						m_itemToDownload.getDisplayName(), m_pageNumber));
	}

	@Override
	protected void onPreExecute() {
		m_pageProgressBar = new ProgressBar(m_context, null, android.R.attr.progressBarStyleHorizontal);
		m_pageLayout.addView(m_pageProgressBar, VIEW_LAYOUT_PARAMS);
		// TextView textView = new TextView(context, null);
		// textView.setText(item.getDisplayName());
		// layout.addView(textView, TEXT_LAYOUT_PARAMS);
	}

	@Override
	public void updateProgress(long loaded, long total) {
		m_pageProgressBar.setMax((int) total);
		m_pageProgressBar.setProgress((int) loaded);
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

		// final float sizeFactor = (float) intermediateBitmap.getWidth() /
		// (float) intermediateBitmap.getHeight();

		// Bitmap result = Bitmap.createScaledBitmap(intermediateBitmap, (int)
		// Math.min(reqWidth, reqHeight * sizeFactor),
		// (int) Math.min(reqHeight, reqWidth / sizeFactor), false);

		Log.d(TAG, "Loading bitmap time: " + (System.currentTimeMillis() - startTime));

		// return result;
		return intermediateBitmap;
	}

	private static boolean checkFileConsistency(File file, ListItem item) {

		long startTime = System.currentTimeMillis();

		String originalMD5 = item.getEtag();
		InputStream is = null;

		try {
			is = new BufferedInputStream(new FileInputStream(file));
			DigestInputStream dis = new DigestInputStream(is, m_messageDigest);
			byte[] bytes = new byte[BYTE_BUFFER_SIZE];
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
		boolean result = originalMD5.equals(md5ToHex(m_messageDigest.digest()));

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
