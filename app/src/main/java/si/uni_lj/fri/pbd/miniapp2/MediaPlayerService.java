package si.uni_lj.fri.pbd.miniapp2;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

public class MediaPlayerService extends Service {

    private AccelerationService accelerationService;
    private boolean serviceBound=false;

    private int currPosition=0;

    private MediaPlayer mediaPlayer;
    private Uri mediaFile;
    boolean stopped;
    private Field[] rawSongs;
    private MediaMetadataRetriever mediaMetadataRetriever;
    private NotificationManager managerCompat;
    private static final String TAG = MediaPlayerService.class.getSimpleName();

    public static final String ACTION_STOP = "stop_service";
    public static final String ACTION_START = "start_service";
    public static final String ACTION_PLAY = "play_service";
    public static final String ACTION_PAUSE = "pause_service";
    public static final String ACTION_EXIT = "exit_service";


    public static final String BROADCAST_ACTION_2 = "si.uni_lj.fri.pbd.miniapp2.2";
    private BroadcastReceiver myBroadCastReceiver;

    private static final String channelID = "background_timer";

    private boolean gesturesOn=false;

    private boolean exitPressed=false;

    static final int NOTIFICATION_ID=7;

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating media player service");
        stopped=true;
        initMediaPlayer();
        createNotificationChannel();
        myBroadCastReceiver=new MyBroadCastReceiver();
        registerMyReceiver();

        Log.d(TAG, "Starting and binding acceleration service");
        Intent i=new Intent();
        i.setClass(this, AccelerationService.class);
        i.setAction(AccelerationService.ACTION_START);
        startService(i);

        bindService(i, mConnection, 0);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service bound");

            AccelerationService.RunServiceBinder binder = (AccelerationService.RunServiceBinder) iBinder;
            accelerationService = binder.getService();
            serviceBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Service disconnect");

