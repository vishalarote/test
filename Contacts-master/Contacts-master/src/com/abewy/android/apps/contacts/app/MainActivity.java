package com.abewy.android.apps.contacts.app;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.abewy.android.apps.contacts.R;
import com.abewy.android.apps.contacts.adapter.LayoutType;
import com.abewy.android.apps.contacts.adapter.MultiObjectAdapter;
import com.abewy.android.apps.contacts.core.CoreApplication;
import com.abewy.android.apps.contacts.core.CorePrefs;
import com.abewy.android.apps.contacts.iab.IabHelper;
import com.abewy.android.apps.contacts.iab.IabResult;
import com.abewy.android.apps.contacts.iab.Inventory;
import com.abewy.android.apps.contacts.iab.Purchase;
import com.abewy.android.apps.contacts.util.EasyRatingDialog;
import com.abewy.android.extended.items.BaseType;
import com.jfeinstein.jazzyviewpager.JazzyViewPager;
import com.jfeinstein.jazzyviewpager.JazzyViewPager.TransitionEffect;
import com.ptr.folding.FoldingDrawerLayout;

public class MainActivity extends BaseFragmentActivity implements   IActionbarSpinner
{
	private static final int		SETTINGS_CODE		= 125;
	private DialerActivity dialerFragment;
	private SectionsPagerAdapter	mSectionsPagerAdapter;
	private JazzyViewPager			mViewPager;
	private SearchView				mSearchView;
	private TransitionEffect		mCurrentEffect;
	private int						mCurrentFragmentIndex;
	private OnQueryTextListener		mQueryTextListener	= new OnQueryTextListener() {

		@Override
		public boolean onQueryTextSubmit(String query)
		{
			return false;
		}

		@Override
		public boolean onQueryTextChange(String newText)
		{
			mSectionsPagerAdapter.setSearchQuery(newText);
			return true;
		}
	};
	private OnActionExpandListener	mExpandListener		= new OnActionExpandListener() {

		@Override
		public boolean onMenuItemActionExpand(MenuItem item)
		{
			mViewPager.setPagingEnabled(false);
			mSearchView.setOnQueryTextListener(mQueryTextListener);
			return true;
		}

		@Override
		public boolean onMenuItemActionCollapse(MenuItem item)
		{
			Log.d("MainActivity", "onMenuItemActionCollapse: ");
			mViewPager.setPagingEnabled(true);
			mSearchView.setOnQueryTextListener(null);
			mSectionsPagerAdapter.setSearchQuery(null);
			return true;
		}
	};

	private String[] mAnimalTitles;

	private FoldingDrawerLayout mDrawerLayout;

	private ListView mDrawerList;

	private ActionBar actionBar;

	private ItemSelectedListener mItemSelectedListener;

