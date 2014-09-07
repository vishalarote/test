package com.jazzyapps.android.apps.contacts.adapter;

import android.provider.ContactsContract.CommonDataKinds.Email;
import android.view.View;
import android.widget.TextView;
import com.jazzyapps.android.apps.contacts.R;
import com.jazzyapps.android.apps.contacts.adapter.holder.TitleTextHolder;
import com.jazzyapps.android.apps.contacts.model.ContactEmail;
import com.jazzyapps.android.extended.items.BaseType;

public class ContactEmailAdapter extends ContactBaseAdapter
{
	public ContactEmailAdapter()
	{
		super();
	}

	@Override
	protected int getLayoutRes()
	{
		return R.layout.item_title_text;
	}

	@Override
	protected void attachViewHolder(View view)
	{
		TextView primaryText = (TextView) view.findViewById(R.id.primary_text);
		TextView secondaryText = (TextView) view.findViewById(R.id.secondary_text);

		setHolder(view, new TitleTextHolder(primaryText, secondaryText));
	}

	@Override
	public void bindData(View view, BaseType data, int position)
	{
		final TitleTextHolder holder = (TitleTextHolder) getHolder(view);

		final ContactEmail email = ((ContactEmail) data);

		holder.primaryText.setText(email.email);
		
		Email.getTypeLabel(holder.primaryText.getResources(), email.type, email.label);
	}
}
