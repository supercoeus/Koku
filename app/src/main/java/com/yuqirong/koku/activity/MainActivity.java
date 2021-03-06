package com.yuqirong.koku.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.*;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.yuqirong.koku.R;
import com.yuqirong.koku.adapter.FragmentAdapter;
import com.yuqirong.koku.constant.AppConstant;
import com.yuqirong.koku.fragment.FragmentFactory;
import com.yuqirong.koku.fragment.WeiboTimeLineFragment;
import com.yuqirong.koku.receiver.RefreshWeiboTimelineReceiver;
import com.yuqirong.koku.service.CheckUnreadService;
import com.yuqirong.koku.util.CommonUtil;
import com.yuqirong.koku.util.LogUtils;
import com.yuqirong.koku.util.SharePrefUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 主页面
 */
public class MainActivity extends BaseActivity implements RefreshWeiboTimelineReceiver.OnUpdateUIListener {

    private DrawerLayout mDrawerLayout; //抽屉
    private ActionBarDrawerToggle toggle; //在ActionBar上的抽屉按钮
    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private boolean isDrawerOpened = false; //判断DrawerLayout是否打开

    private FloatingActionButton mFloatingActionButton;
    private Handler handler = new Handler();
    private ViewPager mViewPager;

    private FragmentAdapter adapter;
    private FragmentManager fm;
    public static final int SEND_NEW_WEIBO = 1001;
    public static final int SEND_NEW_COMMENT = 1003;
    public static final int SEND_NEW_REPOST = 1005;
    private boolean isLastShown = false; //用于fabbutton上一个状态是否显示
    private long firstTime;
    private CoordinatorLayout mCoordinatorLayout;
    private RefreshWeiboTimelineReceiver receiver;
    private TextView tv_unread_remind;

