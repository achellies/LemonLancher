package com.limemobile.app.launcher.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.limemobile.app.launcher.util.IconCache;
import com.limemobile.app.launcher.wp8.R;

import java.util.ArrayList;
import java.util.List;

public class HTCSenseLockScreenView extends FrameLayout {

	private Context mContext;

	private ImageView mSenseUnlock;
	private ImageView mSensePanel;
	private ImageView mSenseLockCircle;

	private OnTriggeredListener mOnTriggeredListener;

	private BitmapDrawable mRingDrawable;
	private BitmapDrawable mRingAppDrawable;
	private BitmapDrawable mRingAppOnDrawable;
	private BitmapDrawable mRingUnlockDrawable;
	private BitmapDrawable mSenseAppBgDrawable;

	private float mDensity;
	private boolean mDragRing = false;
	private boolean mDragShortcut = false;
	private boolean mShortcutTriggered = false;
	private boolean mUnlockTriggered = false;
	private boolean mCalculateRect = false;
	private boolean mAnimationPlaying = false;
	private float mShortcutIconWidth;
	private float mShortcutIconHeight;
	private float mShortcutBasedHeight;
	private float mUnlockTriggeredHeight;
	private float mIconSpacing = 72.0f;
	
//	private Animation mUpAnim;
//	private Animation mDownAnim;
	private Animation mZoomEnterAnim;

	private Rect mRingBound = new Rect(0, 0, 0, 0);
	private ShortcutInfo mDraggingShortcut = null;

	private ArrayList<ShortcutInfo> mShortcuts = new ArrayList<ShortcutInfo>();

	public static class ShortcutInfo {
		Rect bound;
		BitmapDrawable drawable;
		Intent intent;
	}

