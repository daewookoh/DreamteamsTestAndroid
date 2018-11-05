package kr.co.dreamteams.dreamteams_android2;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    private static final int ACCEL_RING_SIZE = 50;
    private static final int VEL_RING_SIZE = 10;

    // change this threshold according to your sensitivity preferences
    private static final float STEP_THRESHOLD = 13;

    private static final int STEP_DELAY_NS = 250000000;

    private int accelRingCounter = 0;
    private float[] accelRingX = new float[ACCEL_RING_SIZE];
    private float[] accelRingY = new float[ACCEL_RING_SIZE];
    private float[] accelRingZ = new float[ACCEL_RING_SIZE];
    private int velRingCounter = 0;
    private float[] velRing = new float[VEL_RING_SIZE];
    private long lastStepTimeNs = 0;
    private float oldVelocityEstimate = 0;

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
        worldZ[0] = SensorFilter.sum(accelRingX) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[1] = SensorFilter.sum(accelRingY) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[2] = SensorFilter.sum(accelRingZ) / Math.min(accelRingCounter, ACCEL_RING_SIZE);

        float normalization_factor = SensorFilter.norm(worldZ);

        worldZ[0] = worldZ[0] / normalization_factor;
        worldZ[1] = worldZ[1] / normalization_factor;
        worldZ[2] = worldZ[2] / normalization_factor;

        float currentZ = SensorFilter.dot(worldZ, currentAccel) - normalization_factor;
        velRingCounter++;
        velRing[velRingCounter % VEL_RING_SIZE] = currentZ;

        float velocityEstimate = SensorFilter.sum(velRing);

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

        //Intent myFilteredResponse = new Intent("make.a.yong.manbo");

        StepValue.Step = count++;

        //String msg = StepValue.Step + "";
        //myFilteredResponse.putExtra("stepService", "오늘 걸음수 : " + msg);

        //sendBroadcast(myFilteredResponse);
        common.log("오늘걸음수 : " + count);

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
                StepValue.Step = 0;
                count = 0;
            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

