package com.example.yadiskviewer;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;
import com.yandex.disk.client.ListParsingHandler;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.CancelledPropfindException;
import com.yandex.disk.client.exceptions.WebdavException;

public class ImageViewerLoader extends AsyncTaskLoader<List<ListItem>> {

	private static Collator collator = Collator.getInstance();

	private static final int ITEMS_PER_REQUEST = 20;

	public static final String IMAGE_TYPE = "image";
	private static String TAG = "ImageViewerLoader";
	static {
		collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
	}

	private Credentials credentials;
	private String dir;
	private Exception exception;
	
	private List<ListItem> fileItemList;
	private Handler handler;
	private boolean hasCancelled;

	private final Comparator<ListItem> FILE_ITEM_COMPARATOR = new Comparator<ListItem>() {
		@Override
		public int compare(ListItem f1, ListItem f2) {
			if (f1.isCollection() && !f2.isCollection()) {
				return -1;
			} else if (f2.isCollection() && !f1.isCollection()) {
				return 1;
			} else {
				return collator.compare(f1.getDisplayName(), f2.getDisplayName());
			}
		}
	};

	public ImageViewerLoader(Context context, Credentials credentials, String dir) {
		super(context);
		handler = new Handler();
		this.credentials = credentials;
		this.dir = dir;
	}

	public Exception getException() {
		return exception;
	}

	@Override
	public List<ListItem> loadInBackground() {
		fileItemList = new ArrayList<ListItem>();
		hasCancelled = false;
		TransportClient client = null;
		try {
			client = TransportClient.getInstance(getContext(), credentials);
			client.getList(dir, ITEMS_PER_REQUEST, new ListParsingHandler() {

				// First item in PROPFIND is the current collection name
				boolean ignoreFirstItem = true;

				@Override
				public boolean handleItem(ListItem item) {
					if (ignoreFirstItem) {
						ignoreFirstItem = false;
						return false;
					} else {
						if (!item.isCollection() && item.getMediaType().equals(IMAGE_TYPE)) {
							fileItemList.add(item);
						}
						return true;
					}
				}

				@Override
				public boolean hasCancelled() {
					return hasCancelled;
				}

				@Override
				public void onPageFinished(int itemsOnPage) {
					ignoreFirstItem = true;
					handler.post(new Runnable() {
						@Override
						public void run() {
							Collections.sort(fileItemList, FILE_ITEM_COMPARATOR);
							deliverResult(new ArrayList<ListItem>(fileItemList));
						}
					});
				}
			});
			Collections.sort(fileItemList, FILE_ITEM_COMPARATOR);
			exception = null;
		} catch (CancelledPropfindException ex) {
			return fileItemList;
		} catch (WebdavException ex) {
			Log.d(TAG, "loadInBackground", ex);
			exception = ex;
		} catch (IOException ex) {
			Log.d(TAG, "loadInBackground", ex);
			exception = ex;
		} finally {
			TransportClient.shutdown(client);
		}
		return fileItemList;
	}

	@Override
	protected void onReset() {
		super.onReset();
		hasCancelled = true;
	}

	@Override
	protected void onStartLoading() {
		forceLoad();
	}
	
}
