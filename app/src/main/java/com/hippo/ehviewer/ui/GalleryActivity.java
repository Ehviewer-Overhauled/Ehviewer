/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.assist.AssistContent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.hippo.app.BaseDialogBuilder;
import com.hippo.app.EditTextDialogBuilder;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.ArchiveGalleryProvider;
import com.hippo.ehviewer.gallery.DirGalleryProvider;
import com.hippo.ehviewer.gallery.EhGalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProvider2;
import com.hippo.ehviewer.widget.GalleryGuideView;
import com.hippo.ehviewer.widget.GalleryHeader;
import com.hippo.ehviewer.widget.ReversibleSeekBar;
import com.hippo.glgallery.GalleryProvider;
import com.hippo.glgallery.GalleryView;
import com.hippo.glgallery.SimpleAdapter;
import com.hippo.glview.view.GLRootView;
import com.hippo.unifile.UniFile;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.IoThreadPoolExecutor;
import com.hippo.widget.ColorView;
import com.hippo.yorozuya.AnimationUtils;
import com.hippo.yorozuya.ConcurrentPool;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.ResourcesUtils;
import com.hippo.yorozuya.SimpleAnimatorListener;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.ViewUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import rikka.core.res.ConfigurationKt;
import rikka.core.res.ResourcesKt;

