package com.jazzyapps.android.apps.contacts.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.jazzyapps.android.apps.contacts.R;
import com.jazzyapps.android.apps.contacts.core.CorePrefs;
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
	private int mAppThemeType;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		int theme = CorePrefs.getAppTheme();
		switch (theme) {
		case 0:
			setTheme(R.style.Theme_App_pink);
			break;
		case 1:
			setTheme(R.style.Theme_App_orange);
			break;
		case 2:
			setTheme(R.style.Theme_Blue);
			break;


		}
		super.onCreate(savedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.preferences);

		mSortByLastName = CorePrefs.isSortingByLastName();
		mRoundedAvatars = CorePrefs.isRoundedPictures();
		mListAnimation = CorePrefs.isAnimatingListGridItems();
		mTransitionEffect = CorePrefs.getViewPagerEffect();
		mPeopleViewType=CorePrefs.getPeopleViewType();

		mAppThemeType=CorePrefs.getAppTheme();


	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		boolean changed = mSortByLastName != CorePrefs.isSortingByLastName()
				|| mRoundedAvatars != CorePrefs.isRoundedPictures()
				|| mListAnimation != CorePrefs.isAnimatingListGridItems()
				|| mTransitionEffect != CorePrefs.getViewPagerEffect()||mPeopleViewType!= CorePrefs.getPeopleViewType()||mAppThemeType!= CorePrefs.getAppTheme();
		restart();
		CorePrefs.setPrefsHaveChanged(changed);
	}

	private void restart()
	{
		Intent localIntent = new Intent(this, PreferencesActivity.class);
		localIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivityForResult (localIntent,125);
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		finish();
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