    @Override
    protected void initData(Bundle savedInstanceState) {
        //检查token是否过期
        checkTokenExpireIn();
        //开启查询未读微博的service
        Intent intent = new Intent(MainActivity.this, CheckUnreadService.class);
        intent.setAction(CheckUnreadService.START_TIMER);
        startService(intent);
        //注册广播，用于监听屏幕开启，屏幕关闭，以及未读微博的更新提醒
        receiver = new RefreshWeiboTimelineReceiver();
        receiver.setOnUpdateUIListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(RefreshWeiboTimelineReceiver.INTENT_UNREAD_UPDATE); //更新未读
        filter.addAction(RefreshWeiboTimelineReceiver.INTENT_FAB_CHANGE); //改变fab
        filter.addAction(Intent.ACTION_SCREEN_OFF); //关闭屏幕
        filter.addAction(Intent.ACTION_USER_PRESENT); //解锁屏幕
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(MainActivity.this, CheckUnreadService.class));
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        super.onDestroy();
    }

    //查询用户access_token的授权相关信息
    private void checkTokenExpireIn() {
        final String access_token = SharePrefUtil.getString(this, "access_token", "");
        if (access_token != "") {
            String url = AppConstant.GET_TOKEN_INFO_URL + "?access_token=" + access_token;
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    try {
                        String expire_in = jsonObject.getString("expire_in");
                        int second = Integer.parseInt(expire_in);
                        if (second > 0) {
                            SharePrefUtil.saveString(MainActivity.this, "expire_in", expire_in);
                        } else {
                            goToAuthorizeActivity();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, errorListener) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("access_token", access_token);
                    return map;
                }
            };
            mQueue.add(jsonObjectRequest);
        } else {
            goToAuthorizeActivity();
        }
    }

    private void goToAuthorizeActivity() {
        AuthorizeActivity.actionStart(this);
        finish();
    }

    Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            LogUtils.i(volleyError.toString());
        }
    };

    @Override
    protected void initToolBar() {
        mToolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

        }
        toggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                if (isLastShown) {
                    mFloatingActionButton.show();
                }
                isDrawerOpened = false;
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                isDrawerOpened = true;
                isLastShown = mFloatingActionButton.isShown();
                mFloatingActionButton.hide();
                super.onDrawerOpened(drawerView);
            }
        };
        //把toggle和ActionBar同步状态
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();
    }


    @Override
    protected void initView() {
        setContentView(R.layout.activity_main);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.mDrawerLayout);
        mToolbar = (Toolbar) findViewById(R.id.mToolbar);
        mTabLayout = (TabLayout) findViewById(R.id.mTabLayout);
        if (mTabLayout != null) {
            setupTabLayoutContent(mTabLayout);
        }
        NavigationView mNavigationView = (NavigationView) findViewById(R.id.mNavigationView);
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.mFloatingActionButton);
        mViewPager = (ViewPager) findViewById(R.id.mViewPager);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.mCoordinatorLayout);
        tv_unread_remind = (TextView) findViewById(R.id.tv_unread_remind);
        if (adapter == null) {
            setupViewPagerContent();
        }
        if (mNavigationView != null) {
            setupDrawerContent(mNavigationView);
        }
        if (mFloatingActionButton != null) {
            setupFloatingActionButtonContent(mFloatingActionButton);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case SEND_NEW_WEIBO:
                if (resultCode == PublishActivity.SEND_WEIBO_SUCCESS) {
                    LogUtils.i("send weibo success");
                    CommonUtil.showNotification(MainActivity.this, R.string.send_remind, R.string.send_success, R.drawable.ic_done_light, true);
                    final Fragment item = adapter.getItem(0);
                    View rootView = item.getView();
                    CommonUtil.showSnackbar(rootView, R.string.publish_success, getResources().getColor(R.color.Indigo_colorPrimary), Snackbar.LENGTH_LONG, R.string.click_to_refresh, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mViewPager.setCurrentItem(0, true);
                            ((WeiboTimeLineFragment) item).refreshWeibo();
                        }
                    });
                }
                break;
            case SEND_NEW_COMMENT:
                if (resultCode == PublishActivity.SEND_COMMENT_SUCCESS) {
                    LogUtils.i("send comment success");
                    CommonUtil.showNotification(MainActivity.this, R.string.send_remind, R.string.send_success, R.drawable.ic_done_light, true);
                }
                break;
            case SEND_NEW_REPOST:
                if (resultCode == PublishActivity.SEND_REPOST_SUCCESS) {
                    LogUtils.i("send repost success");
                    CommonUtil.showNotification(MainActivity.this, R.string.send_remind, R.string.send_success, R.drawable.ic_done_light, true);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setupTabLayoutContent(TabLayout mTabLayout) {
        mTabLayout.setSelectedTabIndicatorColor(getResources().getColor(android.R.color.white));
        mTabLayout.setSelectedTabIndicatorHeight(CommonUtil.dip2px(this, 2));
        mTabLayout.setTabTextColors(getResources().getColor(R.color.unselected_text_color), getResources().getColor(android.R.color.white));
    }

    private void setupFloatingActionButtonContent(FloatingActionButton mFloatingActionButton) {
        //Fab功能
        final int flag = SharePrefUtil.getInt(this, "fab_function", 0);
        if (0 == flag) {
            mFloatingActionButton.setImageResource(R.drawable.ic_create_light);
        } else {
            mFloatingActionButton.setImageResource(R.drawable.ic_refresh_light);
        }
        // Fab位置
        final int position = SharePrefUtil.getInt(this, "fab_position", 1);
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) mFloatingActionButton.getLayoutParams();
        if (1 == position) {
            layoutParams.anchorGravity = Gravity.END | Gravity.BOTTOM | Gravity.RIGHT;
        } else {
            layoutParams.anchorGravity = Gravity.START | Gravity.BOTTOM | Gravity.LEFT;
        }
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (0 == flag) {
                    // 得到状态栏的高度
                    DisplayMetrics dm = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(dm);
                    int topOffset = dm.heightPixels - mCoordinatorLayout.getMeasuredHeight();

                    int[] startingLocation = new int[2];
                    view.getLocationOnScreen(startingLocation);
                    startingLocation[0] += view.getWidth() / 2;
                    startingLocation[1] = startingLocation[1] - topOffset + view.getHeight() / 2;
                    Intent intent = new Intent(MainActivity.this, PublishActivity.class);
                    intent.putExtra("type", PublishActivity.SEND_WEIBO);
                    intent.putExtra("isFromFAButton", true);
                    intent.putExtra(PublishActivity.ARG_REVEAL_START_LOCATION, startingLocation);
                    startActivityForResult(intent, SEND_NEW_WEIBO);
                    overridePendingTransition(0, 0);
                } else {
                    Fragment frag = adapter.getItem(mViewPager.getCurrentItem());
                    ((WeiboTimeLineFragment) frag).refreshWeibo();
                }
            }
        });
    }

    //设置ViewPager内容
    private void setupViewPagerContent() {
        fm = getSupportFragmentManager();
        adapter = new FragmentAdapter(fm);
        adapter.addFragment(FragmentFactory.newInstance(AppConstant.HOME_TIMELINE_URL), getResources().getString(R.string.all_weibo));
        adapter.addFragment(FragmentFactory.newInstance(AppConstant.STATUSES_BILATERAL_TIMELINE_URL), getResources().getString(R.string.bilateral_weibo));
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    //设置navigationView内容
    private void setupDrawerContent(NavigationView navigationView) {

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        LogUtils.i(menuItem.getItemId() + "");
                        menuItem.setCheckable(false);
                        switch (menuItem.getItemId()) {
                            case R.id.nav_search:
                                SearchUserActivity.actionStart(MainActivity.this, SearchUserActivity.SEARCH_USER);
                                break;
                            case R.id.nav_nearly:
                                NearlyDynamicActivity.actionStart(MainActivity.this);
                                break;
                            case R.id.nav_hot:
                                PublicWeiboActivity.actionStart(MainActivity.this);
                                break;
                            case R.id.nav_favorite:
                                MyFavoriteActivity.actionStart(MainActivity.this);
                                break;
                            case R.id.nav_draft:
                                DraftActivity.actionStart(MainActivity.this);
                                break;
                        }
                        return true;
                    }
                });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remind:
                RemindActivity.actionStart(MainActivity.this);
                break;
            case R.id.action_send_weibo:
                Intent intent = new Intent();
                intent.setClass(this, PublishActivity.class);
                intent.putExtra("type", PublishActivity.SEND_WEIBO);
                startActivity(intent);
                break;
            case R.id.action_settings:
                SettingsActivity.actionStart(MainActivity.this);
                break;
            case R.id.action_about:
                AboutActivity.actionStart(MainActivity.this);
                break;
            case R.id.action_exit:
                finish();
                removeAllActivities();
                break;

        }
        return toggle.onOptionsItemSelected(item) | super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isDrawerOpened) {
                mDrawerLayout.closeDrawers();
                isDrawerOpened = false;
            } else {
                long secondTime = System.currentTimeMillis();
                if (secondTime - firstTime > 2000) {
                    CommonUtil.showSnackbar(mViewPager.getChildAt(mViewPager.getCurrentItem()), R.string.again_to_exit, getResources().getColor(R.color.Indigo_colorPrimary));
                    firstTime = secondTime;
                } else {
                    finish();
                    removeAllActivities();
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void updateUI(int unreadCount) {
        String title = String.format(getString(R.string.unread_weibo), unreadCount);
        tv_unread_remind.setText(title);
    }

    @Override
    public void updateFab() {
        setupFloatingActionButtonContent(mFloatingActionButton);
    }
}