	public HTCSenseLockScreenView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HTCSenseLockScreenView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);

		mContext = context;
	}

	@Override
	protected void onFinishInflate() {
		mDensity = mContext.getResources().getDisplayMetrics().density;
		setBackgroundColor(Color.argb(230, 68, 68, 68));

		mSenseUnlock = (ImageView) findViewById(R.id.sense_unlock);
		mSenseUnlock.setVisibility(View.GONE);
		
		mSensePanel = (ImageView) findViewById(R.id.sense_panel);
		
		mSenseLockCircle = (ImageView) findViewById(R.id.sense_lock_circle);
		mSenseLockCircle.setVisibility(View.INVISIBLE);

		loadBitmapDrawable();
		resetRect();
		
        mZoomEnterAnim = AnimationUtils.loadAnimation(mContext,
                R.anim.zoom_enter);
        mZoomEnterAnim.setFillAfter(false);

//        mUpAnim = new TranslateAnimation(
//                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
//                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
//                Animation.RELATIVE_TO_SELF, -0.5f);
//        mUpAnim.setDuration(400);
//        mUpAnim.setFillAfter(false);
//        mUpAnim.setRepeatCount(-1);
//        mUpAnim.setRepeatMode(Animation.REVERSE);
//        mUpAnim.setInterpolator(new AccelerateInterpolator());
//        
//        mDownAnim = new TranslateAnimation(
//                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
//                        0.0f, Animation.RELATIVE_TO_SELF, -0.5f,
//                        Animation.RELATIVE_TO_SELF, 0.0f);
//        mDownAnim.setDuration(800);
//        mDownAnim.setFillAfter(false);
//        mDownAnim.setInterpolator(new DecelerateInterpolator());
		super.onFinishInflate();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		resetRect();
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
	}

	@Override
	protected void onDetachedFromWindow() {
		unloadBitmapDrawable();
		super.onDetachedFromWindow();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		boolean shouldInvalidate = false;
		final int action = event.getAction();
		final int x = (int) event.getX();
		final int y = (int) event.getY();
		final int width = getWidth();
		final int height = getHeight();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (!mShortcutTriggered && !mUnlockTriggered) {
				mDragShortcut = false;
				mDragRing = false;
				mDraggingShortcut = null;
				mShortcutTriggered = false;
				mUnlockTriggered = false;
				resetRect();

				if (mRingBound.contains(x, y)) {	                
					mDragRing = true;
					replaceRing(x, y);
					shouldInvalidate = true;
				} else {
					for (ShortcutInfo info : mShortcuts) {
						if (info.bound.contains(x, y)) {
							mDraggingShortcut = info;
							mDragShortcut = true;
							shouldInvalidate = true;
							startDragShortcut(mDraggingShortcut);
							replaceShortcut(mDraggingShortcut, x, y);
							break;
						}
					}
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mDragRing) {
				shouldInvalidate = true;
				replaceRing(x, y);
			}
			if (mDragShortcut && mDraggingShortcut != null) {
				shouldInvalidate = true;
				replaceShortcut(mDraggingShortcut, x, y);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mDragShortcut && mDraggingShortcut != null
					&& mRingBound.contains(x, y)) {
				mShortcutTriggered = true;
				shouldInvalidate = true;

				mDragShortcut = false;
				mDragRing = false;

				LayoutParams params = (LayoutParams) mSenseUnlock
						.getLayoutParams();
				params.leftMargin = getLeft() + mRingBound.left;
				params.topMargin = getTop() + mRingBound.top;
				params.rightMargin = width - mRingBound.right;
				params.bottomMargin = height - mRingBound.bottom;

				triggered(mDraggingShortcut.intent);
			} else if (mDragRing && y < mUnlockTriggeredHeight) {
				mUnlockTriggered = true;
				shouldInvalidate = true;

				mDragShortcut = false;
				mDragRing = false;

				LayoutParams params = (LayoutParams) mSenseUnlock
						.getLayoutParams();
				params.leftMargin = getLeft() + mRingBound.left;
				params.topMargin = getTop() + mRingBound.top;
				params.rightMargin = width - mRingBound.right;
				params.bottomMargin = height - mRingBound.bottom;

				triggered(null);
			}
			mDragShortcut = false;
			mDragRing = false;
			mDraggingShortcut = null;
			resetRect();
			shouldInvalidate = true;
			break;
		case MotionEvent.ACTION_CANCEL:
			mDragShortcut = false;
			mDragRing = false;
			mShortcutTriggered = false;
			mUnlockTriggered = false;
			mDraggingShortcut = null;
			resetRect();
			shouldInvalidate = true;
			break;
		}

		if (shouldInvalidate)
			invalidate();

		return true;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

		if (!mCalculateRect) {
			mCalculateRect = true;
			resetRect();
			
	        final int width = getWidth();
	        final int height = getHeight();
            
            LayoutParams params = (LayoutParams) mSenseLockCircle.getLayoutParams();
            Rect rect = new Rect(mRingBound);
            
            params.width = rect.width();
            params.height = rect.height();
            
            params.leftMargin = getLeft() + rect.left;
            params.topMargin = getTop() + rect.top;
            params.rightMargin = width - rect.right;
            params.bottomMargin = height - rect.bottom;
            mSenseLockCircle.setImageDrawable(mRingDrawable);
		}

		if (!mRingBound.isEmpty() && !mShortcutTriggered && !mUnlockTriggered && !mAnimationPlaying) {
			BitmapDrawable ringDrawable = null;
			if (!mDragRing && !mDragShortcut) {
				ringDrawable = mRingDrawable;
			} else if (mDragRing) {
				if (mRingBound.bottom < mUnlockTriggeredHeight)
					ringDrawable = mRingUnlockDrawable;
				else
					ringDrawable = mRingDrawable;
			} else if (mDragShortcut) {
				ringDrawable = mRingAppDrawable;

				for (ShortcutInfo info : mShortcuts) {
					if (info != null && mRingBound.contains(info.bound)) {
						ringDrawable = mRingAppOnDrawable;
						break;
					}
				}
			}

            if (ringDrawable != null) {
				ringDrawable.setBounds(mRingBound);
				ringDrawable.draw(canvas);
				
                if (mDragShortcut && mDraggingShortcut != null) {
                    RectF bound = new RectF();
                    
                    int width = mDraggingShortcut.bound.width();
                    int height = mDraggingShortcut.bound.height();
                    
                    bound.left = mRingBound.centerX() - width / 2;
                    bound.top = mRingBound.centerY() - height / 2;
                    bound.right = bound.left + width;
                    bound.bottom = bound.top + height;
                    
                    Rect src = new Rect();
                    src.left = 0;
                    src.top = 0;
                    src.right = (int) mShortcutIconWidth;
                    src.bottom = (int) mShortcutIconHeight;
                    
                    canvas.drawBitmap(mDraggingShortcut.drawable.getBitmap(), src, bound, null);
                }
			}
		}
		
        if (!mDragRing) {
            for (ShortcutInfo info : mShortcuts) {
                if (info != null && info.drawable != null) {
                    Rect rect = new Rect(info.bound);
                    //rect.inset(-5, -5);
                    //mSenseAppBgDrawable.setBounds(rect);
                    //mSenseAppBgDrawable.draw(canvas);
                    rect.bottom += rect.height() / 2;
                    info.drawable.setBounds(rect);
                    info.drawable.draw(canvas);
                }
            }
        }
	}

	private void triggered(final Intent intent) {
		if (mOnTriggeredListener != null) {
			mSenseUnlock.setVisibility(View.VISIBLE);
			mSenseUnlock.clearAnimation();
			mZoomEnterAnim.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					mSenseUnlock.setVisibility(View.GONE);
					if (mShortcutTriggered)
						mOnTriggeredListener.OnShortcutTriggered(intent);
					if (mUnlockTriggered)
						mOnTriggeredListener.OnUnLockTriggered();

					mShortcutTriggered = false;
					mUnlockTriggered = false;
					invalidate();
				}
			});
			mSenseUnlock.startAnimation(mZoomEnterAnim);
		}
	}

	private void startDragShortcut(ShortcutInfo info) {
		int width = mRingBound.width();
		int height = mRingBound.height();

		final int screenWidth = getWidth();
		final int screenHeight = getHeight();
		final int halfWidth = screenWidth / 2;

		mRingBound.left = halfWidth - width / 2;
		mRingBound.right = mRingBound.left + width;
		mRingBound.top = screenHeight - height * 3 / 4;
		mRingBound.bottom = mRingBound.top + height;
		
		int dx = info.bound.centerX() - mRingBound.centerX();
		int dy = info.bound.centerY() - mRingBound.centerY();
		
		mRingBound.offset(dx / 2, dy / 6);
	}

	private void replaceRing(int x, int y) {
		int width = mRingBound.width();
		int height = mRingBound.height();

		mRingBound.left = x - width / 2;
		mRingBound.right = mRingBound.left + width;
		mRingBound.top = y - height / 2;
		mRingBound.bottom = mRingBound.top + height;
	}

	private void replaceShortcut(ShortcutInfo info, int x, int y) {
		if (info != null) {
			int width = info.bound.width();
			int height = info.bound.height();

			info.bound.left = x - width / 2;
			info.bound.right = info.bound.left + width;
			info.bound.top = y - height / 2;
			info.bound.bottom = info.bound.top + height;
		}
	}

	private void resetRect() {
		final int width = getWidth();
		final int height = getHeight();
		final int halfWidth = width / 2;

		mShortcutBasedHeight = height
				- mRingAppDrawable.getBitmap().getHeight() / 2.0f
				- mSensePanel.getHeight();
		mUnlockTriggeredHeight = mShortcutBasedHeight - mShortcutIconHeight;
		mIconSpacing = mShortcutIconWidth * 2 / 4;

		mRingBound.left = (int) (halfWidth - mRingAppDrawable.getBitmap().getWidth() / 2);
		mRingBound.top = (int) (height - mRingAppDrawable.getBitmap().getHeight() / 2);
		mRingBound.right = (int) (mRingBound.left + mRingAppDrawable.getBitmap().getWidth());
		mRingBound.bottom = (int) (mRingBound.top + mRingAppDrawable.getBitmap().getHeight());

		int length = mShortcuts.size();
		assert (length % 2 == 0);

		for (int index = 0; index < length / 2; ++index) {
			ShortcutInfo info = mShortcuts.get(length / 2 - index - 1);
			info.bound.right = (int) (halfWidth - (index + 1) * mIconSpacing - index
					* mShortcutIconWidth);
			info.bound.left = (int) (info.bound.right - mShortcutIconWidth);
			info.bound.bottom = (int) mShortcutBasedHeight;
			info.bound.top = (int) (info.bound.bottom - mShortcutIconHeight);

			ShortcutInfo info2 = mShortcuts.get(length / 2 + index);
			info2.bound.left = (int) (halfWidth + (index + 1) * mIconSpacing + index
					* mShortcutIconWidth);
			info2.bound.right = (int) (info2.bound.left + mShortcutIconWidth);
			info2.bound.bottom = (int) mShortcutBasedHeight;
			info2.bound.top = (int) (info2.bound.bottom - mShortcutIconHeight);
		}
	}

	private void loadBitmapDrawable() {
		Resources res = mContext.getResources();

		mRingDrawable = new BitmapDrawable(
				res.openRawResource(R.drawable.sense_ring));
		mRingAppDrawable = new BitmapDrawable(
				res.openRawResource(R.drawable.sense_ring_appready));
		mRingAppOnDrawable = new BitmapDrawable(
				res.openRawResource(R.drawable.sense_ring_appready_appon));
		mRingUnlockDrawable = new BitmapDrawable(
				res.openRawResource(R.drawable.sense_ring_on_unlock));

		mSenseAppBgDrawable = new BitmapDrawable(
				res.openRawResource(R.drawable.iphone_blank));
		
		int alpha = 45;
		ShortcutInfo info = new ShortcutInfo();
		info.bound = new Rect();
		info.bound.setEmpty();
		Bitmap bitmap = BitmapFactory.decodeStream(res.openRawResource(R.drawable.iphone_phone));
		info.drawable = new BitmapDrawable(IconCache.createReflection(bitmap, 0,
		        bitmap.getHeight() - bitmap.getHeight() / 2, 0,
		        bitmap.getHeight() / 2, alpha));
		info.intent = new Intent(Intent.ACTION_DIAL, null);
		mShortcutIconWidth = bitmap.getWidth();
		mShortcutIconHeight = bitmap.getHeight();
		bitmap.recycle();
		mShortcuts.add(info);

		info = new ShortcutInfo();
		info.bound = new Rect();
		info.bound.setEmpty();
		info.intent = new Intent(Intent.ACTION_VIEW, null);
		info.intent.setType("vnd.android-dir/mms-sms");
		
        List<ResolveInfo> infos = mContext.getPackageManager().queryIntentActivities(info.intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (infos != null && infos.size() > 0) {
            for (int index = 0; index < infos.size(); ++index) {
                ResolveInfo info2 = infos.get(index);
                if ((info2.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == android.content.pm.ApplicationInfo.FLAG_SYSTEM) {
                    info.intent = new Intent(Intent.ACTION_MAIN);
                    info.intent.setPackage(info2.activityInfo.applicationInfo.packageName);
                    info.intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    break;
                }
            }
        }
        bitmap = BitmapFactory.decodeStream(res.openRawResource(R.drawable.iphone_imessage));
        info.drawable = new BitmapDrawable(IconCache.createReflection(bitmap, 0,
                bitmap.getHeight() - bitmap.getHeight() / 2, 0,
                bitmap.getHeight() / 2, alpha));
        bitmap.recycle();
		mShortcuts.add(info);

		info = new ShortcutInfo();
		info.bound = new Rect();
		info.bound.setEmpty();
		info.intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        bitmap = BitmapFactory.decodeStream(res.openRawResource(R.drawable.iphone_camera));
        info.drawable = new BitmapDrawable(IconCache.createReflection(bitmap, 0,
                bitmap.getHeight() - bitmap.getHeight() / 2, 0,
                bitmap.getHeight() / 2, alpha));
        bitmap.recycle();
		mShortcuts.add(info);

		info = new ShortcutInfo();
		info.bound = new Rect();
		info.bound.setEmpty();
		info.intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse(getContext().getString(R.string.default_browser_url)));
        bitmap = BitmapFactory.decodeStream(res.openRawResource(R.drawable.iphone_safari));
        info.drawable = new BitmapDrawable(IconCache.createReflection(bitmap, 0,
                bitmap.getHeight() - bitmap.getHeight() / 2, 0,
                bitmap.getHeight() / 2, alpha));
        bitmap.recycle();
		mShortcuts.add(info);
	}

	private void unloadBitmapDrawable() {
	    if (mSenseLockCircle != null)
	        mSenseLockCircle.setImageDrawable(null);
		if (mRingDrawable != null) {
			mRingDrawable.setCallback(null);
			if (mRingDrawable.getBitmap() != null
					&& !mRingDrawable.getBitmap().isRecycled())
				mRingDrawable.getBitmap().recycle();
		}

		if (mRingAppDrawable != null) {
			mRingAppDrawable.setCallback(null);
			if (mRingAppDrawable.getBitmap() != null
					&& !mRingAppDrawable.getBitmap().isRecycled())
				mRingAppDrawable.getBitmap().recycle();
		}

		if (mRingAppOnDrawable != null) {
			mRingAppOnDrawable.setCallback(null);
			if (mRingAppOnDrawable.getBitmap() != null
					&& !mRingAppOnDrawable.getBitmap().isRecycled())
				mRingAppOnDrawable.getBitmap().recycle();
		}

		if (mRingUnlockDrawable != null) {
			mRingUnlockDrawable.setCallback(null);
			if (mRingUnlockDrawable.getBitmap() != null
					&& !mRingUnlockDrawable.getBitmap().isRecycled())
				mRingUnlockDrawable.getBitmap().recycle();
		}

		if (mSenseAppBgDrawable != null) {
			mSenseAppBgDrawable.setCallback(null);
			if (mSenseAppBgDrawable.getBitmap() != null
					&& !mSenseAppBgDrawable.getBitmap().isRecycled())
				mSenseAppBgDrawable.getBitmap().recycle();
		}

		for (ShortcutInfo info : mShortcuts) {
			if (info != null && info.drawable != null) {
				info.drawable.setCallback(null);
				if (info.drawable.getBitmap() != null
						&& !info.drawable.getBitmap().isRecycled())
					info.drawable.getBitmap().recycle();
			}
		}
		mShortcuts.clear();
	}

	public OnTriggeredListener getOnTriggeredListener() {
		return mOnTriggeredListener;
	}

	public void setOnTriggeredListener(OnTriggeredListener l) {
		this.mOnTriggeredListener = l;
	}
	
//	public void setLayoutAnim_slidedown(ViewGroup panel, Context ctx) {
//
//        AnimationSet set = new AnimationSet(true);
//
//        Animation animation = new TranslateAnimation(
//                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
//                0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
//                Animation.RELATIVE_TO_SELF, 0.0f);
//        animation.setDuration(800);
//        animation.setAnimationListener(new AnimationListener() {
//
//            @Override
//            public void onAnimationStart(Animation animation) {
//                // TODO Auto-generated method stub
//                // MapContacts.this.mapviewgroup.setVisibility(View.VISIBLE);
//
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//                // TODO Auto-generated method stub
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//
//                // TODO Auto-generated method stub
//
//            }
//        });
//        set.addAnimation(animation);
//
//        LayoutAnimationController controller = new LayoutAnimationController(
//                set, 0.25f);
//        panel.setLayoutAnimation(controller);
//
//    }
//
//    public void setLayoutAnim_slideup(ViewGroup panel, Context ctx) {
//
//        AnimationSet set = new AnimationSet(true);
//
//        /*
//         * Animation animation = new AlphaAnimation(1.0f, 0.0f);
//         * animation.setDuration(200); set.addAnimation(animation);
//         */
//
//        Animation animation = new TranslateAnimation(
//                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
//                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
//                Animation.RELATIVE_TO_SELF, -1.0f);
//        animation.setDuration(800);
//        animation.setAnimationListener(new AnimationListener() {
//
//            @Override
//            public void onAnimationStart(Animation animation) {
//                // TODO Auto-generated method stub
//
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//                // TODO Auto-generated method stub
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//                // MapContacts.this.mapviewgroup.setVisibility(View.INVISIBLE);
//                // TODO Auto-generated method stub
//
//            }
//        });
//        set.addAnimation(animation);
//
//        LayoutAnimationController controller = new LayoutAnimationController(
//                set, 0.25f);
//        panel.setLayoutAnimation(controller);
//
//    }

	public interface OnTriggeredListener {
		void OnUnLockTriggered();

		void OnShortcutTriggered(Intent intent);
	}
}
