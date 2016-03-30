package com.awesome.byunghwa.app.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.awesome.byunghwa.app.xyzreader.R;
import com.awesome.byunghwa.app.xyzreader.application.MyApplication;
import com.awesome.byunghwa.app.xyzreader.data.ArticleLoader;
import com.awesome.byunghwa.app.xyzreader.data.UpdaterService;
import com.awesome.byunghwa.app.xyzreader.util.LogUtil;
import com.awesome.byunghwa.app.xyzreader.util.NetworkUtil;
import com.awesome.byunghwa.app.xyzreader.util.Utils;

import java.util.List;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private static final String TAG = "MainActivity";
    private static final boolean DEBUG = true;

    public static final String EXTRA_CURRENT_ITEM_POSITION = "extra_current_item_position";
    public static final String EXTRA_OLD_ITEM_POSITION = "extra_old_item_position";

    private Bundle mTmpState;
    private static boolean mIsReentering;

    //public static final String[] transitionNames = MyApplication.instance.getResources().getStringArray(R.array.transition_name_array);

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            LogUtil.log_i("onMapSharedElements(List<String>, Map<String, View>)", mIsReentering);
            if (mIsReentering) {
                int oldPosition = mTmpState.getInt(EXTRA_OLD_ITEM_POSITION);
                int currentPosition = mTmpState.getInt(EXTRA_CURRENT_ITEM_POSITION);
                if (currentPosition != oldPosition) {
                    // If currentPosition != oldPosition the user must have swiped to a different
                    // page in the DetailsActivity. We must update the shared element so that the
                    // correct one falls into place.
                    String newTransitionName = String.valueOf(currentPosition);
                    View newSharedView = mRecyclerView.findViewWithTag(newTransitionName);
                    if (newSharedView != null) {
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedView);
                    }
                }
                mTmpState = null;
            }

            if (!mIsReentering) {
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                int actionBarId = getResources().getIdentifier("action_bar_container", "id", "android");
                View actionBar = findViewById(actionBarId);

                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
                if (actionBar != null) {
                    actionBar.setTransitionName("actionBar");
                    names.add(actionBar.getTransitionName());
                    sharedElements.put(actionBar.getTransitionName(), actionBar);
                }
            } else {
                names.remove(Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME);
                sharedElements.remove(Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME);
                names.remove(Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME);
                sharedElements.remove(Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME);
                names.remove("actionBar");
                sharedElements.remove("actionBar");
            }

            LogUtil.log_i("=== names: " + names.toString(), mIsReentering);
            LogUtil.log_i("=== sharedElements: " + Utils.setToString(sharedElements.keySet()), mIsReentering);
        }

        @Override
        public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements,
                                         List<View> sharedElementSnapshots) {
            LogUtil.log_i("onSharedElementStart(List<String>, List<View>, List<View>)", mIsReentering);
            logSharedElementsInfo(sharedElementNames, sharedElements);
        }

        @Override
        public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements,
                                       List<View> sharedElementSnapshots) {
            LogUtil.log_i("onSharedElementEnd(List<String>, List<View>, List<View>)", mIsReentering);
            logSharedElementsInfo(sharedElementNames, sharedElements);
        }

        private void logSharedElementsInfo(List<String> names, List<View> sharedElements) {
            LogUtil.log_i("=== names: " + names.toString(), mIsReentering);
            for (View view : sharedElements) {
                int[] loc = new int[2];
                view.getLocationInWindow(loc);
                Log.i(TAG, "=== " + view.getTransitionName() + ": " + "(" + loc[0] + ", " + loc[1] + ")");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        setExitSharedElementCallback(mCallback);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        // set the listener to be notified when a refresh is triggered via the swipe gesture
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            if (NetworkUtil.isNetworkAvailable(this)) {
                refresh();
            } else {
                Toast.makeText(this, getResources().getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        ArticleListRecyclerViewAdapter articleListRecyclerViewAdapter = new ArticleListRecyclerViewAdapter(cursor);
        articleListRecyclerViewAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(articleListRecyclerViewAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        LogUtil.log_i("onActivityReenter(int, Intent)", true);
        super.onActivityReenter(requestCode, data);
        mIsReentering = true;
        mTmpState = new Bundle(data.getExtras());
        int oldPosition = mTmpState.getInt(EXTRA_OLD_ITEM_POSITION);
        int currentPosition = mTmpState.getInt(EXTRA_CURRENT_ITEM_POSITION);
        if (oldPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }
        postponeEnterTransition();
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: hack! not sure why, but requesting a layout pass is necessary in order to fix re-mapping + scrolling glitches!
                mRecyclerView.requestLayout();
                startPostponedEnterTransition();
                return true;
            }
        });
    }

    class ArticleListRecyclerViewAdapter extends RecyclerView.Adapter<ArticleListRecyclerViewAdapter.ViewHolder> {

        private static final String TAG = "ArticleListRecyclerViewAdapter";

        private Cursor mCursor;


        public ArticleListRecyclerViewAdapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_article, parent, false);
            return new ViewHolder(v);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(position);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

        protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public DynamicHeightNetworkImageView thumbnailView;
            //public ImageView thumbnailView;
            public TextView titleView;
            public TextView subtitleView;
            private int mPosition;

            public ViewHolder(View view) {
                super(view);
                thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
                //thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
                titleView = (TextView) view.findViewById(R.id.article_title);
                subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
                view.setOnClickListener(this);
            }

            public void bind(int position) {
                mCursor.moveToPosition(position);
                titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
                subtitleView.setText(
                        DateUtils.getRelativeTimeSpanString(
                                mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR));
                thumbnailView.setImageUrl(
                        mCursor.getString(ArticleLoader.Query.THUMB_URL),
                        ImageLoaderHelper.getInstance(MyApplication.instance).getImageLoader());
                LogUtil.log_i(TAG, "position: " + position + ", photo url: " + mCursor.getString(ArticleLoader.Query.THUMB_URL));
                //Picasso.with(thumbnailView.getContext()).load(mCursor.getString(ArticleLoader.Query.THUMB_URL)).placeholder(R.mipmap.ic_launcher).into(thumbnailView);
                LogUtil.log_i(TAG, "ArticleListRecyclerViewAdapter thumbnail url: " + mCursor.getString(ArticleLoader.Query.THUMB_URL));
                //Glide.with(thumbnailView.getContext()).load(mCursor.getString(ArticleLoader.Query.THUMB_URL)).into(thumbnailView);
                thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

                thumbnailView.setTransitionName(String.valueOf(position));

                thumbnailView.setTag(String.valueOf(position));

                //itemView.setTag(ItemsContract.Items.buildItemUri(mCursor.getLong(ArticleLoader.Query._ID)));
                itemView.setTag(position);

                mPosition = position;
            }

            @Override
            public void onClick(View v) {
                mIsReentering = false;
                LogUtil.log_i("startActivity(Intent, Bundle)", false);
                //Uri uriTag = (Uri) itemView.getTag();
                int position = (int) itemView.getTag();
                //long clickedArticleId = ItemsContract.Items.getItemId(uriTag);
                long clickedArticleId = position;
                LogUtil.log_i(TAG, "clicked article id: " + clickedArticleId);
                Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class);
                intent.putExtra(EXTRA_CURRENT_ITEM_POSITION, position);
                LogUtil.log_i(TAG, "extra current item position: " + position); // extra current item position: 0
                intent.putExtra(ArticleDetailActivity.KEY_CLICKED_IMAGE_ID, clickedArticleId);
                LogUtil.log_i(TAG, "extra clicked image id: " + position); //extra clicked image id: 2036
                LogUtil.log_i(TAG, "onClick thumbnail transition name: " + thumbnailView.getTransitionName());// onClick thumbnail transition name: first
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(
                        ArticleListActivity.this, thumbnailView, thumbnailView.getTransitionName()).toBundle());
            }
        }
    }

}
