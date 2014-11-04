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

public class DiskViewerLoader extends AsyncTaskLoader<List<ListItem>> {

	private static Collator collator = Collator.getInstance();

	private static final int ITEMS_PER_REQUEST = 20;
	private static final String TAG = "DiskViewerLoader";
	static {
		collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
	}

	private Credentials 		m_credentials;
	private String 				m_dir;
	private Exception  			m_exception;
	
	private List<ListItem> 		m_fileItemList;
	private Handler 			m_handler;
	private boolean 			m_hasCancelled;

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

	public DiskViewerLoader(Context context, Credentials credentials, String dir) {
		super(context);
		m_handler = new Handler();
		this.m_credentials = credentials;
		this.m_dir = dir;
	}

	public Exception getException() {
		return m_exception;
	}

	@Override
	public List<ListItem> loadInBackground() {
		m_fileItemList = new ArrayList<ListItem>();
		m_hasCancelled = false;
		TransportClient client = null;
		try {
			client = TransportClient.getInstance(getContext(), m_credentials);
			client.getList(m_dir, ITEMS_PER_REQUEST, new ListParsingHandler() {

				// First item in PROPFIND is the current collection name
				boolean ignoreFirstItem = true;

				@Override
				public boolean handleItem(ListItem item) {
					if (ignoreFirstItem) {
						ignoreFirstItem = false;
						return false;
					} else {
						m_fileItemList.add(item);
						return true;
					}
				}

				@Override
				public boolean hasCancelled() {
					return m_hasCancelled;
				}

				@Override
				public void onPageFinished(int itemsOnPage) {
					ignoreFirstItem = true;
					m_handler.post(new Runnable() {
						@Override
						public void run() {
							Collections.sort(m_fileItemList, FILE_ITEM_COMPARATOR);
							deliverResult(new ArrayList<ListItem>(m_fileItemList));
						}
					});
				}
			});
			Collections.sort(m_fileItemList, FILE_ITEM_COMPARATOR);
			m_exception = null;
		} catch (CancelledPropfindException ex) {
			return m_fileItemList;
		} catch (WebdavException ex) {
			Log.d(TAG, "loadInBackground", ex);
			m_exception = ex;
		} catch (IOException ex) {
			Log.d(TAG, "loadInBackground", ex);
			m_exception = ex;
		} finally {
			TransportClient.shutdown(client);
		}
		return m_fileItemList;
	}

	@Override
	protected void onReset() {
		super.onReset();
		m_hasCancelled = true;
	}

	@Override
	protected void onStartLoading() {
		forceLoad();
	}
}
