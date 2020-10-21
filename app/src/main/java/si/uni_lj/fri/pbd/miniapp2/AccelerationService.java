package si.uni_lj.fri.pbd.miniapp2;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

public class AccelerationService extends Service implements SensorEventListener{


    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private double prevX=0;
    private double prevY=0;
    private double prevZ=0;
    private String command;

    private boolean gesturesOn=false;

    private static final double noise=5.0;
    private static final String TAG = AccelerationService.class.getSimpleName();
    public static final String ACTION_START = "start_service";


    @Override
    public void onCreate(){
        Log.d(TAG, "Creating accelerator service");
        initSensor();
    }

    public class RunServiceBinder extends Binder {
        AccelerationService getService() {
            return AccelerationService.this;
        }
    }

    private void initSensor(){
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Nullable
    @Override
    public IBinder onBind (Intent intent) {
        Log.d(TAG, "Binding service");
        AccelerationService.RunServiceBinder serviceBinder = new AccelerationService.RunServiceBinder();
        return serviceBinder;
    }

    public void setGesturesOn(){
        gesturesOn=true;
        System.out.println("it worked boi");
    }
    public void setGesturesOff(){
        gesturesOn=false;
    }

    //calculate sensor values and send command
    @Override
    public void onSensorChanged(SensorEvent event){
        if(gesturesOn) {
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            double dx = Math.abs(prevX - x);
            double dy = Math.abs(prevY - y);
            double dz = Math.abs(prevZ - z);
            prevX = event.values[0];
            prevY = event.values[1];
            prevZ = event.values[2];
            dx = dx < noise ? 0 : dx;
            dy = dy < noise ? 0 : dy;
            dz = dz < noise ? 0 : dz;
            command = "IDLE";
            if (dx > dz)
                command = "HORIZONTAL";
            if (dz > dx)
                command = "VERTICAL";
            if (!command.equals("IDLE")) {
                updateDisplay();

            }
        }
    }

    private void sendMyBroadCast()
    {
        try
        {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(MediaPlayerService.BROADCAST_ACTION_2);

            broadCastIntent.putExtra("command", command);

            sendBroadcast(broadCastIntent);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    //send broadcast every second
    private void updateDisplay() {
        if(gesturesOn) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    sendMyBroadCast();
                }

            }, 0, 1000);//Update text every second
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int a){

    }
}