public class GalleryActivity extends EhActivity implements SeekBar.OnSeekBarChangeListener,
        GalleryView.Listener {

    public static final String ACTION_DIR = "dir";
    public static final String ACTION_EH = "eh";

    public static final String KEY_ACTION = "action";
    public static final String KEY_FILENAME = "filename";
    public static final String KEY_URI = "uri";
    public static final String KEY_GALLERY_INFO = "gallery_info";
    public static final String KEY_PAGE = "page";
    public static final String KEY_CURRENT_INDEX = "current_index";

    private static final long SLIDER_ANIMATION_DURING = 150;
    private static final long HIDE_SLIDER_DELAY = 3000;

    private final ConcurrentPool<NotifyTask> mNotifyTaskPool = new ConcurrentPool<>(3);
    private String mAction;
    private String mFilename;
    private Uri mUri;
    private GalleryInfo mGalleryInfo;
    private int mPage;
    private String mCacheFileName;
    ActivityResultLauncher<String> saveImageToLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(),
            uri -> {
                if (uri != null) {
                    String filepath = AppConfig.getExternalTempDir() + File.separator + mCacheFileName;
                    File cachefile = new File(filepath);

                    ContentResolver resolver = getContentResolver();

                    IoThreadPoolExecutor.getInstance().execute(() -> {
                        InputStream is = null;
                        OutputStream os = null;
                        try {
                            is = new FileInputStream(cachefile);
                            os = resolver.openOutputStream(uri);
                            IOUtils.copy(is, os);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            IOUtils.closeQuietly(is);
                            IOUtils.closeQuietly(os);
                            runOnUiThread(() -> Toast.makeText(GalleryActivity.this, getString(R.string.image_saved, uri.getPath()), Toast.LENGTH_SHORT).show());
                        }
                        //noinspection ResultOfMethodCallIgnored
                        cachefile.delete();
                    });
                }


            });
    @Nullable
    private GLRootView mGLRootView;
    @Nullable
    private GalleryView mGalleryView;
    @Nullable
    private GalleryProvider2 mGalleryProvider;
    @Nullable
    private GalleryAdapter mGalleryAdapter;
    @Nullable
    private WindowInsetsControllerCompat insetsController;
    @Nullable
    private ColorView mMaskView;
    @Nullable
    private View mClock;
    @Nullable
    private TextView mProgress;
    @Nullable
    private View mBattery;
    @Nullable
    private View mSeekBarPanel;
    private final ValueAnimator.AnimatorUpdateListener mUpdateSliderListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (null != mSeekBarPanel) {
                mSeekBarPanel.requestLayout();
            }
        }
    };
    @Nullable
    private TextView mLeftText;
    @Nullable
    private TextView mRightText;
    @Nullable
    private ReversibleSeekBar mSeekBar;
    private ObjectAnimator mSeekBarPanelAnimator;
    private final SimpleAnimatorListener mShowSliderListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mSeekBarPanelAnimator = null;
        }
    };
    private final SimpleAnimatorListener mHideSliderListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mSeekBarPanelAnimator = null;
            if (mSeekBarPanel != null) {
                mSeekBarPanel.setVisibility(View.INVISIBLE);
            }
        }
    };
    private final Runnable mHideSliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSeekBarPanel != null) {
                hideSlider(mSeekBarPanel);
            }
        }
    };
    private int mLayoutMode;
    private int mSize;
    private int mCurrentIndex;
    private int mSavingPage = -1;
    private EditTextDialogBuilder builder;
    private boolean dialogShown = false;
    private AlertDialog dialog;

    private void buildProvider() {
        if (mGalleryProvider != null) {
            return;
        }

        if (ACTION_DIR.equals(mAction)) {
            if (mFilename != null) {
                mGalleryProvider = new DirGalleryProvider(UniFile.fromFile(new File(mFilename)));
            }
        } else if (ACTION_EH.equals(mAction)) {
            if (mGalleryInfo != null) {
                mGalleryProvider = new EhGalleryProvider(this, mGalleryInfo);
            }
        } else if (Intent.ACTION_VIEW.equals(mAction)) {
            if (mUri != null) {
                try {
                    grantUriPermission(BuildConfig.APPLICATION_ID, mUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception e) {
                    Toast.makeText(this, R.string.error_reading_failed, Toast.LENGTH_SHORT).show();
                }
                mGalleryProvider = new ArchiveGalleryProvider(this, mUri);
            }
        }
    }

    private void onInit() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        mAction = intent.getAction();
        mFilename = intent.getStringExtra(KEY_FILENAME);
        mUri = intent.getData();
        mGalleryInfo = intent.getParcelableExtra(KEY_GALLERY_INFO);
        mPage = intent.getIntExtra(KEY_PAGE, -1);
        buildProvider();
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mAction = savedInstanceState.getString(KEY_ACTION);
        mFilename = savedInstanceState.getString(KEY_FILENAME);
        mUri = savedInstanceState.getParcelable(KEY_URI);
        mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO);
        mPage = savedInstanceState.getInt(KEY_PAGE, -1);
        mCurrentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX);
        buildProvider();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTION, mAction);
        outState.putString(KEY_FILENAME, mFilename);
        outState.putParcelable(KEY_URI, mUri);
        if (mGalleryInfo != null) {
            outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo);
        }
        outState.putInt(KEY_PAGE, mPage);
        outState.putInt(KEY_CURRENT_INDEX, mCurrentIndex);
    }

    @Override
    protected void attachBaseContext(@NonNull Context newBase) {
        switch (Settings.getReadTheme()) {
            case 1:
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2:
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
        super.attachBaseContext(newBase);
    }

    @Override
    @SuppressWarnings({"WrongConstant"})
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Settings.getReadingFullscreen()) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }

        builder = new EditTextDialogBuilder(this, null, getString(R.string.archive_passwd));
        builder.setTitle(getString(R.string.archive_need_passwd));
        builder.setPositiveButton(getString(android.R.string.ok), null);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        if (mGalleryProvider == null) {
            finish();
            return;
        }

        ArchiveGalleryProvider.showPasswd = new ShowPasswdDialogHandler(this);

        mGalleryProvider.start();

        // Get start page
        int startPage;
        if (savedInstanceState == null) {
            startPage = mPage >= 0 ? mPage : mGalleryProvider.getStartPage();
        } else {
            startPage = mCurrentIndex;
        }

        setContentView(R.layout.activity_gallery);
        mGLRootView = (GLRootView) ViewUtils.$$(this, R.id.gl_root_view);
        mGalleryAdapter = new GalleryAdapter(mGLRootView, mGalleryProvider);
        Resources resources = getResources();
        mGalleryView = new GalleryView.Builder(this, mGalleryAdapter)
                .setListener(this)
                .setLayoutMode(Settings.getReadingDirection())
                .setScaleMode(Settings.getPageScaling())
                .setStartPosition(Settings.getStartPosition())
                .setStartPage(startPage)
                .setBackgroundColor(ResourcesKt.resolveColor(getTheme(), android.R.attr.colorBackground))
                .setPagerInterval(Settings.getShowPageInterval() ? resources.getDimensionPixelOffset(R.dimen.gallery_pager_interval) : 0)
                .setScrollInterval(Settings.getShowPageInterval() ? resources.getDimensionPixelOffset(R.dimen.gallery_scroll_interval) : 0)
                .setPageMinHeight(resources.getDimensionPixelOffset(R.dimen.gallery_page_min_height))
                .setPageInfoInterval(resources.getDimensionPixelOffset(R.dimen.gallery_page_info_interval))
                .setProgressColor(ResourcesUtils.getAttrColor(this, androidx.appcompat.R.attr.colorPrimary))
                .setProgressSize(resources.getDimensionPixelOffset(R.dimen.gallery_progress_size))
                .setPageTextColor(ResourcesKt.resolveColor(getTheme(), android.R.attr.textColorSecondary))
                .setPageTextSize(resources.getDimensionPixelOffset(R.dimen.gallery_page_text_size))
                .setPageTextTypeface(Typeface.DEFAULT)
                .setErrorTextColor(resources.getColor(R.color.red_500))
                .setErrorTextSize(resources.getDimensionPixelOffset(R.dimen.gallery_error_text_size))
                .setDefaultErrorString(resources.getString(R.string.error_unknown))
                .setEmptyString(resources.getString(R.string.error_empty))
                .build();

        mGLRootView.setContentPane(mGalleryView);
        mGalleryProvider.setListener(mGalleryAdapter);
        mGalleryProvider.setGLRoot(mGLRootView);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            if (Settings.getReadingFullscreen()) {
                insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                // This does not hide navigation bar sometimes on 31, framework bug???
                insetsController.hide(WindowInsetsCompat.Type.systemBars());
            } else {
                insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH);
                insetsController.show(WindowInsetsCompat.Type.systemBars());
            }
            boolean night = ConfigurationKt.isNight(getResources().getConfiguration());
            insetsController.setAppearanceLightStatusBars(!night);
            insetsController.setAppearanceLightNavigationBars(!night);
        }

        mMaskView = (ColorView) ViewUtils.$$(this, R.id.mask);
        mMaskView.setOnGenericMotionListener((view, event) -> {
            if (mGalleryView == null) {
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_SCROLL) {
                float scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL) * 300;
                if (scroll < 0.0f) {
                    if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                        mGalleryView.pageLeft();
                    } else if (mLayoutMode == GalleryView.LAYOUT_LEFT_TO_RIGHT) {
                        mGalleryView.pageRight();
                    } else if (mLayoutMode == GalleryView.LAYOUT_TOP_TO_BOTTOM) {
                        mGalleryView.onScroll(0, -scroll, 0, -scroll, 0, -scroll);
                    }
                } else {
                    if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                        mGalleryView.pageRight();
                    } else if (mLayoutMode == GalleryView.LAYOUT_LEFT_TO_RIGHT) {
                        mGalleryView.pageLeft();
                    } else if (mLayoutMode == GalleryView.LAYOUT_TOP_TO_BOTTOM) {
                        mGalleryView.onScroll(0, -scroll, 0, -scroll, 0, -scroll);
                    }
                }
            }
            return false;
        });
        mClock = ViewUtils.$$(this, R.id.clock);
        mProgress = (TextView) ViewUtils.$$(this, R.id.progress);
        mBattery = ViewUtils.$$(this, R.id.battery);
        mClock.setVisibility(Settings.getShowClock() ? View.VISIBLE : View.GONE);
        mProgress.setVisibility(Settings.getShowProgress() ? View.VISIBLE : View.GONE);
        mBattery.setVisibility(Settings.getShowBattery() ? View.VISIBLE : View.GONE);

        mSeekBarPanel = ViewUtils.$$(this, R.id.seek_bar_panel);
        mLeftText = (TextView) ViewUtils.$$(mSeekBarPanel, R.id.left);
        mRightText = (TextView) ViewUtils.$$(mSeekBarPanel, R.id.right);
        mSeekBar = (ReversibleSeekBar) ViewUtils.$$(mSeekBarPanel, R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);

        mSize = mGalleryProvider.size();
        mCurrentIndex = startPage;
        if (mGalleryView != null) {
            mLayoutMode = mGalleryView.getLayoutMode();
        }
        updateSlider();

        // Update keep screen on
        if (Settings.getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Orientation
        int orientation;
        switch (Settings.getScreenRotation()) {
            default:
            case 0:
                orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                break;
            case 1:
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                break;
            case 2:
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                break;
            case 3:
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                break;
        }
        setRequestedOrientation(orientation);

        // Screen lightness
        setScreenLightness(Settings.getCustomScreenLightness(), Settings.getScreenLightness());

        // Cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        GalleryHeader galleryHeader = findViewById(R.id.gallery_header);
        ViewCompat.setOnApplyWindowInsetsListener(galleryHeader, (v, insets) -> {
            if (!Settings.getReadingFullscreen()) {
                galleryHeader.setTopInsets(insets.getInsets(WindowInsetsCompat.Type.statusBars()).top);
            } else {
                galleryHeader.setDisplayCutout(insets.getDisplayCutout());
            }
            return WindowInsetsCompat.CONSUMED;
        });

        if (Settings.getGuideGallery()) {
            FrameLayout mainLayout = (FrameLayout) ViewUtils.$$(this, R.id.main);
            mainLayout.addView(new GalleryGuideView(this));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLRootView = null;
        mGalleryView = null;
        if (mGalleryAdapter != null) {
            mGalleryAdapter.clearUploader();
            mGalleryAdapter = null;
        }
        if (mGalleryProvider != null) {
            mGalleryProvider.setListener(null);
            mGalleryProvider.stop();
            mGalleryProvider = null;
        }

        mMaskView = null;
        mClock = null;
        mProgress = null;
        mBattery = null;
        mSeekBarPanel = null;
        mLeftText = null;
        mRightText = null;
        mSeekBar = null;

        SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGLRootView != null) {
            mGLRootView.onPause();
        }
    }    ActivityResultLauncher<String> requestStoragePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (result && mSavingPage != -1) {
                    saveImage(mSavingPage);
                } else {
                    Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
                }
                mSavingPage = -1;
            });

    @Override
    protected void onResume() {
        super.onResume();

        if (mGLRootView != null) {
            mGLRootView.onResume();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mGalleryView == null) {
            return super.onKeyDown(keyCode, event);
        }

        // Check volume
        if (Settings.getVolumePage()) {
            if (Settings.getReverseVolumePage()) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    keyCode = KeyEvent.KEYCODE_VOLUME_DOWN;
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    keyCode = KeyEvent.KEYCODE_VOLUME_UP;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                    mGalleryView.pageRight();
                } else {
                    mGalleryView.pageLeft();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                    mGalleryView.pageLeft();
                } else {
                    mGalleryView.pageRight();
                }
                return true;
            }
        }

        // Check keyboard and Dpad
        switch (keyCode) {
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                    mGalleryView.pageRight();
                } else {
                    mGalleryView.pageLeft();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mGalleryView.pageLeft();
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                    mGalleryView.pageLeft();
                } else {
                    mGalleryView.pageRight();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mGalleryView.pageRight();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MENU:
                onTapMenuArea();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Check volume
        if (Settings.getVolumePage()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                    keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true;
            }
        }

        // Check keyboard and Dpad
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_SPACE ||
                keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @SuppressLint("SetTextI18n")
    private void updateProgress() {
        if (mProgress == null) {
            return;
        }
        if (mSize <= 0 || mCurrentIndex < 0) {
            mProgress.setText(null);
        } else {
            mProgress.setText((mCurrentIndex + 1) + "/" + mSize);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateSlider() {
        if (mSeekBar == null || mRightText == null || mLeftText == null || mSize <= 0 || mCurrentIndex < 0) {
            return;
        }

        TextView start;
        TextView end;
        if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
            start = mRightText;
            end = mLeftText;
            mSeekBar.setReverse(true);
        } else {
            start = mLeftText;
            end = mRightText;
            mSeekBar.setReverse(false);
        }
        start.setText(Integer.toString(mCurrentIndex + 1));
        end.setText(Integer.toString(mSize));
        mSeekBar.setMax(mSize - 1);
        mSeekBar.setProgress(mCurrentIndex);
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView start;
        if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
            start = mRightText;
        } else {
            start = mLeftText;
        }
        if (fromUser && null != start) {
            start.setText(Integer.toString(progress + 1));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        SimpleHandler.getInstance().postDelayed(mHideSliderRunnable, HIDE_SLIDER_DELAY);
        int progress = seekBar.getProgress();
        if (progress != mCurrentIndex && null != mGalleryView) {
            mGalleryView.setCurrentPage(progress);
        }
    }

    @Override
    public void onUpdateCurrentIndex(int index) {
        if (null != mGalleryProvider) {
            mGalleryProvider.putStartPage(index);
        }

        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_CURRENT_INDEX, index);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapSliderArea() {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_SLIDER_AREA, 0);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapMenuArea() {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_MENU_AREA, 0);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapErrorText(int index) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_ERROR_TEXT, index);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onLongPressPage(int index) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_LONG_PRESS_PAGE, index);
        SimpleHandler.getInstance().post(task);
    }

    private void showSlider(View sliderPanel) {
        if (null != mSeekBarPanelAnimator) {
            mSeekBarPanelAnimator.cancel();
            mSeekBarPanelAnimator = null;
        }

        sliderPanel.setTranslationY(sliderPanel.getHeight());
        sliderPanel.setVisibility(View.VISIBLE);

        mSeekBarPanelAnimator = ObjectAnimator.ofFloat(sliderPanel, "translationY", 0.0f);
        mSeekBarPanelAnimator.setDuration(SLIDER_ANIMATION_DURING);
        mSeekBarPanelAnimator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
        mSeekBarPanelAnimator.addUpdateListener(mUpdateSliderListener);
        mSeekBarPanelAnimator.addListener(mShowSliderListener);
        mSeekBarPanelAnimator.start();

        if (insetsController != null) {
            if (Settings.getReadingFullscreen())
                insetsController.show(WindowInsetsCompat.Type.systemBars());
        }
    }

    private void hideSlider(View sliderPanel) {
        if (null != mSeekBarPanelAnimator) {
            mSeekBarPanelAnimator.cancel();
            mSeekBarPanelAnimator = null;
        }

        mSeekBarPanelAnimator = ObjectAnimator.ofFloat(sliderPanel, "translationY", sliderPanel.getHeight());
        mSeekBarPanelAnimator.setDuration(SLIDER_ANIMATION_DURING);
        mSeekBarPanelAnimator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
        mSeekBarPanelAnimator.addUpdateListener(mUpdateSliderListener);
        mSeekBarPanelAnimator.addListener(mHideSliderListener);
        mSeekBarPanelAnimator.start();

        if (insetsController != null) {
            if (Settings.getReadingFullscreen())
                insetsController.hide(WindowInsetsCompat.Type.systemBars());
        }
    }

    /**
     * @param lightness 0 - 200
     */
    private void setScreenLightness(boolean enable, int lightness) {
        if (null == mMaskView) {
            return;
        }

        Window w = getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        if (enable) {
            lightness = MathUtils.clamp(lightness, 0, 200);
            if (lightness > 100) {
                mMaskView.setColor(0);
                // Avoid BRIGHTNESS_OVERRIDE_OFF,
                // screen may be off when lp.screenBrightness is 0.0f
                lp.screenBrightness = Math.max((lightness - 100) / 100.0f, 0.01f);
            } else {
                mMaskView.setColor(MathUtils.lerp(0xde, 0x00, lightness / 100.0f) << 24);
                lp.screenBrightness = 0.01f;
            }
        } else {
            mMaskView.setColor(0);
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        }
        w.setAttributes(lp);
    }

    private void shareImage(int page) {
        if (null == mGalleryProvider) {
            return;
        }

        File dir = AppConfig.getExternalTempDir();
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show();
            return;
        }
        UniFile file;
        if (null == (file = mGalleryProvider.save(page, UniFile.fromFile(dir), mGalleryProvider.getImageFilename(page)))) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        String filename = file.getName();
        if (filename == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(filename));
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "image/jpeg";
        }

        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", new File(dir, filename));

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        if (mGalleryInfo != null)
            intent.putExtra(Intent.EXTRA_TEXT, EhUrl.getGalleryDetailUrl(mGalleryInfo.gid, mGalleryInfo.token));
        intent.setDataAndType(uri, mimeType);

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_image)));
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            Toast.makeText(this, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyImage(int page) {
        if (null == mGalleryProvider) {
            return;
        }

        File dir = AppConfig.getExternalCopyTempDir();
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show();
            return;
        }
        UniFile file;
        if (null == (file = mGalleryProvider.save(page, UniFile.fromFile(dir), mGalleryProvider.getImageFilename(page)))) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        String filename = file.getName();
        if (filename == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", new File(dir, filename));

        var clipboardManager = getSystemService(ClipboardManager.class);
        if (clipboardManager != null) {
            var clipData = ClipData.newUri(getContentResolver(), "ehviewer", uri);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage(int page) {
        if (null == mGalleryProvider) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            mSavingPage = page;
            requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }

        String filename = mGalleryProvider.getImageFilenameWithExtension(page);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(filename));
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "image/jpeg";
        }

        String realPath;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + AppConfig.APP_DIRNAME);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            realPath = Environment.DIRECTORY_PICTURES + File.separator + AppConfig.APP_DIRNAME;
        } else {
            File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), AppConfig.APP_DIRNAME);
            realPath = path.toString();
            if (!FileUtils.ensureDirectory(path)) {
                Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
                return;
            }
            values.put(MediaStore.MediaColumns.DATA, path + File.separator + filename);
        }
        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mGalleryProvider.save(page, UniFile.fromMediaUri(this, imageUri))) {
            try {
                resolver.delete(imageUri, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(imageUri, contentValues, null, null);
        }

        Toast.makeText(this, getString(R.string.image_saved, realPath + File.separator + filename), Toast.LENGTH_SHORT).show();
    }

    private void saveImageTo(int page) {
        if (null == mGalleryProvider) {
            return;
        }
        File dir = AppConfig.getExternalTempDir();
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show();
            return;
        }
        UniFile file;
        if (null == (file = mGalleryProvider.save(page, UniFile.fromFile(dir), mGalleryProvider.getImageFilename(page)))) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        String filename = file.getName();
        if (filename == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        mCacheFileName = filename;
        try {
            saveImageToLauncher.launch(filename);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            Toast.makeText(this, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show();
        }
    }

    private void showPageDialog(final int page) {
        Resources resources = GalleryActivity.this.getResources();
        BaseDialogBuilder builder = new BaseDialogBuilder(GalleryActivity.this);
        builder.setTitle(resources.getString(R.string.page_menu_title, page + 1));

        final CharSequence[] items;
        items = new CharSequence[]{
                getString(R.string.page_menu_refresh),
                getString(R.string.page_menu_share),
                getString(android.R.string.copy),
                getString(R.string.page_menu_save),
                getString(R.string.page_menu_save_to)};
        pageDialogListener(builder, items, page);
        builder.show();
    }

    private void pageDialogListener(AlertDialog.Builder builder, CharSequence[] items, int page) {
        builder.setItems(items, (dialog, which) -> {
            if (mGalleryProvider == null) {
                return;
            }

            switch (which) {
                case 0: // Refresh
                    mGalleryProvider.removeCache(page);
                    mGalleryProvider.forceRequest(page);
                    break;
                case 1: // Share
                    shareImage(page);
                    break;
                case 2: // Copy
                    copyImage(page);
                    break;
                case 3: // Save
                    saveImage(page);
                    break;
                case 4: // Save to
                    saveImageTo(page);
                    break;
            }
        });
    }

    @Nullable
    private String getGalleryDetailUrl() {
        long gid;
        String token;
        if (mGalleryInfo != null) {
            gid = mGalleryInfo.gid;
            token = mGalleryInfo.token;
        } else {
            return null;
        }
        return EhUrl.getGalleryDetailUrl(gid, token, 0, false);
    }

    @Override
    public void onProvideAssistContent(AssistContent outContent) {
        super.onProvideAssistContent(outContent);

        String url = getGalleryDetailUrl();
        if (url != null) {
            outContent.setWebUri(Uri.parse(url));
        }
    }

    private void showPasswdDialog() {
        if (!dialogShown) {
            dialogShown = true;
            dialog.show();
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    GalleryActivity.this.onProvidePasswd();
                });
            }
            dialog.setOnCancelListener(v -> finish());
        }
    }

    private void onProvidePasswd() {
        String passwd = builder.getText();
        if (passwd.isEmpty())
            builder.setError(getString(R.string.passwd_cannot_be_empty));
        else {
            ArchiveGalleryProvider.passwd = passwd;
            ArchiveGalleryProvider.pv.v();
        }
    }

    private void onPasswdWrong() {
        builder.setError(getString(R.string.passwd_wrong));
    }

    private void onPasswdCorrect() {
        dialog.dismiss();
    }

    private static class ShowPasswdDialogHandler extends Handler {

        //弱引用持有HandlerActivity , GC 回收时会被回收掉
        private final WeakReference<GalleryActivity> weakReference;

        public ShowPasswdDialogHandler(GalleryActivity activity) {
            this.weakReference = new WeakReference(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            GalleryActivity activity = weakReference.get();
            super.handleMessage(msg);
            if (null != activity) {
                switch (msg.what) {
                    case 0:
                        activity.showPasswdDialog();
                        break;
                    case 1:
                        activity.onPasswdWrong();
                        break;
                    case 2:
                        activity.onPasswdCorrect();
                        break;
                }
            }
        }
    }

    private class GalleryMenuHelper implements DialogInterface.OnClickListener {

        private final View mView;
        private final Spinner mScreenRotation;
        private final Spinner mReadingDirection;
        private final Spinner mScaleMode;
        private final Spinner mStartPosition;
        private final Spinner mReadTheme;
        private final MaterialSwitch mKeepScreenOn;
        private final MaterialSwitch mShowClock;
        private final MaterialSwitch mShowProgress;
        private final MaterialSwitch mShowBattery;
        private final MaterialSwitch mShowPageInterval;
        private final MaterialSwitch mVolumePage;
        private final MaterialSwitch mReverseVolumePage;
        private final MaterialSwitch mReadingFullscreen;
        private final MaterialSwitch mCustomScreenLightness;
        private final SeekBar mScreenLightness;

        @SuppressLint("InflateParams")
        public GalleryMenuHelper(Context context) {
            mView = LayoutInflater.from(context).inflate(R.layout.dialog_gallery_menu, null);
            mScreenRotation = mView.findViewById(R.id.screen_rotation);
            mReadingDirection = mView.findViewById(R.id.reading_direction);
            mScaleMode = mView.findViewById(R.id.page_scaling);
            mStartPosition = mView.findViewById(R.id.start_position);
            mReadTheme = mView.findViewById(R.id.read_theme);
            mKeepScreenOn = mView.findViewById(R.id.keep_screen_on);
            mShowClock = mView.findViewById(R.id.show_clock);
            mShowProgress = mView.findViewById(R.id.show_progress);
            mShowBattery = mView.findViewById(R.id.show_battery);
            mShowPageInterval = mView.findViewById(R.id.show_page_interval);
            mVolumePage = mView.findViewById(R.id.volume_page);
            mReverseVolumePage = mView.findViewById(R.id.reverse_volume_page);
            mReadingFullscreen = mView.findViewById(R.id.reading_fullscreen);
            mCustomScreenLightness = mView.findViewById(R.id.custom_screen_lightness);
            mScreenLightness = mView.findViewById(R.id.screen_lightness);

            mScreenRotation.setSelection(Settings.getScreenRotation());
            mReadingDirection.setSelection(Settings.getReadingDirection());
            mScaleMode.setSelection(Settings.getPageScaling());
            mStartPosition.setSelection(Settings.getStartPosition());
            mReadTheme.setSelection(Settings.getReadTheme());
            mKeepScreenOn.setChecked(Settings.getKeepScreenOn());
            mShowClock.setChecked(Settings.getShowClock());
            mShowProgress.setChecked(Settings.getShowProgress());
            mShowBattery.setChecked(Settings.getShowBattery());
            mShowPageInterval.setChecked(Settings.getShowPageInterval());
            mVolumePage.setChecked(Settings.getVolumePage());
            mVolumePage.setOnCheckedChangeListener((buttonView, isChecked) -> mReverseVolumePage.setEnabled(isChecked));
            mReverseVolumePage.setEnabled(mVolumePage.isChecked());
            mReverseVolumePage.setChecked(Settings.getReverseVolumePage());
            mReadingFullscreen.setChecked(Settings.getReadingFullscreen());
            mCustomScreenLightness.setChecked(Settings.getCustomScreenLightness());
            mScreenLightness.setProgress(Settings.getScreenLightness());
            mScreenLightness.setEnabled(Settings.getCustomScreenLightness());

            mCustomScreenLightness.setOnCheckedChangeListener((buttonView, isChecked) -> mScreenLightness.setEnabled(isChecked));
        }

        public View getView() {
            return mView;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mGalleryView == null) {
                return;
            }

            int screenRotation = mScreenRotation.getSelectedItemPosition();
            int layoutMode = GalleryView.sanitizeLayoutMode(mReadingDirection.getSelectedItemPosition());
            int scaleMode = GalleryView.sanitizeScaleMode(mScaleMode.getSelectedItemPosition());
            int startPosition = GalleryView.sanitizeStartPosition(mStartPosition.getSelectedItemPosition());
            int readTheme = mReadTheme.getSelectedItemPosition();
            boolean keepScreenOn = mKeepScreenOn.isChecked();
            boolean showClock = mShowClock.isChecked();
            boolean showProgress = mShowProgress.isChecked();
            boolean showBattery = mShowBattery.isChecked();
            boolean showPageInterval = mShowPageInterval.isChecked();
            boolean volumePage = mVolumePage.isChecked();
            boolean reverseVolumePage = mReverseVolumePage.isChecked();
            boolean readingFullscreen = mReadingFullscreen.isChecked();
            boolean customScreenLightness = mCustomScreenLightness.isChecked();
            int screenLightness = mScreenLightness.getProgress();

            boolean oldReadingFullscreen = Settings.getReadingFullscreen();
            int oldReadTheme = Settings.getReadTheme();

            Settings.putScreenRotation(screenRotation);
            Settings.putReadingDirection(layoutMode);
            Settings.putPageScaling(scaleMode);
            Settings.putStartPosition(startPosition);
            Settings.putReadTheme(readTheme);
            Settings.putKeepScreenOn(keepScreenOn);
            Settings.putShowClock(showClock);
            Settings.putShowProgress(showProgress);
            Settings.putShowBattery(showBattery);
            Settings.putShowPageInterval(showPageInterval);
            Settings.putVolumePage(volumePage);
            Settings.putReverseVolumePage(reverseVolumePage);
            Settings.putReadingFullscreen(readingFullscreen);
            Settings.putCustomScreenLightness(customScreenLightness);
            Settings.putScreenLightness(screenLightness);

            int orientation;
            switch (screenRotation) {
                default:
                case 0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                    break;
                case 1:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                    break;
                case 2:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                    break;
                case 3:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                    break;
            }
            setRequestedOrientation(orientation);
            mGalleryView.setLayoutMode(layoutMode);
            mGalleryView.setScaleMode(scaleMode);
            mGalleryView.setStartPosition(startPosition);
            if (keepScreenOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            if (mClock != null) {
                mClock.setVisibility(showClock ? View.VISIBLE : View.GONE);
            }
            if (mProgress != null) {
                mProgress.setVisibility(showProgress ? View.VISIBLE : View.GONE);
            }
            if (mBattery != null) {
                mBattery.setVisibility(showBattery ? View.VISIBLE : View.GONE);
            }
            mGalleryView.setPagerInterval(showPageInterval ? getResources().getDimensionPixelOffset(R.dimen.gallery_pager_interval) : 0);
            mGalleryView.setScrollInterval(showPageInterval ? getResources().getDimensionPixelOffset(R.dimen.gallery_scroll_interval) : 0);
            setScreenLightness(customScreenLightness, screenLightness);

            // Update slider
            mLayoutMode = layoutMode;
            updateSlider();

            if (oldReadingFullscreen != readingFullscreen || oldReadTheme != readTheme) {
                recreate();
            }
        }
    }

    private class NotifyTask implements Runnable {

        public static final int KEY_LAYOUT_MODE = 0;
        public static final int KEY_SIZE = 1;
        public static final int KEY_CURRENT_INDEX = 2;
        public static final int KEY_TAP_SLIDER_AREA = 3;
        public static final int KEY_TAP_MENU_AREA = 4;
        public static final int KEY_TAP_ERROR_TEXT = 5;
        public static final int KEY_LONG_PRESS_PAGE = 6;

        private int mKey;
        private int mValue;

        public void setData(int key, int value) {
            mKey = key;
            mValue = value;
        }

        private void onTapMenuArea() {
            BaseDialogBuilder builder = new BaseDialogBuilder(GalleryActivity.this);
            GalleryMenuHelper helper = new GalleryMenuHelper(builder.getContext());
            builder.setTitle(R.string.gallery_menu_title)
                    .setView(helper.getView())
                    .setPositiveButton(android.R.string.ok, helper).show();
        }

        private void onTapSliderArea() {
            if (mSeekBarPanel == null || mSize <= 0 || mCurrentIndex < 0) {
                return;
            }

            SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);

            if (mSeekBarPanel.getVisibility() == View.VISIBLE) {
                hideSlider(mSeekBarPanel);
            } else {
                showSlider(mSeekBarPanel);
                SimpleHandler.getInstance().postDelayed(mHideSliderRunnable, HIDE_SLIDER_DELAY);
            }
        }

        private void onTapErrorText(int index) {
            if (mGalleryProvider != null) {
                mGalleryProvider.forceRequest(index);
            }
        }

        private void onLongPressPage(final int index) {
            showPageDialog(index);
        }

        @Override
        public void run() {
            switch (mKey) {
                case KEY_LAYOUT_MODE:
                    GalleryActivity.this.mLayoutMode = mValue;
                    updateSlider();
                    break;
                case KEY_SIZE:
                    GalleryActivity.this.mSize = mValue;
                    updateSlider();
                    updateProgress();
                    break;
                case KEY_CURRENT_INDEX:
                    GalleryActivity.this.mCurrentIndex = mValue;
                    updateSlider();
                    updateProgress();
                    break;
                case KEY_TAP_MENU_AREA:
                    onTapMenuArea();
                    break;
                case KEY_TAP_SLIDER_AREA:
                    onTapSliderArea();
                    break;
                case KEY_TAP_ERROR_TEXT:
                    onTapErrorText(mValue);
                    break;
                case KEY_LONG_PRESS_PAGE:
                    onLongPressPage(mValue);
                    break;
            }
            mNotifyTaskPool.push(this);
        }
    }

    private class GalleryAdapter extends SimpleAdapter {

        public GalleryAdapter(@NonNull GLRootView glRootView, @NonNull GalleryProvider provider) {
            super(glRootView, provider);
        }

        @Override
        public void onDataChanged() {
            super.onDataChanged();

            if (mGalleryProvider != null) {
                int size = mGalleryProvider.size();
                NotifyTask task = mNotifyTaskPool.pop();
                if (task == null) {
                    task = new NotifyTask();
                }
                task.setData(NotifyTask.KEY_SIZE, size);
                SimpleHandler.getInstance().post(task);
            }
        }
    }




}
