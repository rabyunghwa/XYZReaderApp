package com.awesome.byunghwa.app.xyzreader.ui;


import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.awesome.byunghwa.app.xyzreader.R;
import com.awesome.byunghwa.app.xyzreader.data.ArticleLoader;
import com.awesome.byunghwa.app.xyzreader.util.LogUtil;
import com.awesome.byunghwa.app.xyzreader.util.Utils;
import com.bumptech.glide.Glide;

/**
 * A simple {@link Fragment} subclass.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final String ARG_SELECTED_IMAGE_POSITION = "arg_selected_image_position";

    private Cursor mCursor;
    private long mItemId;
    private int mItemPosition;
    private TextView titleView;
    private TextView bylineView;
    private TextView bodyView;
    private ImageView headerImage;
    private ImageView backgroundImage;
    //private Toolbar toolbar;

    //private View bgViewGroup;
    private View mRootView;


    public ArticleDetailFragment() {
        // Required empty public constructor
    }

    public static ArticleDetailFragment newInstance(long itemId, int position) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(ARG_SELECTED_IMAGE_POSITION, position);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
            mItemPosition = getArguments().getInt(ARG_SELECTED_IMAGE_POSITION);
        }
        LogUtil.log_i(TAG, "Clicked Item Id: " + mItemId);
        LogUtil.log_i(TAG, "Clicked Item Position" + mItemPosition);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        LogUtil.log_i(TAG, "OnActivityCreated Gets called");
        getLoaderManager().initLoader(0, null, this);

        //initScrollFade(headerImage);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        LogUtil.log_i(TAG, "OnCreateView gets called");
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        View revealContainer = mRootView.findViewById(R.id.reveal_container);
        if (mRootView == null) {
            return null;
        }
        titleView = (TextView) mRootView.findViewById(R.id.article_title);
        bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
        headerImage = (ImageView) revealContainer.findViewById(R.id.header_image);

        headerImage.setTransitionName(ArticleListActivity.transitionNames[mItemPosition]);

        LogUtil.log_i(TAG, "header image transition name: " + ArticleListActivity.transitionNames[mItemPosition]);

        backgroundImage = (ImageView) revealContainer.findViewById(R.id.background_image);

        //bgViewGroup = mRootView.findViewById(R.id.bg_viewgroup);

        //toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar_detail);

        //LogUtil.log_i(TAG, "ImageView is null: " + (image == null));

        /*mRootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onPreDraw() {
                mRootView.getViewTreeObserver().removeOnPreDrawListener(this);
                getActivity().startPostponedEnterTransition();
                return true;
            }
        });*/

        //ViewCompat.setTransitionName(headerImage, ArticleListActivity.transitionNames[mItemPosition]);

        return mRootView;
    }

    /*@Override
    public void onResume() {
        super.onResume();
        LogUtil.log_i("info", "onResume gets called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bgViewGroup.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (bgViewGroup.isAttachedToWindow()) {
                        v.removeOnLayoutChangeListener(this);
                    }

                }
            });
        }
    }*/

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(ArticleDetailActivity.activity, mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    private void bindViews() {
        if (mCursor != null) {
            String title = mCursor.getString(ArticleLoader.Query.TITLE);
            LogUtil.log_i(TAG, "Article Title: " + title);
            titleView.setText(title);
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
            bylineView.setTextColor(getResources().getColor(R.color.text_primary));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            transitionSetup();
        }

    }

    private void transitionSetup() {
        // image loading
        if (mCursor != null) {
            //getActivity().startPostponedEnterTransition();
            String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            //String photoUrl = mCursor.getString(ArticleLoader.Query.THUMB_URL);
            LogUtil.log_i(TAG, "PhotoUrl: " + photoUrl);

            /*Glide.with(ArticleDetailActivity.activity)
                    .load(photoUrl)
                    .asBitmap()
                    .into(new BitmapImageViewTarget(headerImage) {
                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                            super.onResourceReady(bitmap, anim);
                            //getActivity().startPostponedEnterTransition();
                            scheduleStartPostponedTransition(headerImage);
                            Palette.Builder builder = new Palette.Builder(bitmap);
                            builder.generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    // Here's your generated palette
                                    applyPalette(palette, headerImage);
                                }
                            });
                        }
                    });*/


            /*Picasso.with(headerImage.getContext()).load(photoUrl).placeholder(R.mipmap.ic_launcher).into(new Target() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    // cache is now warmed up
                    getActivity().startPostponedEnterTransition();
                    headerImage.setImageBitmap(bitmap);
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                }
            });*/

            // header image setup
            Glide.with(headerImage.getContext()).load(photoUrl).into(headerImage);

            mRootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public boolean onPreDraw() {
                    mRootView.getViewTreeObserver().removeOnPreDrawListener(this);
                    getActivity().startPostponedEnterTransition();
                    return true;
                }
            });

            LogUtil.log_i(TAG, "ArticleDetailFragment photourl: " + photoUrl);
            //Picasso.with(headerImage.getContext()).load(photoUrl).into(headerImage);

            // background image setup
            Glide.with(ArticleDetailActivity.activity).load(Utils.RADIOHEAD_BACKGROUND_URLS[mItemPosition]).into(backgroundImage);
        }
    }

    /**
     * Schedules the shared element transition to be started immediately
     * after the shared element has been measured and laid out within the
     * activity's view hierarchy. Some common places where it might make
     * sense to call this method are:
     *
     * (1) Inside a Fragment's onCreateView() method (if the shared element
     *     lives inside a Fragment hosted by the called Activity).
     *
     * (2) Inside a Picasso Callback object (if you need to wait for Picasso to
     *     asynchronously load/scale a bitmap before the transition can begin).
     *
     * (3) Inside a LoaderCallback's onLoadFinished() method (if the shared
     *     element depends on data queried by a Loader).
     */
    /*private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        getActivity().startPostponedEnterTransition();
                        return true;
                    }
                });
    }*/

    /*private void applyPalette(Palette palette, ImageView image) {
        int primaryDark = MyApplication.instance.getResources().getColor(R.color.theme_primary_dark);
        int primary = MyApplication.instance.getResources().getColor(R.color.theme_primary);

        AppCompatActivity activity = (AppCompatActivity) getActivity();

        LogUtil.log_i(TAG, "Toolbar is null: " + (toolbar == null));

        if (getView() != null) {
            toolbar = (Toolbar) getView().findViewById(R.id.toolbar_detail);

            LogUtil.log_i(TAG, "Toolbar is null: " + (toolbar == null));

            activity.setSupportActionBar(toolbar);
            if (mCursor != null) {
                toolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
            }

            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            toolbar.setBackgroundColor(palette.getMutedColor(primary));
        }

        //ActivityCompat.startPostponedEnterTransition(activity);

        WindowCompatUtils.setStatusBarcolor(ArticleDetailActivity.activity.getWindow(), palette.getDarkMutedColor(primaryDark));
        initScrollFade(image);
        updateBackground((FloatingActionButton) ArticleDetailActivity.activity.findViewById(R.id.share_fab), palette);
    }

    private void updateBackground(FloatingActionButton fab, Palette palette) {
        int lightMutedColor = palette.getLightMutedColor(MyApplication.instance.getResources().getColor(android.R.color.white));
        int mutedColor = palette.getMutedColor(MyApplication.instance.getResources().getColor(R.color.theme_accent));

        fab.setRippleColor(lightMutedColor);
        fab.setBackgroundTintList(ColorStateList.valueOf(mutedColor));
    }

    private void initScrollFade(final ImageView image) {
        if (getView() != null) {
            final ScrollView scrollView = (ScrollView) getView().findViewById(R.id.scroll);

            LogUtil.log_i(TAG, "ScrollView is null: " + (scrollView == null));

            setComponentsStatus(scrollView, image);

            scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    setComponentsStatus(scrollView, image);
                }
            });
        }
    }

    private void setComponentsStatus(final ScrollView scrollView, final ImageView image) {
        final int scrollY = scrollView.getScrollY(); // error: for other pages, scrollY is always 0
        LogUtil.log_i(TAG, "ScrollY: " + scrollY); // the error might be due to scrollview. when you change a page, the ScrollView is till the previous page's
        image.setTranslationY(-scrollY / 2);

        LogUtil.log_i(TAG, "Toolbar is null: " + (toolbar == null));

        if (toolbar != null) {
            ColorDrawable background = (ColorDrawable) toolbar.getBackground();
            int padding = scrollView.getPaddingTop();
            double alpha = (1 - (((double) padding - (double) scrollY) / (double) padding)) * 255.0;
            alpha = alpha < 0 ? 0 : alpha;
            alpha = alpha > 255 ? 255 : alpha;

            if (background != null) {
                background.setAlpha((int) alpha);
            }

            float scrollRatio = (float) (alpha / 255f);
            int titleColor = getAlphaColor(Color.WHITE, scrollRatio);
            toolbar.setTitleTextColor(titleColor);
        }
    }

    private int getAlphaColor(int color, float scrollRatio) {
        return Color.argb((int) (scrollRatio * 255f), Color.red(color), Color.green(color), Color.blue(color));
    }*/

    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on screen.
     */
    @Nullable
    public View getSharedElement() {
        if (getView() != null) {
            View view = getView().findViewById(R.id.header_image);
            //LogUtil.log_i(TAG, "get sharedelement view is null?" + (view == null));
            if (Utils.isViewInBounds(getView().findViewById(R.id.scroll), view)) {
                return view;
            }
        }
        return null;
    }

}
