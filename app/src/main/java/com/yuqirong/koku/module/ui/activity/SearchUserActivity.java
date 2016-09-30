package com.yuqirong.koku.module.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.yuqirong.koku.R;
import com.yuqirong.koku.app.AppConstant;
import com.yuqirong.koku.module.ui.fragment.SearchUserFragment;
import com.yuqirong.koku.util.LogUtils;
import com.yuqirong.koku.util.SharePrefUtil;
import com.yuqirong.koku.module.ui.weidgt.swipeback.SwipeBackLayout;
import com.yuqirong.koku.module.ui.weidgt.swipeback.app.SwipeBackActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import butterknife.BindView;


/**
 * 搜索用户
 * Created by Anyway on 2015/9/8.
 */
public class SearchUserActivity extends SwipeBackActivity {

    @BindView(R.id.mToolbar)
    Toolbar mToolbar;
    private FragmentManager fm;
    public static final int SEARCH_USER = 1050;
    public static final int AT_USER = 1060;
    private ActionBar actionBar;

    @Override
    protected void initData(Bundle savedInstanceState) {
        int type = getIntent().getIntExtra("type", 0);
        fm = getSupportFragmentManager();
        SearchUserFragment fragment = new SearchUserFragment();
        Bundle bundle = new Bundle();
        if (type == SEARCH_USER) {
            bundle.putInt("type", SEARCH_USER);
            fragment.setArguments(bundle);
            actionBar.setTitle(R.string.serach_user);
        } else if (type == AT_USER) {
            bundle.putInt("type", AT_USER);
            actionBar.setTitle(R.string.at_friends);
        }
        fragment.setArguments(bundle);
        fm.beginTransaction().replace(R.id.mFrameLayout, fragment, "SearchUserFragment").commit();
    }

    @Override
    protected void initView() {
        mToolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(mToolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        SwipeBackLayout mSwipeBackLayout = getSwipeBackLayout();
        mSwipeBackLayout.setEdgeTrackingEnabled(SwipeBackLayout.EDGE_LEFT);
    }

    @Override
    public int getContentViewId() {
        return R.layout.activity_search_user;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(onQueryTextListener);
        return true;
    }

    SearchView.OnQueryTextListener onQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String s) {
            try {
                if (!TextUtils.isEmpty(s)) {
                    String url = AppConstant.SEARCH_USER_URL + "?count=20&q=" + URLEncoder.encode(s, "UTF-8") + "&access_token=" + SharePrefUtil.getString(SearchUserActivity.this, "access_token", "");
                    LogUtils.i(url);
                    SearchUserFragment searchUserFragment = (SearchUserFragment) fm.findFragmentByTag("SearchUserFragment");
                    getData(url, searchUserFragment.getSearchlistener(), searchUserFragment.getErrorListener());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            return true;
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void actionStart(Context context, int type) {
        Intent intent = new Intent(context, SearchUserActivity.class);
        intent.putExtra("type", type);
        context.startActivity(intent);
    }

}
