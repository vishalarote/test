package com.abewy.android.apps.contacts;

import java.util.HashMap;

import android.graphics.Typeface;

import com.abewy.android.apps.contacts.core.CoreApplication;
import com.abewy.android.apps.contacts.imageloader.ImageLoader;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

public class ContactsApplication extends CoreApplication
{
	private boolean  mIsFirstLaunch = true;
	
	public static ContactsApplication getInstance()
	{
		return (ContactsApplication) CoreApplication.getInstance();
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	@Override
	protected void initPreferences()
	{
		//PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}

	@Override
	protected void initGlobals()
	{
		//KlyphLocale.setAppLocale(KlyphLocale.getAppLocale());
	}
	
	@Override
	protected void initOthers()
	{
		ImageLoader.initImageLoader(this);
		Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf");
		Typeface.createFromAsset(getAssets(), "Roboto-Thin.ttf");
		//ImageLoader.FADE_ENABLED = KlyphPreferences.isPhotoEffectEnabled();
	}
	
	public boolean  isFirstLaunch()
	{
		return mIsFirstLaunch;
	}
	
	public void launchComplete()
	{
		mIsFirstLaunch = false;
	}
	


    // The following line should be changed to include the correct property id.
    private static final String PROPERTY_ID = "UA-52771589-2";

    public static int GENERAL_TRACKER = 0;

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

     

    synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(PROPERTY_ID)
                    : (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(
                            R.xml.global_tracker)
                            : analytics.newTracker(R.xml.ecommerce_tracker);
            t.enableAdvertisingIdCollection(true);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

}