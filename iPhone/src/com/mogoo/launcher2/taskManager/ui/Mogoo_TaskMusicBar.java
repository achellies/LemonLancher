package com.mogoo.launcher2.taskManager.ui;

import com.mogoo.launcher.R;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Mogoo_TaskMusicBar extends RelativeLayout{
	
	private SeekBar mVoiceProgress;
	private ImageView mVoiceIcon;
	private AudioManager audioManager;
	private int maxvolume;
	private int maxRingVolume;
	
    private Context mContext;
    private Resources resources;
    
	public Mogoo_TaskMusicBar(Context context) {
		super(context);
		LayoutInflater mInflater=LayoutInflater.from(context);
		mInflater.inflate(R.layout.mogoo_task_music_bar, this,true);
		mContext=context;
		resources=context.getResources();

	    audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
	    maxvolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	    
	    maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
	    
		init();
	}
	
	public AudioManager getAudioManager() {
		return audioManager;
	}
    private void findViews(){
    	mVoiceProgress = (SeekBar)findViewById(R.id.voiceprogress);
    	mVoiceIcon = (ImageView)findViewById(R.id.voiceicon);
    }
    private void setListener(){
    	mVoiceProgress.setOnSeekBarChangeListener(mVoiceSeekListener);
    }
    private void init(){
		findViews();
		setListener();
		if(!audioManager.isMusicActive()) {
			
			setRingVolume();
			
		} else {
			setVolume();
		}
	  
    }
	/**
     *  设置媒体音量
     *  
     */
    public void setVolume() {
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        mVoiceProgress.setProgress(volume*100/maxvolume);
    }
    
    public void setRingVolume() {
    	Log.d("lss", " audioManager.getMode()=" + audioManager.getMode());
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        mVoiceProgress.setProgress(volume*100/maxRingVolume);
    }
    
    /**
     * 音量进度监听
     * 
     */
    private OnSeekBarChangeListener mVoiceSeekListener = new OnSeekBarChangeListener() {
        int volume;
        public void onStartTrackingTouch(SeekBar bar) {
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
        	if( !audioManager.isMusicActive()) {
        		 volume = maxRingVolume*progress/100;
        	} else {
        		volume = maxvolume*progress/100;
        	}
        }
        public void onStopTrackingTouch(SeekBar bar) {
        	if(!audioManager.isMusicActive()) {
        		audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        	} else {
        		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        	}
        }
    };

}
