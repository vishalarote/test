package com.jazzyapps.android.apps.contacts.app;

import com.jazzyapps.android.apps.contacts.R;
import com.jazzyapps.android.apps.contacts.core.CorePrefs;

import android.app.ListFragment;
import android.os.Bundle;

public class BaseListFragment extends android.support.v4.app.ListFragment
{
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		int theme = CorePrefs.getAppTheme();
		switch (theme) {
		case 1:
			getActivity().setTheme(R.style.Theme_App_orange);
			break;

		case 0:
			getActivity().	setTheme(R.style.Theme_App_pink);
			break;
		 
		 
		}
		super.onCreate(savedInstanceState);
	}

	protected void setListVisible(boolean visible)
	{
		setListShown(visible);
		
	}

	protected void setEmptyText(int resId)
	{
		if (getActivity() != null)
			setEmptyText(getResources().getString(resId));
	}
}
