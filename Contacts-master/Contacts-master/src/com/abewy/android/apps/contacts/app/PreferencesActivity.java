package com.abewy.android.apps.contacts.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.abewy.android.apps.contacts.R;
import com.abewy.android.apps.contacts.core.CorePrefs;
import com.jfeinstein.jazzyviewpager.JazzyViewPager.TransitionEffect;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private static final String			ABOUT_KEY				= "contacts_about";
	private static final String			CHANGELOG_KEY			= "contacts_changelog";
	
	private boolean mSortByLastName;
	private boolean mRoundedAvatars;
	private boolean mListAnimation;
	private TransitionEffect mTransitionEffect;
	private int mPeopleViewType;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.preferences);
		
		mSortByLastName = CorePrefs.isSortingByLastName();
		mRoundedAvatars = CorePrefs.isRoundedPictures();
		mListAnimation = CorePrefs.isAnimatingListGridItems();
		mTransitionEffect = CorePrefs.getViewPagerEffect();
		mPeopleViewType=CorePrefs.getPeopleViewType();
		 
	 

		 
	}
	

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		boolean changed = mSortByLastName != CorePrefs.isSortingByLastName()
							|| mRoundedAvatars != CorePrefs.isRoundedPictures()
							|| mListAnimation != CorePrefs.isAnimatingListGridItems()
							|| mTransitionEffect != CorePrefs.getViewPagerEffect()||mPeopleViewType!= CorePrefs.getPeopleViewType();
		
		CorePrefs.setPrefsHaveChanged(changed);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume()
	{
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
}
