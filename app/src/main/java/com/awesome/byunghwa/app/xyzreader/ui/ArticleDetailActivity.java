package com.awesome.byunghwa.app.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.awesome.byunghwa.app.xyzreader.R;
import com.awesome.byunghwa.app.xyzreader.adapter.TransitionAdapter;
import com.awesome.byunghwa.app.xyzreader.data.ArticleLoader;
import com.awesome.byunghwa.app.xyzreader.util.CircularReveal;
import com.awesome.byunghwa.app.xyzreader.util.LogUtil;
import com.awesome.byunghwa.app.xyzreader.util.Utils;

import java.util.List;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener, ViewPager.OnPageChangeListener  {

    private static final String TAG = "ArticleDetailActivity";

    public static final String KEY_CLICKED_IMAGE_ID = "com.awesome.byunghwa.app.xyzreader.CLICKEDIMAGEID";

    private Cursor mCursor;

    private long mSelectedItemId;

    private MyPagerAdapter mPagerAdapter;

    private static AppBarLayout appBarLayout;

    public static ArticleDetailActivity activity;

    private static final boolean DEBUG = true;

    private static final String STATE_CURRENT_POSITION = "state_current_position";
    private static final String STATE_OLD_POSITION = "state_old_position";

    private int mCurrentPosition;
    private int mOriginalPosition;
    private boolean mIsReturning;

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            LogUtil.log_i("onMapSharedElements(List<String>, Map<String, View>)", mIsReturning);
            if (mIsReturning) {
                View sharedView = mPagerAdapter.getCurrentDetailsFragment().getSharedElement();
                if (sharedView == null) {
                    // If shared view is null, then it has likely been scrolled off screen and
                    // recycled. In this case we cancel the shared element transition by
                    // removing the shared elements from the shared elements map.
                    names.clear();
                    sharedElements.clear();
                } else if (mCurrentPosition != mOriginalPosition) {
                    names.clear();
                    sharedElements.clear();
                    names.add(sharedView.getTransitionName());
                    sharedElements.put(sharedView.getTransitionName(), sharedView);
                }
            }

            LogUtil.log_i("=== names: " + names.toString(), mIsReturning);
            LogUtil.log_i("=== sharedElements: " + Utils.setToString(sharedElements.keySet()), mIsReturning);
        }

        @Override
        public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements,
                                         List<View> sharedElementSnapshots) {
            LogUtil.log_i("onSharedElementStart(List<String>, List<View>, List<View>)", mIsReturning);
            if (!mIsReturning) {
                getWindow().setEnterTransition(makeEnterTransition(getSharedElement(sharedElements)));
                LogUtil.log_i(TAG, "shared element count: " + sharedElements.size());
            }
        }

        @Override
        public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements,
                                       List<View> sharedElementSnapshots) {
            LogUtil.log_i("onSharedElementEnd(List<String>, List<View>, List<View>)", mIsReturning);
            if (mIsReturning) {
                getWindow().setReturnTransition(makeReturnTransition());
            }
        }

        private View getSharedElement(List<View> sharedElements) {
            for (final View view : sharedElements) {
                if (view instanceof ImageView) {
                    return view;
                }
            }
            return null;
        }
    };
    private ViewPager mPager;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Transition makeEnterTransition(View sharedElement) {
        View rootView = mPagerAdapter.getCurrentDetailsFragment().getView();
        assert rootView != null;

        TransitionSet enterTransition = new TransitionSet();

        // Play a circular reveal animation starting beneath the shared element.
        Transition circularReveal = new CircularReveal(sharedElement); // issue: sharedElement is null
        circularReveal.addTarget(rootView.findViewById(R.id.reveal_container));
        enterTransition.addTransition(circularReveal);

        // Slide the cards in through the bottom of the screen.
        Transition cardSlide = new Slide(Gravity.BOTTOM);
        cardSlide.addTarget(rootView.findViewById(R.id.text_container));
        enterTransition.addTransition(cardSlide);

        // Don't fade the navigation/status bars.
        Transition fade = new Fade();
        fade.excludeTarget(android.R.id.navigationBarBackground, true);
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        enterTransition.addTransition(fade);

        final Resources res = getResources();
        final ImageView backgroundImage = (ImageView) rootView.findViewById(R.id.background_image);
        backgroundImage.setAlpha(0f);
        enterTransition.addListener(new TransitionAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                backgroundImage.animate().alpha(1f).setDuration(res.getInteger(R.integer.image_background_fade_millis));
            }
        });

        enterTransition.setDuration(getResources().getInteger(R.integer.transition_duration_millis));
        return enterTransition;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Transition makeReturnTransition() {
        View rootView = mPagerAdapter.getCurrentDetailsFragment().getView();
        assert rootView != null;

        TransitionSet returnTransition = new TransitionSet();

        // Slide and fade the circular reveal container off the top of the screen.
        TransitionSet slideFade = new TransitionSet();
        slideFade.addTarget(rootView.findViewById(R.id.reveal_container));
        slideFade.addTransition(new Slide(Gravity.TOP));
        slideFade.addTransition(new Fade());
        returnTransition.addTransition(slideFade);

        // Slide the cards off the bottom of the screen.
        Transition cardSlide = new Slide(Gravity.BOTTOM);
        cardSlide.addTarget(rootView.findViewById(R.id.text_container));
        returnTransition.addTransition(cardSlide);

        returnTransition.setDuration(getResources().getInteger(R.integer.transition_duration_millis));
        return returnTransition;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        postponeEnterTransition();
        setEnterSharedElementCallback(mCallback);

        //getWindow().getSharedElementEnterTransition().setDuration(getResources().getInteger(R.integer.transition_duration_millis));

        if (savedInstanceState == null) {
            LogUtil.log_i(TAG, "getIntent() is null?" + (getIntent() == null));

            if (getIntent() != null && getIntent().hasExtra(KEY_CLICKED_IMAGE_ID) && getIntent().hasExtra(ArticleListActivity.EXTRA_CURRENT_ITEM_POSITION)) {
                mCurrentPosition = getIntent().getIntExtra(ArticleListActivity.EXTRA_CURRENT_ITEM_POSITION, 0);
                mOriginalPosition = mCurrentPosition;
                LogUtil.log_i(TAG, "current item position: " + mCurrentPosition);
                long mStartId = getIntent().getLongExtra(KEY_CLICKED_IMAGE_ID, 0);
                LogUtil.log_i(TAG, "clicked item id: " + mStartId);// 2025
                mSelectedItemId = mStartId;
                LogUtil.log_i(TAG, "selected item id: " + mSelectedItemId);
            }
        } else {
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_POSITION);
            mOriginalPosition = savedInstanceState.getInt(STATE_OLD_POSITION);
        }

        activity = this;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.share_fab);
        fab.setOnClickListener(this);

        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(this);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        LogUtil.log_i(TAG, "mCurrentPosition: " + mCurrentPosition);

        LogUtil.log_i(TAG, "savedInstance is null?" + (savedInstanceState == null));

        getWindow().getSharedElementEnterTransition().setDuration(getResources().getInteger(R.integer.transition_duration_millis));

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_POSITION, mCurrentPosition);
        outState.putInt(STATE_OLD_POSITION, mOriginalPosition);
    }

    @Override
    public void finishAfterTransition() {
        LogUtil.log_i("finishAfterTransition()", true);
        mIsReturning = true;
        getWindow().setReturnTransition(makeReturnTransition());
        Intent data = new Intent();
        data.putExtra(ArticleListActivity.EXTRA_OLD_ITEM_POSITION, getIntent().getExtras().getInt(ArticleListActivity.EXTRA_CURRENT_ITEM_POSITION));
        data.putExtra(ArticleListActivity.EXTRA_CURRENT_ITEM_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();
        LogUtil.log_i(TAG, "cursor item count: " + mCursor.getCount());

        mPager.setCurrentItem(mCurrentPosition); // we have to set current page here instead of in onCreate
        // since after we get the cursor, we will pass it on to adapter to display data
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    // FAB share button click event
    @Override
    public void onClick(View v) {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText("Some sample text")
                .getIntent(), getString(R.string.action_share)));

        // FloatingActionButton behavior will pay attention to Snack Bar views and move to leave them enough space to paint. The only requirements are that the FAB is inside a Coordinator Layout and that we attach the SnackBar to it
        // The content view in this case is the CoordinatorLayout, so when the FAB is clicked, the SnackBar appears and the FAB animates with it.
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        Snackbar.make(coordinatorLayout, "FAB Clicked", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        LogUtil.log_i(TAG, "mPager onPageSelected gets called!");
        mCurrentPosition = position;
        if (mCursor != null) {
            mCursor.moveToPosition(position);
        }
        LogUtil.log_i(TAG, "Title: " + mCursor.getString(ArticleLoader.Query.TITLE));
        mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    // ViewPager Setup
    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        private ArticleDetailFragment mCurrentFragment;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentFragment = (ArticleDetailFragment) object;
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID), position);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }

        public ArticleDetailFragment getCurrentDetailsFragment() {
            return mCurrentFragment;
        }
    }

    /**
     * It seems that the ActionBar view is reused between activities. Changes need to be reverted,
     * or the ActionBar will be transparent when we go back to Main Activity
     */
    private void restablishActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getReturnTransition().addListener(new TransitionAdapter() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    if (appBarLayout != null) {
                        appBarLayout.getBackground().setAlpha(255);
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        restablishActionBar();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            restablishActionBar();
        }

        return super.onOptionsItemSelected(item);
    }
}
