/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/**
 * 
 */
package com.mediatek.vlw;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import com.mediatek.xlog.Xlog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.mediatek.featureoption.FeatureOption;

public class VLWMediaController extends MediaController {
	static final String TAG = "VLWMediaController";
	static final boolean DEBUG = false;
	
	private static final int MAX_LEVEL = 10000;
	private static final int MAX_SEEK = 1000;
	private static final int FADE_OUT = 1;
	private static final int SHOW_PROGRESS = 2;
	private static final int FPS = 33;
	public final ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
	Callback[] mGottenCallbacks;
	boolean mHaveGottenCallbacks;

	private MediaPlayerControl mPlayer;
	private final Context mContext;
	private View mRoot;
	StringBuilder mFormatBuilder;
	Formatter mFormatter;
	private TextView mEndTime;
	private TextView mCurrentTime;
	private VLWSeekBar mEditorBarStart;
	private VLWSeekBar mEditorBarEnd;
	private VLWSeekBar mProgressBar;
	private boolean mDragging;
	private int mDuration;
	private int mNewPosition;
	private int mStartPosition;
	private int mEndPosition;
	//theme manager +
	private static final int  PROGRESSRASPBERRY = -1945992;
	private static final int  PROGRESSMINT = -16726640;
	private static final int  PROGRESSMOCHA = -4743069;

	//theme manager +

//    private HandlerThread mWorkerThread;
//    private Handler mWorker;
    private boolean mPosChange;
    private boolean mIsEditmode;
//    private Runnable mPendingRunnable;
//    private final Object mLock = new Object();
    private int mMinDuration = 1;
    
