package com.abewy.android.apps.contacts.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.abewy.android.apps.contacts.BuildConfig;
import com.abewy.android.apps.contacts.R;
import com.abewy.android.apps.contacts.search.AbstractSearchService;
import com.abewy.android.apps.contacts.search.SearchCallback;
import com.abewy.android.apps.contacts.search.SearchService;

public class DialerActivity extends Fragment  implements AdapterView.OnItemClickListener {
    private static final String TAG = DialerActivity.class.getSimpleName();
    private static final int MAX_HITS = 20;
    /**
     * Called when the activity is first created.
     */
    private AbstractSearchService searchService;
    private TextView mInput;
    private ListView list;
    private ResultAdapter resultAdapter = new ResultAdapter();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
	private RelativeLayout llLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	llLayout    = (RelativeLayout)    inflater.inflate(R.layout.main, container, false);

      
        mInput = (TextView)llLayout. findViewById(R.id.digits);
        list = ((ListView) llLayout.findViewById(R.id.list));
        list.setAdapter(resultAdapter);
        list.setOnItemClickListener(this);
        searchService = SearchService.getInstance(getActivity());
        search(null);
		return llLayout;
    }

    @Override
	public void onStop() {
        super.onStop();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStop");
        }
    }

    @Override
	public void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume");
        }
    }

    @Override
	public void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroy");
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Map<String, Object> searchRes = (Map<String, Object>) list.getAdapter().getItem(position);
        if (searchRes.containsKey(SearchService.FIELD_NUMBER)) {
            call(searchRes.get(SearchService.FIELD_NUMBER).toString());
        }
    }

    private void call(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.fromParts("tel", number, null));
        startActivity(intent);
    }

    public void onBtnClick(View view) {
        switch (view.getId()) {
            case R.id.deleteButton:
                delChar();
                break;
            default:
                addChar(((TextView) view).getText().toString());
        }
        String searchText = mInput.getText().toString();
        if (TextUtils.isEmpty(searchText)) {
            search(null);
        } else if ("*#*".equals(searchText)) {
            searchService.asyncRebuild(true);
        } else if (searchText.indexOf('*') == -1) {
            search(searchText);
        }
    }

    private void addChar(String c) {
        c = c.toLowerCase(Locale.CHINA);
        mInput.setText(mInput.getText() + String.valueOf(c.charAt(0)));
    }

    private void delChar() {
        String text = mInput.getText().toString();
        if (text.length() > 0) {
            text = text.substring(0, text.length() - 1);
            mInput.setText(text);
        }
    }

    private void search(String query) {
        SearchCallback searchCallback = new SearchCallback() {
            private long start = System.currentTimeMillis();

            @Override
            public void onSearchResult(String query, long hits,
                                       final List<Map<String, Object>> result) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        resultAdapter.setItems(result);
                        resultAdapter.notifyDataSetChanged();
                        list.smoothScrollToPosition(0);
                    }
                });
                Log.v(SearchService.TAG, "query:" + query + ",result: " + result.size() + ",time used:" + (System.currentTimeMillis() - start));
            }
        };
        searchService.query(query, MAX_HITS, true, searchCallback);
    }


    private static class ViewHolder {
        public TextView name;
        public TextView number;
    }

    private class ResultAdapter extends BaseAdapter {
        private List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();

        @Override
        public synchronized int getCount() {
            return items.size();
        }

        public synchronized void setItems(List<Map<String, Object>> items) {
            this.items = items;
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                rowView = LayoutInflater.from(getActivity()).inflate(
                        R.layout.list_item, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.name = (TextView) rowView.findViewById(R.id.name);
                viewHolder.number = (TextView) rowView
                        .findViewById(R.id.number);
                rowView.setTag(viewHolder);
            }
            ViewHolder holder = (ViewHolder) rowView.getTag();
            Map<String, Object> searchRes = items.get(position);
            StringBuilder nameBuilder = new StringBuilder();
            if (searchRes.containsKey(SearchService.FIELD_NAME)) {
                nameBuilder.append(searchRes.get(
                        SearchService.FIELD_NAME).toString());
            } else {
                nameBuilder.append("é™Œç”Ÿå�·ç �");
            }
            nameBuilder.append(' ');
            if (searchRes.containsKey(SearchService.FIELD_PINYIN)) {
                nameBuilder.append(searchRes.get(SearchService.FIELD_PINYIN).toString());
            }
            StringBuilder numberBuilder = new StringBuilder();
            if (searchRes.containsKey(SearchService.FIELD_HIGHLIGHTED_NUMBER)) {
                numberBuilder.append(searchRes.get(SearchService.FIELD_HIGHLIGHTED_NUMBER).toString());
            } else if (searchRes.containsKey(SearchService.FIELD_NUMBER)) {
                numberBuilder.append(searchRes.get(SearchService.FIELD_NUMBER));
            }
            holder.name.setText(Html.fromHtml(nameBuilder.toString()));
            holder.number.setText(Html.fromHtml(numberBuilder.toString()));
            return rowView;
        }

    }

}
