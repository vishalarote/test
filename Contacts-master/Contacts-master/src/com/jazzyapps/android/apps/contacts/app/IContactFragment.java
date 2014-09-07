/**
* @author Jonathan
*/

package com.jazzyapps.android.apps.contacts.app;

import java.util.List;
import com.jazzyapps.android.apps.contacts.model.Contact;
import com.jazzyapps.android.extended.items.BaseType;

public interface IContactFragment
{
	public void setContacts(List<BaseType> contacts);
	public void filter(String filter);
	public List<Contact> getDisplayedContacts();
}