	/**
	 * @param context
	 */
	public VLWMediaController(Context context) {
		super(context);
		mContext = context;
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public VLWMediaController(Context context, AttributeSet attrs) {
		super(context, attrs);
		mRoot = this;
		mContext = context;
	}

	@Override
	public void onFinishInflate() {
		if (mRoot != null) {
			initControllerView(mRoot);
		}
	}

	/**
	 * (non-Javadoc)
	 * @param context
	 * @param useFastForward
	 */
	public VLWMediaController(Context context, boolean useFastForward) {
		super(context, useFastForward);
		mContext = context;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		int width = mEditorBarStart.getWidth();
		int paddingLeft = mEditorBarStart.getPaddingLeft();
		int paddingRight = mEditorBarStart.getPaddingRight();
		int available = width - paddingLeft - paddingRight;

		int loc[];
		loc = new int[2];
		mEditorBarStart.getLocationInWindow(loc);
		int startPos = loc[0] + paddingLeft + available
			* mEditorBarStart.getProgress() / mEditorBarStart.getMax();
		
		width = mEditorBarEnd.getWidth();
		paddingLeft = mEditorBarEnd.getPaddingLeft();
		paddingRight = mEditorBarEnd.getPaddingRight();
		available = width - paddingLeft - paddingRight;
		mEditorBarEnd.getLocationInWindow(loc);
		int endPos = loc[0] + paddingLeft + available
			* mEditorBarEnd.getProgress() / mEditorBarEnd.getMax();
		
		mProgressBar.getLocationInWindow(loc);
		int bottom = mProgressBar.getBottom();
		int posX = (int) ev.getX();
		int posY = (int) ev.getY();

		// dispatch events to start editor
		int startOffset = Math.abs(startPos - posX);
		int endOffset = Math.abs(endPos - posX);
		if ( (posY > bottom && startOffset < endOffset  // touch on it?
				&& !mEditorBarEnd.isPressed()) 			// come from the end bar?
				|| mEditorBarStart.isPressed()			// should consume the event if pressed until touch up on it
			) {
			if (mEditorBarEnd.isPressed()) {
				mEditorBarEnd.setPressed(false);
			}
			return mEditorBarStart.dispatchTouchEvent(ev);
		}
		
		if (mEditorBarStart.isPressed()) {
			mEditorBarStart.setPressed(false);
			// resolve issue that after leaving pressed state the SeekBar is 
			// in half pressed state
			mEditorBarStart.invalidate();
		}
		return super.dispatchTouchEvent(ev);
	}

	/*private void sendRunnableToWorker (Runnable r) {
        if (mWorkerThread == null) {
            mWorkerThread = new HandlerThread("WorkerThread");
            mWorkerThread.setPriority(Thread.MIN_PRIORITY);
            mWorkerThread.start();
        }
        if (mWorker == null) {
            mWorker = new Handler(mWorkerThread.getLooper());
        }
        mWorker.post(r);
    }
    
    private void sendRunnableToWorkerDelayed (Runnable r, long delayMillis) {
        if (mWorkerThread == null) {
            mWorkerThread = new HandlerThread("WorkerThread");
            mWorkerThread.start();
        }
        if (mWorker == null) {
            mWorker = new Handler(mWorkerThread.getLooper());
        }
        mWorker.postDelayed(r, delayMillis);
    }*/
    
    private int setLevelFromProgress(int progress) {
    	float scale = mProgressBar.getMax() > 0 ? (float) progress / (float) mProgressBar.getMax() : 0;
        final Drawable d = mProgressBar.getProgressDrawable();
        if (d != null) {
            Drawable progressDrawable = null;

            if (d instanceof LayerDrawable) {
                progressDrawable = ((LayerDrawable) d).findDrawableByLayerId(R.id.clip);
            }

            final int level = (int) (scale * MAX_LEVEL);
            if (progressDrawable != null) {
            	progressDrawable.setLevel(level);
            } else {
            	d.setLevel(level);
            }
            
            return level;
        }
        
        return 0;
    }
    
	/**
	 * (non-Javadoc)
	 * @return the mStartPosition
	 */
	public long getStartPosition() {
		return mStartPosition;
	}

	/**
	 * (non-Javadoc)
	 * @return the mEndPosition
	 */
	public long getEndPosition() {
		return mEndPosition;
	}
	
	/**
	 * play the video and update UI
	 */
	public void play() {
		if (mPlayer != null) {
			if (mIsEditmode) {
				mPlayer.seekTo(mNewPosition);
			}
			mPlayer.start();
			updateUI(true);
			mMyHandler.sendEmptyMessage(SHOW_PROGRESS);
			mIsEditmode = false;
			mDragging = false;
		}
	}

	/**
	 * pause the mediaplayer and update UI
	 */
	public void pause() {
		mMyHandler.removeMessages(SHOW_PROGRESS);

		if (mPlayer != null) {
			mPlayer.pause();
			updateUI(false);
		}
	}

	/**
	 * Surface of VideoView will recreated every time Visibility changed, it 
	 * will reload video and play it.
	 * But we want VideoEditor just to be paused and stay where it is
	 * This is a big ugly hack so:
	 * @hide
	 */
	public void traceBack(int curPos) {
		Xlog.d("VideoEditor", "traceBack(), curPos=" + curPos);
		mPlayer.seekTo(curPos);
		
		setProgress();
		
		int progress = (int) (MAX_SEEK * (double)mStartPosition / mDuration + 0.5);
		mEditorBarStart.setProgress(progress);
		mProgressBar.setLeftThreshold(progress);
		// need to update the clip drawable because above calling did not trigger
		// onProgressChanged() of the mEditorBarStart
		setLevelFromProgress(progress);
		
		progress = (int) (MAX_SEEK * (double)mEndPosition / mDuration + 0.5);
		mEditorBarEnd.setProgress(progress);
		mProgressBar.setRightThreshold(progress);
		progress = (int) (MAX_SEEK * (double)mStartPosition / mDuration + mMinDuration);
		mEditorBarEnd.setLeftThreshold(progress);
		progress = (int) (MAX_SEEK * (double)mEndPosition / mDuration - mMinDuration);
		mEditorBarStart.setRightThreshold(progress);
		
		updateFrame(curPos);
		updateState();
	}
	
	/**
	 * must called when start playing a new video when the player is prepared
	 * 
	 * @param start
	 * @param end
	 * @param duration
	 */
	public void initControllerState(int start, int end, int duration) {
		if (DEBUG) {
			Xlog.i(TAG, String.format("initControllerState(%d, %d, %d)", 
					start, end, duration));
		}
		
		mStartPosition = start;
		mEndPosition = end;
		mNewPosition = start;
		mDuration = duration;
		mPlayer.seekTo(mNewPosition);
		int step = duration / MAX_SEEK;
		if (step <= 0) {
			mMinDuration = MAX_SEEK / 2;
		} else if (step < MAX_SEEK){
			mMinDuration = (int)Math.ceil(MAX_SEEK / (float)(step - 1));
		} else {
			mMinDuration = 1;
		}

		int rt = (int) (MAX_SEEK * (double)mEndPosition / duration + 0.5);
		mEditorBarEnd.setProgress(rt);
		mProgressBar.setRightThreshold(rt);
		rt = (int) (MAX_SEEK * (double)mEndPosition / duration - mMinDuration);
		mEditorBarStart.setRightThreshold(rt);
		
		int lt = (int) (MAX_SEEK * (double)mStartPosition / duration + 0.5);
		mEditorBarStart.setProgress(lt);
		mProgressBar.setLeftThreshold(lt);
		mProgressBar.setProgress(lt);
		setLevelFromProgress(lt);
		lt = (int) (MAX_SEEK * (double)mStartPosition / duration + mMinDuration);
		mEditorBarEnd.setLeftThreshold(lt);
		
		if (mEndTime != null)
			mEndTime.setText(stringForTime(mEndPosition));
		if (mCurrentTime != null)
			mCurrentTime.setText(stringForTime(mStartPosition));
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController#setAnchorView(android.view.View)
	 */
	@Override
	public void setAnchorView(View view) {
		// TODO Auto-generated method stub
		super.setAnchorView(view);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.MediaController#setMediaPlayer(android.widget.MediaController
	 * .MediaPlayerControl)
	 */
	@Override
	public void setMediaPlayer(MediaPlayerControl player) {
		super.setMediaPlayer(player);
		mPlayer = player;
	}

	/* (non-Javadoc)
	 * @see android.widget.MediaController#hide()
	 */
	@Override
	public void hide() {
		// do nothing just override super.hide() to change behavior
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController#show()
	 */
	@Override
	public void show(int timeout) {
        // just override and do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.MediaController#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.MediaController#onTrackballEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
		return true;
	}

	/* (non-Javadoc)
	 * @see android.widget.MediaController#dispatchKeyEvent(android.view.KeyEvent)
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			return false;
		}
		
		return super.dispatchKeyEvent(event);
	}

	/**
	 * Create the view that holds the widgets that control playback. Derived
	 * classes can override this to create their own.
	 * 
	 * @return The controller view.
	 * @hide This doesn't work as advertised
	 */
	protected View makeControllerView() {
		if (DEBUG) {
			Xlog.i(TAG, "mContext = " + mContext);
		}
		LayoutInflater inflate = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mRoot = inflate.inflate(R.layout.media_controller, null);
		initControllerView(mRoot);

		return mRoot;
	}

	protected void initControllerView(View v) {
		mEditorBarStart = (VLWSeekBar) v.findViewById(R.id.editor_bar_start);
		if (mEditorBarStart != null) {
			mEditorBarStart.setOnSeekBarChangeListener(mStartListener);
			mEditorBarStart.setMax(1000);
			mEditorBarStart.setRightThreshold(MAX_SEEK - mMinDuration);
			mEditorBarStart.setAlpha(0);
			mEditorBarStart.setOnTouchUpWithoutHandledListener(new VLWSeekBar.OnTouchUpWithoutHandledListener() {				
				@Override
				public void onTouchUp(float progress) {
					updateFrame(mStartPosition);
					updateUI(false);
					updateState();
				}
			});
		}
		mEditorBarEnd = (VLWSeekBar) v.findViewById(R.id.editor_bar_end);
		if (mEditorBarEnd != null) {
			mEditorBarEnd.setOnSeekBarChangeListener(mEndListener);
			mEditorBarEnd.setMax(1000);
			mEditorBarEnd.setLeftThreshold(mMinDuration);
			mEditorBarEnd.setProgress(1000);
			mEditorBarEnd.setAlpha(0);
			mEditorBarEnd.setOnTouchUpWithoutHandledListener(new VLWSeekBar.OnTouchUpWithoutHandledListener() {				
				@Override
				public void onTouchUp(float progress) {
					updateFrame(mEndPosition);
					updateUI(false);
					updateState();
				}
			});
		}
		mProgressBar = (VLWSeekBar) v.findViewById(R.id.seek_bar);
		if (mProgressBar != null) {
			//Theme manager +
			if(FeatureOption.MTK_THEMEMANAGER_APP){
				Resources res = mContext.getResources();
				int textColor = res.getThemeMainColor();
				if(textColor!=0){
					switch (textColor) {
					case PROGRESSRASPBERRY:
						Drawable drawableRaspberry = mContext.getResources().getDrawable(R.drawable.progressraspberry);
						mProgressBar.setProgressDrawable(drawableRaspberry);
						break;
					case PROGRESSMINT:
						Drawable drawableMint = mContext.getResources().getDrawable(R.drawable.progressmint);
						mProgressBar.setProgressDrawable(drawableMint);
						break;
					case PROGRESSMOCHA:
						Drawable drawableMocha = mContext.getResources().getDrawable(R.drawable.progressmocha);
						mProgressBar.setProgressDrawable(drawableMocha);
						break;
					}			
				}
			}
			//Theme manager +
			mProgressBar.setOnSeekBarChangeListener(mProgressListener);
			mProgressBar.setMax(1000);
			mProgressBar.setThumb(null);
			mProgressBar.setOnTouchUpWithoutHandledListener(new VLWSeekBar.OnTouchUpWithoutHandledListener() {				
				@Override
				public void onTouchUp(float progress) {
					mDragging = false;
					updateFrame(mNewPosition);
					updateState();
				}
			});
		}
		mEndTime = (TextView) v.findViewById(R.id.time);
		mCurrentTime = (TextView) v.findViewById(R.id.time_current);
		mFormatBuilder = new StringBuilder();
		mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
		
	}

	// There are two scenarios that can trigger the seekbar listener to trigger:
	//
	// The first is the user using the touchpad to adjust the posititon of the
	// seekbar's thumb. In this case onStartTrackingTouch is called followed by
	// a number of onProgressChanged notifications, concluded by
	// onStopTrackingTouch.
	// We're setting the field "mDragging" to true for the duration of the
	// dragging
	// session to avoid jumps in the position in case of ongoing playback.
	//
	// The second scenario involves the user operating the scroll ball, in this
	// case there WON'T BE onStartTrackingTouch/onStopTrackingTouch
	// notifications,
	// we will simply apply the updated position without suspending regular
	// updates.
	private final OnSeekBarChangeListener mProgressListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			mDragging = true;

			// By removing these pending progress messages we make sure
			// that a) we won't update the progress while the user adjusts
			// the seekbar and b) once the user is done dragging the thumb
			// we will post one of these messages to the queue again and
			// this ensures that there will be exactly one message queued up.
			mMyHandler.removeMessages(SHOW_PROGRESS);
		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			if (!fromuser) {
				// We're not interested in programmatically generated changes to
				// the progress bar's position.
				return;
			}
			if (mPlayer != null) {
				mDuration = mPlayer.getDuration();
				mNewPosition = (int)((mDuration * (long)progress) / MAX_SEEK);
			}
			if (!mDragging) {
				mPlayer.seekTo(mNewPosition);
				setProgress();
				mMyHandler.sendEmptyMessage(SHOW_PROGRESS);
			}
			if (mCurrentTime != null) {
			    mCurrentTime.setText(stringForTime(mNewPosition));
			}
			
			// If the progress change caused by key event, update the frame and all states, 
			// including the pause/play button.
			if (bar instanceof VLWSeekBar && ((VLWSeekBar)bar).fromKeyEvent()) {
			    updateUIWhenProgressChanged(progress);
                ((VLWSeekBar)bar).resetFromKeyEvent();
            }
			
		}

		public void onStopTrackingTouch(SeekBar bar) {
		    updateUIWhenProgressChanged(-1);
		}
	};
	
	private  final OnSeekBarChangeListener mStartListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			if (!mIsEditmode) {
				pause();
				mIsEditmode = true;
			}
		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			if (!fromuser) {
				// We're not interested in programmatically generated changes to
				// the progress bar's position.
				return;
			}
			// TODO need to do so if onStartTrackingTouch is not received
			if (!mIsEditmode) {
				Xlog.w(TAG, "onProgressChanged, edit mode");
				pause();
				mIsEditmode = true;
			}
			
			mStartPosition = (int)((mDuration * (long)progress) / MAX_SEEK);
			mNewPosition = mStartPosition;
			if (mProgressBar != null) {
				mProgressBar.setProgress(progress);
				mProgressBar.setLeftThreshold(progress);
			}
			updateState();
			if (mCurrentTime != null) {
				mCurrentTime.setText(stringForTime(mStartPosition));
			}

			if (mEditorBarEnd != null) {
				mEditorBarEnd.setLeftThreshold(progress + mMinDuration);
			}
			
			// If the progress change caused by key event, update the frame and all states.
            if (bar instanceof VLWSeekBar && ((VLWSeekBar)bar).fromKeyEvent()) {
                updateUIWhenStartChanged(progress);
                ((VLWSeekBar)bar).resetFromKeyEvent();
            }
            
            // hide the progress at the left of start editor bar
			setLevelFromProgress(progress);
		}

