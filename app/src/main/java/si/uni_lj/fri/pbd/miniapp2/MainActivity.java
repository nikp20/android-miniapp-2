package si.uni_lj.fri.pbd.miniapp2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String BROADCAST_ACTION = "si.uni_lj.fri.pbd.miniapp2";

    private MediaPlayerService mediaPlayerService;
    private boolean serviceBound;

    private MyBroadCastReceiver myBroadCastReceiver;

    private TextView songIdTextView;
    private TextView songInfo;

    private String prevArtist="";
    private String prevTitle="";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView imageView=findViewById(R.id.image_home);
        imageView.setImageResource(R.mipmap.ul100);
        songIdTextView=findViewById(R.id.songDuration);
        songInfo=findViewById(R.id.songInfo);
        myBroadCastReceiver=new MyBroadCastReceiver();
        registerMyReceiver();
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service bound");

            MediaPlayerService.RunServiceBinder binder = (MediaPlayerService.RunServiceBinder) iBinder;
            mediaPlayerService = binder.getService();
            serviceBound = true;
            //mediaPlayerService.background();
            mediaPlayerService.foreground();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Service disconnect");

            serviceBound = false;
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Starting and binding service");
        Intent i=new Intent();
        i.setClass(this, MediaPlayerService.class);
        i.setAction(MediaPlayerService.ACTION_START);
        startService(i);
        bindService(i, mConnection, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(serviceBound && mediaPlayerService.isPlaying()){
            System.out.println("test");
        }
        else{
            stopService(new Intent(this, MediaPlayerService.class));
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(serviceBound){
            unbindService(mConnection);
            serviceBound=false;
        }
        unregisterReceiver(myBroadCastReceiver);
    }

    public void playButtonClick(View v) {
        if (serviceBound && !mediaPlayerService.isPlaying()) {
            Log.d(TAG, "Starting media player");
            mediaPlayerService.play();
        }
    }
    public void stopButtonClick(View v) {
        if (serviceBound) {
            Log.d(TAG, "Stoping media player");
            mediaPlayerService.stop();
        }
    }
    public void pauseButtonClick(View v) {
        if (serviceBound && mediaPlayerService.isPlaying()) {
            Log.d(TAG, "Pausing media player");
            mediaPlayerService.pause();
        }
    }
    public void exitButtonClick(View v) {
        if (serviceBound) {
            Log.d(TAG, "Exiting media player");
            //mediaPlayerService.exit();
            if(mediaPlayerService.isPlaying()){
                mediaPlayerService.stop();
            }
            onDestroy();
            finishAndRemoveTask();
            //updateUIStartRun();
        }
    }

    public void gesturesOnClicked(View v){
        if(serviceBound){
            mediaPlayerService.setGesturesOn();
            Toast toast = Toast.makeText(this, "Gestures activated", Toast.LENGTH_SHORT);
            toast.show();

        }
    }
    public void gesturesOffClicked(View v){
        if(serviceBound){
            mediaPlayerService.setGesturesOff();
            Toast toast = Toast.makeText(this, "Gestures deactivated", Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    /**
     * This method is responsible to register an action to BroadCastReceiver
     * */
    private void registerMyReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BROADCAST_ACTION);
            registerReceiver(myBroadCastReceiver, intentFilter);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * MyBroadCastReceiver is responsible to receive broadCast from media player service
     * then updates the text views with received data
     * */
    class MyBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if(mediaPlayerService.isPlaying()) {
                    Log.d(TAG, "onReceive() called");

                    if (intent.getStringExtra("exitPressed") != null && intent.getStringExtra("exitPressed").equals("true")) {
                        onDestroy();
                        finishAndRemoveTask();
                    }

                    //uncomment this line if you had sent some data
                    String duration = intent.getStringExtra("duration"); // data is a key specified to intent while sending broadcast
                    String currPosition = intent.getStringExtra("currPosition"); // data is a key specified to intent while sending broadcast
                    String trackTitle = intent.getStringExtra("trackTitle"); // data is a key specified to intent while sending broadcast
                    String trackArtist = intent.getStringExtra("trackArtist"); // data is a key specified to intent while sending broadcast

                    String currPositionSec;
                    String durationSec;
                    int dur = Integer.parseInt(duration);
                    int curr = Integer.parseInt(currPosition);

                    if (songIdTextView != null) {
                        currPositionSec = String.format("%02d", curr % 60);
                        currPosition = String.format("%02d", curr / 60);
                        durationSec = String.format("%02d", dur % 60);
                        duration = String.format("%02d", dur / 60);
                        songIdTextView.setText(currPosition + ":" + currPositionSec + "/" + duration + ":" + durationSec);
                    }
                    if (songInfo != null && !prevArtist.equals(trackArtist)) {
                        songInfo.setText(trackArtist + " - " + trackTitle);
                    }
                }

            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
