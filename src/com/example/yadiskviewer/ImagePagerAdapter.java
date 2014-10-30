package com.example.yadiskviewer;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

public class ImagePagerAdapter extends FragmentPagerAdapter {

	private final List<Fragment> pages;

	public ImagePagerAdapter(FragmentManager fm, List<Fragment> pages) {
		super(fm);

		this.pages = pages;
	}	

	@Override
	public int getCount() {
		return pages.size();
	}
	
	@Override
	public Fragment getItem(int arg0) {
		return this.pages.get(arg0);
	}

}
