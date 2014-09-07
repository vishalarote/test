/**
 * @author Jonathan
 */

package com.jazzyapps.android.apps.contacts.app;

import com.jazzyapps.android.apps.contacts.R;
import com.jazzyapps.android.apps.contacts.adapter.LayoutType;


public class PeopleMediumGridFragment extends PeopleBigGridFragment
{
	private static int		mLastPosition		= 0;
	private static int		mLastFavPosition	= 0;
	
	public PeopleMediumGridFragment()
	{

	}
	
	@Override
	protected int getNumColumn()
	{
		return getResources().getInteger(R.integer.medium_grid_columns);
	}

	@Override
	protected int getCustomLayout()
	{
		return R.layout.grid_big;
	}
	
	protected int getAdapterLayoutType()
	{
		return LayoutType.GRID_MEDIUM;
	}
	
	@Override
	protected int getLastPosition(boolean favorite)
	{
		return favorite ? mLastFavPosition : mLastPosition;
	}

	@Override
	protected void setLastPosition(boolean favorite, int position)
	{
		if (favorite)
			mLastFavPosition = position;
		else
			mLastPosition = position;
	}
}
