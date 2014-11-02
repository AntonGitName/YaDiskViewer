package com.example.yadiskviewer;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

public class ImagePagerAdapter extends FragmentStatePagerAdapter {

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		// TODO Auto-generated method stub
		super.destroyItem(container, position, object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		// TODO Auto-generated method stub
		return super.instantiateItem(container, position);
	}

	private static final String TAG = "ImagePagerAdapter";
	
	public static final String LIST_ITEM_KEY = "LIST_ITEM_KEY";
	
	private final List<ListItem> list;
	
	private final Credentials credentials;
	
	public ImagePagerAdapter(FragmentManager fragmentManager, ArrayList<ListItem> list, Credentials credentials) {
		super(fragmentManager);

		this.list = list;
		this.credentials = credentials;
	}

	@Override
	public int getCount() {
		return list.size();
	}
	
	@Override
	public Fragment getItem(int arg0) {
		ListItem item = list.get(arg0);
		
		Log.d(TAG, "Fragment num: " + arg0);
		Log.d(TAG, "Image to show: " + item.getName());
		
		ImagePageFragment fragment = new ImagePageFragment();

		Bundle args = new Bundle();
		args.putParcelable(DiskViewerFragment.CREDENTIALS, credentials);
		args.putParcelable(LIST_ITEM_KEY, item);
		fragment.setArguments(args);
		
		return fragment;
	}

}
