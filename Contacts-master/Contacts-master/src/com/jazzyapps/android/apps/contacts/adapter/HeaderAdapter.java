package com.jazzyapps.android.apps.contacts.adapter;

import android.view.View;
import android.widget.TextView;
import com.jazzyapps.android.apps.contacts.R;
import com.jazzyapps.android.apps.contacts.R.id;
import com.jazzyapps.android.apps.contacts.R.layout;
import com.jazzyapps.android.apps.contacts.adapter.holder.HeaderHolder;
import com.jazzyapps.android.extended.items.BaseType;
import com.jazzyapps.android.extended.items.Header;

public class HeaderAdapter extends ContactBaseAdapter
{
	public HeaderAdapter()
	{
		super();
	}
	
	@Override
	protected int getLayoutRes()
	{
		return R.layout.item_header;
	}
	
	@Override
	protected void attachViewHolder(View view)
	{
		view.setTag(new HeaderHolder((TextView) view.findViewById(R.id.header_title)));
	}
	
	@Override
	public void bindData(View view, BaseType data, int position)
	{
		Header header = (Header) data;
		
		HeaderHolder holder = (HeaderHolder) view.getTag();
		holder.headerTitle.setText(header.name);
	}

	@Override
	public boolean isEnabled(BaseType object)
	{
		return false;
	}	
}