            serviceBound = false;
        }
    };



    //initial notification
    public void foreground(){
        startForeground(NOTIFICATION_ID, createNotification("",""));
    }

    public boolean isPlaying(){
        return mediaPlayer.isPlaying();
    };

    private void initMediaPlayer() {
        rawSongs = R.raw.class.getFields();
        mediaPlayer = new MediaPlayer();
    }

    //choose random song from /res/raw/
    private void chooseRandom(){
        int rand=(int) (Math.random()*rawSongs.length);
        mediaFile= Uri.parse("android.resource://" + getPackageName() + "/raw/" + rawSongs[rand].getName());
    }


    public void play(){
        if(stopped) {
            try {
                chooseRandom();
                mediaPlayer.reset();
                mediaPlayer.setDataSource(this, mediaFile);
                mediaPlayer.prepare();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(currPosition);
            mediaPlayer.start();
            stopped=false;
        }
        updateDisplay();
    }

    public void pause(){
        System.out.println(mediaPlayer.isPlaying());
        if(mediaPlayer.isPlaying()){
            currPosition=mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    //stop media player command
    public void stop(){
        mediaPlayer.stop();
        stopped=true;
        currPosition=0;
    }


    public void setGesturesOn(){
        gesturesOn=true;
        accelerationService.setGesturesOn();
    }

    public void setGesturesOff(){
        gesturesOn=false;
        if(serviceBound)
            unbindService(mConnection);
        serviceBound=false;
        accelerationService.setGesturesOff();
    }

    //get commands from notification intents and act accordingly
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");
        System.out.println(intent.getAction());

        if (intent.getAction().equals(ACTION_STOP)) {
            stop();
        }
        if (intent.getAction().equals(ACTION_PLAY)) {
            play();
        }
        if (intent.getAction().equals(ACTION_PAUSE)) {
            pause();
        }
        if (intent.getAction().equals(ACTION_EXIT)) {
            exitPressed=true;
        }
        return Service.START_STICKY;
    }

    public class RunServiceBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }


    @Nullable
    @Override
    public IBinder onBind (Intent intent){
        Log.d(TAG, "Binding service");
        RunServiceBinder serviceBinder = new RunServiceBinder();
        return serviceBinder;
    }

    //when service is destroyed unbind connection to acceleration service
    @Override
    public void onDestroy () {
        super.onDestroy();
        if(serviceBound){
            unbindService(mConnection);
            serviceBound=false;
        }
        else{
            stopService(new Intent(this, AccelerationService.class));
        }
        unregisterReceiver(myBroadCastReceiver);
        stopForeground(true);
        mediaPlayer.release();
        Log.d(TAG, "Destroying media player service");
    }


    /**
     * Creates a notification for placing the service into the foreground
     * 3 buttons added via intents
     * @return a notification for interacting with the service when in the foreground
     */
    private Notification createNotification (String songTime, String songTitle) {

        Intent actionIntent = new Intent(this, MediaPlayerService.class);
        actionIntent.setAction(ACTION_STOP);
        PendingIntent actionPendingIntent = PendingIntent.getService(this, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setContentTitle(songTime)
                .setContentText(songTitle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setChannelId(channelID);

        builder.addAction(android.R.drawable.ic_media_pause, "Stop", actionPendingIntent);

        if(mediaPlayer.isPlaying()){
            Intent pauseIntent = new Intent(this, MediaPlayerService.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent);
        }
        else{
            Intent playIntent = new Intent(this, MediaPlayerService.class);
            playIntent.setAction(ACTION_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.ic_media_pause, "Play", playPendingIntent);
        }

        Intent exitIntent = new Intent(this, MediaPlayerService.class);
        exitIntent.setAction(ACTION_EXIT);
        PendingIntent exitPendingIntent = PendingIntent.getService(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(android.R.drawable.ic_media_pause, "Exit", exitPendingIntent);

        return builder.build();
    }

    //creates channel for notifications
    private void createNotificationChannel () {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        } else {
            NotificationChannel channel = new NotificationChannel(MediaPlayerService.channelID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_desc));
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);

            managerCompat = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            managerCompat.createNotificationChannel(channel);
        }
    }

    /**
     * This method is responsible to send broadCast to main activity
     * */
    private void sendMyBroadCast() {
        try {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(MainActivity.BROADCAST_ACTION);

            String duration = String.valueOf(mediaPlayer.getDuration() / 1000);
            String currPosition = String.valueOf(mediaPlayer.getCurrentPosition() / 1000);
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(this, mediaFile);
            String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

            int dur = Integer.parseInt(duration);
            int curr = Integer.parseInt(currPosition);

            String exitPressedS=String.valueOf(exitPressed);

            broadCastIntent.putExtra("duration", duration);
            broadCastIntent.putExtra("currPosition", currPosition);
            broadCastIntent.putExtra("exitPressed", exitPressedS);

            String currPositionSec = String.format("%02d", curr % 60);
            currPosition = String.format("%02d", curr / 60);
            String durationSec = String.format("%02d", dur % 60);
            duration = String.format("%02d", dur / 60);

            String songTime = currPosition + ":" + currPositionSec + "/" + duration + ":" + durationSec;
            String songTitle = artist + " - " + title;


            broadCastIntent.putExtra("trackTitle", title);
            broadCastIntent.putExtra("trackArtist", artist);

            sendBroadcast(broadCastIntent);

            //updates notification
            startForeground(NOTIFICATION_ID, createNotification(songTime, songTitle));

            //if exit button in notification pressed, onDestroy method called
            if(exitPressed) {
                onDestroy();
                exitPressed = false;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //updates display every second by sending broadcasts
    private void updateDisplay() {
        if(mediaPlayer.isPlaying()) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    sendMyBroadCast();
                }

            }, 0, 1000);//Update text every second
        }
    }

    /**
     * This method is responsible to register an action to BroadCastReceiver
     * */
    private void registerMyReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BROADCAST_ACTION_2);
            registerReceiver(myBroadCastReceiver, intentFilter);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * MyBroadCastReceiver is responsible to receive broadCast from register action
     * */
    class MyBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Log.d(TAG, "mediaPlayer onReceive() called");
                String command = intent.getStringExtra("command"); // data is a key specified to intent while sending broadcast
                if(command.equals("HORIZONTAL") && mediaPlayer.isPlaying())
                    pause();
                if(command.equals("VERTICAL") && !mediaPlayer.isPlaying())
                    play();

            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

