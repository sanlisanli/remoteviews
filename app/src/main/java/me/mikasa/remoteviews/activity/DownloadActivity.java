package me.mikasa.remoteviews.activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.List;

import me.mikasa.remoteviews.R;
import me.mikasa.remoteviews.base.BaseActivity;
import me.mikasa.remoteviews.utils.DownloadReceiver;
import me.mikasa.remoteviews.utils.DownloadTask;
import me.mikasa.remoteviews.utils.NotificationUtil;

public class DownloadActivity extends BaseActivity {
    private static final String downloadUrl="https://imtt.dd.qq.com/16891/7FD0912B0033089F938B3A2D385B9B87.apk";
    //"?fsname=com.sohu.inputmethod.sogou_8.24.2_872.apk&amp;csr=1bbd";//搜狗输入法
    private static final String fileName="搜狗输入法.apk";//注意加上文件后缀
    private DownloadTask downloadTask;
    private static DownloadTask.STATE state=DownloadTask.STATE.IDLE;
    private DownloadReceiver downloadReceiver;
    private PendingIntent broadcastPendingIntent;//好像不能设置为getRemoteViews()的局部变量
    private static boolean isFirst=true;
    private NotificationUtil notificationUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int setLayoutResId() {
        return R.layout.activity_download;
    }

    @Override
    protected void initData() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadReceiver);
    }

    @Override
    protected void initView() {
        TextView tv_info=findViewById(R.id.tv_info);
        String msg=getResources().getString(R.string.sougou_info);
        tv_info.setText(msg.replace("。"," "+"\r\n"+" "+"\r\n"));
    }

    @Override
    protected void initListener() {
        findViewById(R.id.btn_start_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] permission={Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestRuntimePermission(permission,permissionListener);//申请存储权限
            }
        });
    }
    private void download(){
        if (state==DownloadTask.STATE.IDLE){
            state=DownloadTask.STATE.RUNNING;
            downloadTask=new DownloadTask(downloadUrl,fileName,downloadListener);
            downloadTask.execute();//开始下载任务
            showToast(downloadTask.getFileName()+"开始下载");
            if (isFirst){
                initReceiver();//动态注册广播接收器
                notificationUtil=new NotificationUtil(this);
                isFirst=false;
            }
        }else {
            showToast("当前已有下载任务，需待任务完成");
        }
    }
    private void initReceiver(){
        downloadReceiver=new DownloadReceiver(broadcastListener);
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(DownloadReceiver.DOWNLOADRECEIVER);
        registerReceiver(downloadReceiver,intentFilter);//注册广播接收器
    }

    private void showNotification(int progress,boolean paused){
        RemoteViews remoteView;
        if (!paused){
            remoteView=getRemoteViews(progress,false);
        }else{
            remoteView=getRemoteViews(progress,true);
        }
        notificationUtil.getBuilder().setContent(remoteView);
        //builder.setContent(remoteView);//默认情况下通知高度为64dp
        notificationUtil.sendNotification();
    }
    private DownloadTask.DownloadListener downloadListener=new DownloadTask.DownloadListener() {
        @Override
        public void onUpdate(int progress) {
            showNotification(progress,false);
        }

        @Override
        public void onSuccess() {
            showToast(downloadTask.getFileName()+"下载完成");
            downloadTask=null;
            state=DownloadTask.STATE.IDLE;
        }

        @Override
        public void onFailed() {
            showToast(downloadTask.getFileName()+"下载失败");
            downloadTask=null;
            state=DownloadTask.STATE.IDLE;
        }

        @Override
        public void onPaused(int progress) {
            downloadTask=null;
            state=DownloadTask.STATE.IDLE;
            if (progress<100){
                showNotification(progress,true);
            }
        }

        @Override
        public void onCancel() {
            showToast(downloadTask.getFileName()+"取消下载");
            downloadTask=null;
            state=DownloadTask.STATE.IDLE;
        }

        @Override
        public void onSDcardMiss() {
            showToast("检测不到SD卡");
            downloadTask=null;
            state=DownloadTask.STATE.IDLE;
        }
    };
    private PermissionListener permissionListener=new PermissionListener() {
        @Override
        public void onGranted() {
            download();
        }

        @Override
        public void onDenied(List<String> deniedPermission) {

        }
    };
    private DownloadReceiver.BroadcastListener broadcastListener=new DownloadReceiver.BroadcastListener() {
        @Override
        public void onReceive(int state) {
            switch (state){
                case DownloadReceiver.download_continue:
                    download();
                    downloadTask.reStart();
                    break;
                case DownloadReceiver.download_paused:
                    downloadTask.pause();
                    break;
            }
        }
    };
    private RemoteViews getRemoteViews(int progress,boolean paused){
        RemoteViews remoteViews=new RemoteViews(this.getPackageName(),R.layout.layout_remoteviews_download);
        remoteViews.setImageViewResource(R.id.iv_icon,R.drawable.sougou);
        Intent intent=new Intent(DownloadReceiver.DOWNLOADRECEIVER);
        if (!paused){
            remoteViews.setTextColor(R.id.tv_title,Color.parseColor("#69f0ae"));
            remoteViews.setImageViewResource(R.id.iv_state,R.drawable.bofang_white);
            if (progress==100){
                remoteViews.setTextViewText(R.id.tv_title,downloadTask.getFileName()+"下载完成");
                remoteViews.setImageViewResource(R.id.iv_state,R.drawable.complete_white);
                intent.putExtra("state","complete");
            }else {
                remoteViews.setTextViewText(R.id.tv_title,downloadTask.getFileName()+"下载中...");
                intent.putExtra("state","paused");
            }
        }else {
            remoteViews.setTextColor(R.id.tv_title,Color.parseColor("#ff9100"));
            remoteViews.setImageViewResource(R.id.iv_state,R.drawable.zanting_white);
            remoteViews.setTextViewText(R.id.tv_title,"下载暂停");
            intent.putExtra("state","continue");
        }
        remoteViews.setProgressBar(R.id.notification_progress,100,progress,false);
        broadcastPendingIntent=PendingIntent.getBroadcast(this,0,
                intent,PendingIntent.FLAG_CANCEL_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.iv_state,broadcastPendingIntent);
        return remoteViews;
    }
}
