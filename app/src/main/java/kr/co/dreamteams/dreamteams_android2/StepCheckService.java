package kr.co.dreamteams.dreamteams_android2;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StepCheckService extends Service implements SensorEventListener {

    Common common = new Common(this);

    public static int count = StepValue.Step;
    private long lastTime;
    private float speed;
    private float lastX;
    private float lastY;
    private float lastZ;

    private float x, y, z;
    private static final int SHAKE_THRESHOLD = 800;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

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
    } // end of onCreate

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
            StepValue.Step = 0;
        } // end of if
    } // end of onDestroy

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i("onSensorChanged", "IN");
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long gabOfTime = (currentTime - lastTime);

            if (gabOfTime > 100) { //  gap of time of step count
                Log.i("onSensorChanged_IF", "FIRST_IF_IN");
                lastTime = currentTime;

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / gabOfTime * 12000;

                if (speed > SHAKE_THRESHOLD) {
                    Log.i("onSensorChanged_IF", "SECOND_IF_IN");
                    Intent myFilteredResponse = new Intent("make.a.yong.manbo");

                    StepValue.Step = count++;

                    String msg = StepValue.Step + "";
                    myFilteredResponse.putExtra("stepService", "오늘 걸음수 : " + msg);

                    sendBroadcast(myFilteredResponse);

                    if(count%10==0) {
                        long now = System.currentTimeMillis();
                        Date date = new Date(now);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        //SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
                        String today = sdf.format(date);

                        common.putSP("step_count", String.valueOf(count));
                        String step_record_date =  common.getSP("step_record_date");

                        if(!step_record_date.equals(today)){
                            //common.putSP("2018-10-20", "20");
                            //common.putSP("2018-10-22", "22");
                            //common.putSP("2018-10-24", "24");
                            //common.putSP("2018-10-25", "25");
                            //common.putSP("2018-10-26", "26");
                            //common.putSP("2018-10-27", "27");
                            //common.putSP("2018-10-28", "28");

                            String lastday = common.getSP("step_record_date");
                            common.putSP(lastday,String.valueOf(count));
                            common.putSP("step_record_date", today);
                            common.putSP("step_count", "0");
                            StepValue.Step = 0;
                            count = 0;
                        }

                    }

                } // end of if

                lastX = event.values[0];
                lastY = event.values[1];
                lastZ = event.values[2];
            } // end of if
        } // end of if

    } // end of onSensorChanged

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

