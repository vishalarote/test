package com.abewy.android.apps.contacts.app;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.abewy.android.apps.contacts.ContactsApplication;
import com.abewy.android.apps.contacts.R;
import com.abewy.android.apps.contacts.core.CorePrefs;

public class BaseFragmentActivity extends FragmentActivity {
	 @Override
	protected void onCreate(Bundle arg0) {
		int theme = CorePrefs.getAppTheme();
		switch (theme) {
		case 1:
			setTheme(R.style.Theme_App_orange);
			break;

		case 0:
			setTheme(R.style.Theme_App_pink);
			break;
		 
		 
		}
		

		super.onCreate(arg0);
	}

}
