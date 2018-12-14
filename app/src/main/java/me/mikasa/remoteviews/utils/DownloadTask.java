package me.mikasa.remoteviews.utils;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String,Integer,Integer> {
    public static final String directoryPath=Environment.getExternalStorageDirectory()+
            File.separator+"downloadPath";
    private DownloadListener mListener;
    private String mUrl,fileName;
    private static boolean isPaused=false;
    private static boolean isCancel=false;
    private static final int DOWNLOAD_SUCCESS = 0;
    private static final int DOWNLOAD_FAILED = 1;
    private static final int DOWNLOAD_PAUSE = 2;
    private static final int DOWNLOAD_CANCEL = 3;
    private static final int SDCARD_MISS=4;
    private static final int updateInterval=8;//更新间隔，避免频繁更新
    private int currentProgress=0;//当前下载进度
    public DownloadTask(String url,String name,DownloadListener listener){
        this.mUrl=url;
        this.fileName=name;//注意自行加文件后缀，如xxx.apk，xxx.jpg，xxx.mp3
        this.mListener=listener;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            return SDCARD_MISS;
        }
        InputStream is=null;
        RandomAccessFile raf=null;
        File file=null;
        long startLength=0;//已下载文件长度
        long contentLength;//文件总长度
        try {
            File directory=new File(directoryPath);
            if (!directory.exists()){
                directory.mkdir();
            }
            file=new File(directoryPath,fileName);
            if (!file.exists()){
                file.createNewFile();
            }else {
                startLength=file.length();
            }
            OkHttpClient client=new OkHttpClient();
            Request request=new Request.Builder()
                    .url(mUrl)
                    .addHeader("RANGE","bytes="+startLength+"-")
                    .build();
            Response response=client.newCall(request).execute();
            if (response!=null){
                Log.i("opopop",String.valueOf(response.body().contentLength()));
                is=response.body().byteStream();
                contentLength=startLength+response.body().contentLength();
                raf=new RandomAccessFile(file,"rwd");
                raf.seek(startLength);
                byte[] bytes=new byte[6*1024];
                int total=0;
                int length;
                while ((length = is.read(bytes)) != -1){
                    if (isCancel){
                        return DOWNLOAD_CANCEL;
                    }else if (isPaused){
                        return DOWNLOAD_PAUSE;
                    }else {
                        total+=length;
                        raf.write(bytes,0,length);
                        int progress=(int) ((total + startLength) * 100 / contentLength);
                        publishProgress(progress);//更新进度
                    }
                }
                response.body().close();
                return DOWNLOAD_SUCCESS;
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                if (is!=null){
                    is.close();
                }
                if (raf!=null){
                    raf.close();
                }
                if (isCancel&&file!=null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return DOWNLOAD_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mListener==null){
            return;
        }
        if (values[0]==0){
            mListener.onUpdate(0);
        }else if (values[0]==100){
            mListener.onUpdate(100);
        }else if (values[0]-currentProgress>updateInterval){
            currentProgress=values[0];
            mListener.onUpdate(values[0]);
        }else if (isPaused){//如果暂停立即通知更新
            mListener.onPaused(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer){
            case DOWNLOAD_SUCCESS:
                mListener.onSuccess();
                break;
            case DOWNLOAD_FAILED:
                mListener.onFailed();
                break;
            case DOWNLOAD_PAUSE:
                mListener.onPaused(currentProgress);
                break;
            case DOWNLOAD_CANCEL:
                mListener.onCancel();
                break;
            case SDCARD_MISS:
                mListener.onSDcardMiss();
                break;
                default:
                    break;
        }
    }
    public void reStart(){
        isPaused=false;
    }
    public void pause(){
        isPaused=true;
    }
    public void cancel(){
        isCancel=true;
    }

    public interface DownloadListener{
        void onUpdate(int progress);
        void onSuccess();
        void onFailed();
        void onPaused(int progress);
        void onCancel();
        void onSDcardMiss();
    }
    public String getFileName(){
        return fileName.substring(0,fileName.lastIndexOf("."));
    }
    public enum STATE{IDLE,RUNNING}
}