		public void onStopTrackingTouch(SeekBar bar) {
		    updateUIWhenStartChanged(-1);
		}
	};
	
	private final OnSeekBarChangeListener mEndListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			if (!mIsEditmode) {
				pause();
				mIsEditmode = true;
			}
		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			if (!fromuser) {
				// We're not interested in programmatically generated changes to
				// the progress bar's position.
				return;
			}
			// TODO need to do so if onStartTrackingTouch is not received
			if (!mIsEditmode) {
				Xlog.w(TAG, "onProgressChanged, edit mode");
				pause();
				mIsEditmode = true;
			}
			
			mEndPosition = (int)((mDuration * (long)progress) / MAX_SEEK);
			mNewPosition = mStartPosition;
			updateState();
			if (mProgressBar != null) {
				int p = mEditorBarStart.getProgress();
				if (mProgressBar.getProgress() != p) {
					mProgressBar.setProgress(p);
				}
				mProgressBar.setRightThreshold(progress);
			}

			if (mEditorBarStart != null) {
				mEditorBarStart.setRightThreshold(progress - mMinDuration);
			}
			
			if (mCurrentTime != null) {
				mCurrentTime.setText(stringForTime(mNewPosition));
			}
			if (mEndTime != null) {
				mEndTime.setText(stringForTime(mEndPosition));
			}
			
			// If the progress change caused by key event, update the frame and all states.
			if (bar instanceof VLWSeekBar && ((VLWSeekBar)bar).fromKeyEvent()) {
			    updateUIWhenEndChanged(progress);
                ((VLWSeekBar)bar).resetFromKeyEvent();
            }
		}

		public void onStopTrackingTouch(SeekBar bar) {
		    updateUIWhenEndChanged(-1);
		}
	};
	
    /**
     * Update all UI state when the progress of the play seek bar changed, also
     * sets progresses, updateFrame will make the video view seek to the current
     * frame and updateUI will set the image resource of the pause/play button.
     * @param newProgress  < 0 from onStopTracking
     * 					> 0 from onProgressChanged
     */
    private void updateUIWhenProgressChanged(int newProgress) {
        // just only need to update frame when in pause state
        if (mIsEditmode || !mPlayer.isPlaying()) {
        	updateFrame(mNewPosition);
        }
        if (newProgress < 0) {
        	mDragging = false;
        	mMyHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    }

    /**
     * Update all UI state when the start seek bar changed, also sets progresses
     * and thresholds.
     * @param newProgress  < 0 from onStopTracking
     * 					> 0 from onProgressChanged
     */
    private void updateUIWhenStartChanged(int newProgress) {
        if (mDuration != 0) {
            int progress = newProgress > 0 ? newProgress : 
            	(int)(MAX_SEEK * (double)mStartPosition / mDuration + 0.5);
            mEditorBarStart.setProgress(progress);
            if (mProgressBar != null) {
                mProgressBar.setLeftThreshold(progress);
            }
            progress = (int) (MAX_SEEK * (double)mStartPosition / mDuration + mMinDuration);
            if (mEditorBarEnd != null) {
                mEditorBarEnd.setLeftThreshold(progress);
            }
        }

        updateFrame(mStartPosition);
        updateState();
    }

    /**
     * Update all UI state when the end seek bar changed, also sets progresses
     * and thresholds.
     * @param newProgress  < 0 from onStopTracking
     * 					> 0 from onProgressChanged
     */
    private void updateUIWhenEndChanged(int newProgress) {
        if (mDuration != 0) {
            int progress = newProgress > 0 ? newProgress : 
            	(int) (MAX_SEEK * (double)mEndPosition / mDuration + 0.5);
            mEditorBarEnd.setProgress(progress);
            if (mProgressBar != null) {
                mProgressBar.setRightThreshold(progress);
            }
            progress = (int) (MAX_SEEK * (double)mEndPosition / mDuration - mMinDuration);
            if (mEditorBarStart != null) {
                mEditorBarStart.setRightThreshold(progress);
            }
        }

        updateFrame(mEndPosition);
        updateState();
    }

	private final Handler mMyHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FADE_OUT:
				hide();
				break;
			case SHOW_PROGRESS:
				removeMessages(SHOW_PROGRESS);
				final long pos = setProgress();
				if (!mDragging && mPlayer.isPlaying()) {
					Message message = obtainMessage(SHOW_PROGRESS);
					sendMessageDelayed(message, MAX_SEEK - (pos % MAX_SEEK));
					// just play the clip session
					long limit = mNewPosition + 50;
					if ( limit >= mEndPosition || limit >= mDuration) {
						mNewPosition = mStartPosition;
						mPlayer.seekTo(mNewPosition);
						play();
						if (DEBUG) {
							Xlog.i(TAG, "playback completed, loop again");
						}
					}
				}
				break;
				
			default:
				Xlog.e(TAG, "unknown msg: " + msg);
				break;
			}
		}
	};

	protected String stringForTime(int timeMs) {
		int totalSeconds = timeMs / 1000;

		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;

		mFormatBuilder.setLength(0);
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds)
					.toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	private void updateFrame(final int position) {
		// update video view picture
		if (mPlayer == null) {
			return;
		}
		mPlayer.seekTo(position);
		/*Runnable r = new Runnable() {
			
			@Override
			public void run() {
				mPlayer.seekTo(position);
				synchronized (mLock) {
					mPendingRunnable = null;
				}
			}
		};
		if (DEBUG) {
			Xlog.d(TAG, "updateFrame() position=" + position);
		}
		synchronized (mLock) {
			if (mPendingRunnable == null) {
				mPendingRunnable = r;
				sendRunnableToWorker(r);
			}
		}*/

	}

	protected void updateUI(boolean isPlaying) {
		// notify anchor activity to update its UI
		ungetCallbacks();
		Callback callbacks[] = getCallbacks();
		if (callbacks != null) {
			for (Callback c : callbacks) {
				//boolean isPlaying = mPlayer.isPlaying();
				c.updateUI(isPlaying);
			}
		}
	}

	protected void updateState() {
		// notify anchor activity to update its state
		ungetCallbacks();
		Callback callbacks[] = getCallbacks();
		if (callbacks != null) {
			for (Callback c : callbacks) {
				c.updateState(mStartPosition, mEndPosition);
			}
		}
	}
	
	protected int setProgress() {
		if (mPlayer == null || mDragging) {
			return 0;
		}
		int curPos = mPlayer.getCurrentPosition();
		mDuration = mPlayer.getDuration();
		// walkaround for ALPS00238214 Rootcause: mPlayer.getCurrentPosition() < mNewPosition
		if (curPos < mNewPosition && curPos < mEndPosition && curPos > mStartPosition) {
			Xlog.w(TAG, "setProgress() Warning: mNewPosition=" + mNewPosition 
					+ " curPos=" + curPos + " duration=" + mDuration);
			mNewPosition += Math.min(100, mDuration - mNewPosition);
		} else {
			mNewPosition = curPos;
		}
		if (mProgressBar != null) {
			if (mDuration > 0) {
				// use long to avoid overflow
				int pos = (int)(MAX_SEEK * (double)mNewPosition / mDuration + 0.5);
				mProgressBar.setProgress(pos);
				if (DEBUG) {
					Xlog.d(TAG, "setProgress() mNewPosition=" + mNewPosition 
							+ ", mDuration=" + mDuration);
				}
			}
		}

		if (mEndTime != null) {
		    mEndTime.setText(stringForTime(mDuration));
		}
		
		if (mCurrentTime != null) {
		    mCurrentTime.setText(stringForTime(mNewPosition));
		}

		// If duration is short, refresh every 100ms
		if (mDuration < 10000) {
			return 900;
		}
		return mNewPosition;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (mEditorBarStart != null) {
			mEditorBarStart.setEnabled(enabled);
		}
		if (mEditorBarEnd != null) {
			mEditorBarEnd.setEnabled(enabled);
		}
		if (mProgressBar != null) {
			mProgressBar.setEnabled(enabled);
		}
		super.setEnabled(enabled);
	}

	public void setMdragging(boolean isTrue) {
		mDragging = isTrue;
	}

	/**
	 * Whether the player is in playing state.
	 * 
	 * @return
	 */
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}

	public void addCallback(Callback callback) {
		synchronized (mCallbacks) {
			// This is a linear search, but in practice we'll
			// have only a couple callbacks, so it doesn't matter.
			if (!mCallbacks.contains(callback)) {
				mCallbacks.add(callback);
			}
		}
	}

	public void removeCallback(Callback callback) {
		synchronized (mCallbacks) {
			mCallbacks.remove(callback);
		}
	}

	public Callback[] getCallbacks() {
		if (mHaveGottenCallbacks) {
			return mGottenCallbacks;
		}

		synchronized (mCallbacks) {
			final int N = mCallbacks.size();
			if (N > 0) {
				if (mGottenCallbacks == null || mGottenCallbacks.length != N) {
					mGottenCallbacks = new Callback[N];
				}
				mCallbacks.toArray(mGottenCallbacks);
			} else {
				mGottenCallbacks = null;
			}
			mHaveGottenCallbacks = true;
		}

		return mGottenCallbacks;
	}

	public void ungetCallbacks() {
		mHaveGottenCallbacks = false;
	}

	/**
	 * if you want to add some media control button for this media controller, maybe
	 * you need this callback notification
	 */
	public interface Callback {
		void updateUI(boolean isPlaying);
		void updateState(int start, int end);
	}
}