	private ActionBarDrawerToggle mDrawerToggle;
	public Context mContext;
	private EasyRatingDialog easyRatingDialog;
	public   void onBtnClick(View view) {
		dialerFragment.onBtnClick(view);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//Crashlytics.start(this);
		setContentView(R.layout.activity_main);
		easyRatingDialog = new EasyRatingDialog(this);

		actionBar = getActionBar();

		mContext=this;
		// Specify that tabs should be displayed in the action bar.
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mAnimalTitles = getResources().getStringArray(R.array.animal_array);
		mDrawerLayout = (FoldingDrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		// set up the drawer's list view with items and click listener
		mDrawerList.setAdapter(new MySimpleArrayAdapter(this,mAnimalTitles));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mItemSelectedListener = new ItemSelectedListener();

		// enable ActionBar app icon to behave as action to toggle nav drawer
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
				mDrawerLayout, /* DrawerLayout object */
				R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
				R.string.drawer_open, /* "open drawer" description for accessibility */
				R.string.drawer_close /* "close drawer" description for accessibility */
				) {

			public void onDrawerClosed(View view) {
				actionBar.setTitle("Contacts");
				invalidateOptionsMenu(); // creates call to
				// onPrepareOptionsMenu()
			}

			public void onDrawerSlide(View drawerView, float slideOffset) {

			}

			public void onDrawerOpened(View drawerView) {
				actionBar.setTitle("Contacts");
				invalidateOptionsMenu(); // creates call to
				// onPrepareOptionsMenu()
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);






		String base64EncodedPublicKey = CoreApplication.generateIabKey();

		// compute your public key and store it in base64EncodedPublicKey
		mHelper = new IabHelper(this, base64EncodedPublicKey);

		// enable debug logging (for a production application, you should set this to false).
		// mHelper.enableDebugLogging(true);

		mViewPager = (JazzyViewPager) findViewById(R.id.pager);

		// CorePrefs.setHasDonated(false);

		setupViewPager();



		if (!CorePrefs.hasDonated())
		{
			launchIab();
		}


		mSectionsPagerAdapter.addTab(actionBar.newTab().  setIcon(R.drawable.ic_action_users),	FragmentContainer.class, null);
		mSectionsPagerAdapter.addTab(actionBar.newTab(). setIcon(R.drawable.ic_action_star),	FavoritesFragmentContainer.class, null);
		mSectionsPagerAdapter.addTab(actionBar.newTab().  setIcon(R.drawable.ic_phone_default),	DialerActivity.class, null);


	}

	private void setupViewPager()
	{
		mCurrentEffect = CorePrefs.getViewPagerEffect();
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		mViewPager.setTransitionEffect(mCurrentEffect);
		mViewPager.setFadeEnabled(false);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setPageMargin(30);
		mViewPager.setCurrentItem(mCurrentFragmentIndex);
		mViewPager.setOnPageChangeListener(mSectionsPagerAdapter);
		//	mViewPager.setPagingEnabled(!CorePrefs.isFirstLaunch());

		// mSectionsPagerAdapter.setData(contacts, favContacts);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == SETTINGS_CODE)
		{
			if (CorePrefs.getPrefsHaveChanged())
			{
				CorePrefs.setPrefsHaveChanged(false);
				restart();
			}
		}

		if (mHelper == null)
			return;

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data))
		{
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		}
		else
		{
			Log.d(TAG, "onActivityResult handled by IABUtil.");
		}
	}
	@Override
	protected void onStart() {
		super.onStart();
		easyRatingDialog.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		easyRatingDialog.showIfNeeded(this);
	}
	private void restart()
	{
		Intent localIntent = new Intent(this, MainActivity.class);
		localIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(localIntent);
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		getMenuInflater().inflate(R.menu.main, menu);

		MenuItem item = menu.findItem(R.id.action_search);
		mSearchView = (SearchView) item.getActionView();

		item.setOnActionExpandListener(mExpandListener);

		if (!CorePrefs.hasDonated())
		{
			menu.add(Menu.NONE, R.id.action_help_me, 99, R.string.action_help_me);
		}


		return true;
	}

	// ___ Tabs management
	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		return super.onPrepareOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.action_add_contact)
		{
			// Creates a new Intent to insert a contact
			Intent intent = new Intent(Intents.Insert.ACTION);
			// Sets the MIME type to match the Contacts Provider
			intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
			intent.putExtra("finishActivityOnSaveCompleted", true);
			startActivity(intent);
			return true;
		}

		if (item.getItemId() == R.id.action_help_me)
		{
			startActivity(new Intent(this, HelpMeActivity.class));
			return true;
		}

		if (item.getItemId() == R.id.action_settings)
		{
			mCurrentFragmentIndex = mViewPager.getCurrentItem();
			startActivityForResult(new Intent(this, PreferencesActivity.class), SETTINGS_CODE);
			return true;
		}
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}



	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		if (mHelper != null)
		{
			mHelper.dispose();
			mHelper = null;
		}

		if (mSectionsPagerAdapter != null)
			mSectionsPagerAdapter.onDestroy();

		if (mViewPager != null)
		{
			mViewPager.setOnPageChangeListener(null);
		}

		mSectionsPagerAdapter = null;
		mViewPager = null;
		mSearchView = null;
	}
	static final class TabInfo {
		private final Class<?> clss;
		private final Bundle args;

		TabInfo(Class<?> _class, Bundle _args) {
			clss = _class;
			args = _args;
		}
	}
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener,ActionBar.TabListener
	{
		private FragmentContainer	peopleFragment;
		private FragmentContainer	favoritesFragment;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

		public SectionsPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			if (position == 0)
			{
				peopleFragment = new FragmentContainer();

				return peopleFragment;
			}
			else if(position == 1)

			{
				favoritesFragment = new FavoritesFragmentContainer();

				return favoritesFragment;
			}else 
			{
				dialerFragment= new DialerActivity();
				return dialerFragment;
			} 

		}
		public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {

			TabInfo info = new TabInfo(clss, args);
			tab.setTag(info);
			tab.setTabListener(this);
			mTabs.add(info);

			actionBar.addTab(tab);
			notifyDataSetChanged();
		}

		@Override
		public int getItemPosition(Object object)
		{
			return POSITION_NONE;
		}

		@Override
		public int getCount()
		{
			return  mTabs.size();
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			/*Locale l = Locale.getDefault();

			switch (position)
			{
				case 0:
				{
					return getString(R.string.title_section1).toUpperCase(l);
				}
				case 1:
				{
					return getString(R.string.title_section2).toUpperCase(l);
				}
			}*/

			return null;
		}

		@Override
		public Object instantiateItem(ViewGroup container, final int position)
		{
			Object obj = super.instantiateItem(container, position);
			mViewPager.setObjectForPosition(obj, position);
			return obj;
		}

		public void setSearchQuery(String query)
		{
			if (mViewPager.getCurrentItem() == 0)
				peopleFragment.searchQuery(query);
			else
				favoritesFragment.searchQuery(query);
		}

		/*public void destroyItem(ViewGroup container, int position, Object obj)
		{
			container.removeView(mViewPager.findViewFromObject(position));
			((JazzyViewPager)container).removeObject(position);
		}
		 */
		public void onDestroy()
		{
			peopleFragment = null;
			favoritesFragment = null;
		}

		@Override
		public void onPageSelected(int position)
		{
			actionBar.setSelectedNavigationItem(position);

			/*if (peopleFragment == null)
				return;

			if (position == 0)
			{
				peopleFragment.onSetToFront();
				favoritesFragment.onSetToBack();
			}
			else
			{
				favoritesFragment.onSetToFront();
				peopleFragment.onSetToBack();
			}*/
		}

		@Override
		public void onPageScrollStateChanged(int state)
		{

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{

		}

		@Override
		public void onTabReselected(Tab arg0, FragmentTransaction arg1) {


		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Object tag = tab.getTag();
			for (int i=0; i<mTabs.size(); i++) {
				if (mTabs.get(i) == tag) {
					mViewPager.setCurrentItem(i);
				}
			}
			mDrawerLayout.closeDrawer(mDrawerList);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub

		}
	}

	// ___ InApp Billing
	private IabHelper							mHelper;

	private static final String					TAG						= "MainActivity";

	// Listener that's called when we finish querying the items and subscriptions we own
	IabHelper.QueryInventoryFinishedListener	mGotInventoryListener	= new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory)
		{
			Log.d(TAG, "Query inventory finished.");
			// MessengerApplication.PRO_VERSION_CHECKED = true;
			// Have we been disposed of in the meantime? If so, quit.
			if (mHelper == null)
				return;

			// Is it a failure?
			if (result.isFailure())
			{
				Log.d(TAG, "Failed to query inventory: " + result);
				// Fail to check, so we don't display ads
				// to avoid pro users to see ads
				// MessengerApplication.IS_PRO_VERSION = true;
				return;
			}

			Log.d(TAG, "Query inventory was successful.");

			/*
			 * Check for items we own. Notice that for each purchase, we check
			 * the developer payload to see if it's correct! See
			 * verifyDeveloperPayload().
			 */

			String[] skus = getResources().getStringArray(R.array.donate_values);

			for (String sku : skus)
			{
				Purchase donation = inventory.getPurchase(sku);

				if (donation != null)
				{
					// mHelper.consumeAsync(donation, null);
					CorePrefs.setHasDonated(true);
					break;
				}
			}

			if (CorePrefs.hasDonated())
				invalidateOptionsMenu();

			Log.d(TAG, "Initial inventory query finished; enabling main UI.");
		}
	};

	private void launchIab()
	{
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result)
			{
				Log.d(TAG, "Setup finished.");

				if (!result.isSuccess())
				{
					// Oh noes, there was a problem.
					Log.d("MainActivity.onCreate(...).new OnIabSetupFinishedListener() {...}",
							"onIabSetupFinished: Problem setting up in-app billing: " + result);
					return;
				}

				// Have we been disposed of in the meantime? If so, quit.
				if (mHelper == null)
					return;

				// IAB is fully set up. Now, let's get an inventory of stuff we own.
				Log.d(TAG, "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(mGotInventoryListener);
			}
		});

	}

	@Override
	public void displaySpinnerInActionBar(String[] array, int position, OnNavigationListener listener)
	{
		ArrayAdapter<CharSequence> list = new ArrayAdapter<CharSequence>(getActionBar().getThemedContext(),
				android.R.layout.simple_dropdown_item_1line, array);
		list.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

		getActionBar().setNavigationMode(ActionBar. NAVIGATION_MODE_LIST);  
		getActionBar().setListNavigationCallbacks(list, listener);
		getActionBar().setSelectedNavigationItem(position);
	}

	@Override
	public void displaySpinnerInActionBar(int array, int position, OnNavigationListener listener)
	{
		ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(getActionBar().getThemedContext(), array,
				android.R.layout.simple_dropdown_item_1line);
		list.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

		//	getActionBar().setNavigationMode(ActionBar.  NAVIGATION_MODE_LIST);
		//	getActionBar().setListNavigationCallbacks(list, listener);
		//getActionBar().setSelectedNavigationItem(position);
	}

	@Override
	public void displaySpinnerInActionBar(List<BaseType> data, int position, OnNavigationListener listener)
	{
		MultiObjectAdapter adapter = new MultiObjectAdapter(null, LayoutType.DROP_DOWN_ITEM);
		adapter.addAll(data);

		//getActionBar().setNavigationMode(ActionBar. NAVIGATION_MODE_LIST);
		//	getActionBar().setListNavigationCallbacks(adapter, listener);
		//	getActionBar().setSelectedNavigationItem(position);
	}

	@Override
	public void removeSpinnerInActionBar()
	{
		// getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		//  getActionBar().setListNavigationCallbacks(null, null);
	}

	/* The click listner for ListView in the navigation drawer */
	private class DrawerItemClickListener implements
	ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position);
		}
	}

	private void selectItem(int position) {
		// update the main content by replacing fragments

		switch(position){
		
		case 0 :
			String appPackage = mContext.getPackageName();
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackage));
		mContext.startActivity(intent);
		break;
		case 3:	startActivityForResult(new Intent(this, PreferencesActivity.class), SETTINGS_CODE);			break;

		}


		mDrawerLayout.closeDrawer(mDrawerList);
	}

	/**
	 * Listens for selection events of the spinner located on the action bar.
	 * Every time a new value is selected, the number of folds in the folding
	 * view is updated and is also restored to a default unfolded state.
	 */
	private class ItemSelectedListener implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			int mNumberOfFolds = Integer.parseInt(parent.getItemAtPosition(pos)
					.toString());

			mDrawerLayout.getFoldingLayout(mDrawerList).setNumberOfFolds(
					mNumberOfFolds);

		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}
	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	public class MySimpleArrayAdapter extends ArrayAdapter<String> {
		private final Context context;
		private final String[] values;

		public MySimpleArrayAdapter(Context context, String[] values) {
			super(context, R.layout.drawer_list_item, values);
			this.context = context;
			this.values = values;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.drawer_list_item, parent, false);
			TextView textView = (TextView) rowView.findViewById(R.id.menutxt);
			ImageView imageView = (ImageView) rowView.findViewById(R.id.imageViewicon);
			textView.setText(values[position]);
			// Change the icon for Windows and iPhone
			String s = values[position];

			//    imageView.setImageResource(R.drawable.no);


			return rowView;
		}
	} 
}
