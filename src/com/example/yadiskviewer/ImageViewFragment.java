package com.example.yadiskviewer;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

public class ImageViewFragment extends Fragment {

	private static String TAG = "ImageViewFragment";
	
	private ViewPager viewPager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		return inflater.inflate(R.layout.image_pager_fragment, container, false);
	}
	 
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		registerForContextMenu(getView());
		
		Log.d(TAG, "Creating adapter");
		
		Bundle args = getArguments();
		
		viewPager = (ViewPager) getActivity().findViewById(R.id.view_pager);
		
		ArrayList<ListItem> list = args.getParcelableArrayList(DiskViewerFragment.IMAGES_LIST_KEY);
		Credentials credentials = args.getParcelable(DiskViewerFragment.CREDENTIALS);
		viewPager.setAdapter(new ImagePagerAdapter(getFragmentManager(), list, credentials));
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.view_action_bar, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			getFragmentManager().popBackStack();
			break;
		case R.id.view_current_folder_all:
			Log.d(TAG, "Change view mode to disk");
			getFragmentManager().popBackStack();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
}
