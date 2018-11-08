package kr.co.dreamteams.dreamteams_android2;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StepCheckService extends Service implements SensorEventListener {

    Common common = new Common(this);

    public static int count = 0;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    private static final int ACCEL_RING_SIZE = 50;
    private static final int VEL_RING_SIZE = 10;

    // change this threshold according to your sensitivity preferences
    private static final float STEP_THRESHOLD = 12;

    private static final int STEP_DELAY_NS = 150000000;

    private int accelRingCounter = 0;
    private float[] accelRingX = new float[ACCEL_RING_SIZE];
    private float[] accelRingY = new float[ACCEL_RING_SIZE];
    private float[] accelRingZ = new float[ACCEL_RING_SIZE];
    private int velRingCounter = 0;
    private float[] velRing = new float[VEL_RING_SIZE];
    private long lastStepTimeNs = 0;
    private float oldVelocityEstimate = 0;

    NotificationCompat.Builder builder;
    RemoteViews remoteViews;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("onCreate", "IN");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        String step_count = common.getSP("step_count");

        if(!step_count.isEmpty())
        {
            count = Integer.parseInt(step_count);
        }

        myStartForeground(step_count);

    } // end of onCreate


    public  void myStartForeground(String text){

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("Refresh_YN","Y");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        //remoteViews = new RemoteViews(getPackageName(), R.layout.notification_service);

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "snwodeer_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "SnowDeer Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null,null);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        if(text.isEmpty())
        {
            text = "만보기 실행중";
        }
        else {
            text = "오늘 걸음수 : " + text + "보";
        }
        builder.setSmallIcon(R.mipmap.icon)
                //.setContent(remoteViews)
                //.setContentTitle("드림팀즈")
                .setContentText(text)
                .setSound(null)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(pendingIntent);

        startForeground(1, builder.build());

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i("onStartCommand", "IN");
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        } // end of if



        return START_STICKY;
    } // end of onStartCommand


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("onDestroy", "IN");
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            count = 0;
        } // end of if
    } // end of onDestroy


    public void calculate(long timeNs, float x, float y, float z){
        float[] currentAccel = new float[3];
        currentAccel[0] = x;
        currentAccel[1] = y;
        currentAccel[2] = z;

        // First step is to update our guess of where the global z vector is.
        accelRingCounter++;
        accelRingX[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[0];
        accelRingY[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[1];
        accelRingZ[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[2];

        float[] worldZ = new float[3];
        worldZ[0] = this.sum(accelRingX) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[1] = this.sum(accelRingY) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[2] = this.sum(accelRingZ) / Math.min(accelRingCounter, ACCEL_RING_SIZE);

        float normalization_factor = this.norm(worldZ);

        worldZ[0] = worldZ[0] / normalization_factor;
        worldZ[1] = worldZ[1] / normalization_factor;
        worldZ[2] = worldZ[2] / normalization_factor;

        float currentZ = this.dot(worldZ, currentAccel) - normalization_factor;
        velRingCounter++;
        velRing[velRingCounter % VEL_RING_SIZE] = currentZ;

        float velocityEstimate = this.sum(velRing);

        if (velocityEstimate > 5) {
            //common.log("vel : " + velocityEstimate);
            //common.log("st  : " + Float.toString(STEP_THRESHOLD));
            //common.log("time: " + Long.toString(timeNs-lastStepTimeNs));
            //common.log("last: " + Long.toString(lastStepTimeNs));
            //common.log("DELA: " + Long.toString(STEP_DELAY_NS));
        }

        if (velocityEstimate > STEP_THRESHOLD
                && oldVelocityEstimate <= STEP_THRESHOLD
                && (timeNs - lastStepTimeNs > STEP_DELAY_NS)) {
            reportStep();
            lastStepTimeNs = timeNs;

        }

        oldVelocityEstimate = velocityEstimate;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            calculate(event.timestamp, event.values[0], event.values[1], event.values[2]);
        }

    } // end of onSensorChanged

    private void reportStep(){


        count++;

        //Intent myFilteredResponse = new Intent("make.a.yong.manbo");
        //String msg = StepValue.Step + "";
        //myFilteredResponse.putExtra("stepService", "오늘 걸음수 : " + msg);

        //sendBroadcast(myFilteredResponse);
        common.log("오늘걸음수 : " + count);
        myStartForeground(Integer.toString(count));

        if(count%10==0) {
            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            //SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
            String today = sdf.format(date);

            common.putSP("step_count", String.valueOf(count));
            String step_record_date =  common.getSP("step_record_date");

            if(!step_record_date.equals(today)){

                String lastday = common.getSP("step_record_date");
                common.putSP(lastday,String.valueOf(count));
                common.putSP("step_record_date", today);
                common.putSP("step_count", "0");
                count = 0;
            }

        }

    }

    public static float sum(float[] array) {
        float retval = 0;
        for (int i = 0; i < array.length; i++) {
            retval += array[i];
        }
        return retval;
    }

    public static float norm(float[] array) {
        float retval = 0;
        for (int i = 0; i < array.length; i++) {
            retval += array[i] * array[i];
        }
        return (float) Math.sqrt(retval);
    }


    public static float dot(float[] a, float[] b) {
        float retval = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return retval;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

