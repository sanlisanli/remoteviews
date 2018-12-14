package me.mikasa.remoteviews.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadReceiver extends BroadcastReceiver {
    public static final String DOWNLOADRECEIVER="me.mikasa.remoteviews.utils.DownloadReceiver";
    public static final int download_continue=0;
    public static final int download_paused=1;
    private BroadcastListener mListener;

    public DownloadReceiver(){
    }
    public DownloadReceiver(BroadcastListener listener){
        this.mListener=listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String state=intent.getStringExtra("state");
        switch (intent.getAction()){
            case DOWNLOADRECEIVER:
                if (mListener!=null){
                    if (state.equals("continue")){
                        mListener.onReceive(download_continue);
                    }else if (state.equals("paused")){
                        mListener.onReceive(download_paused);
                    }
                }
                break;
        }
    }

    public interface BroadcastListener{
        void onReceive(int state);
    }
}
