package com.example.aravindh.demo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;


import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor accSensor;
    private Sensor magSensor;
    private Sensor gyroSensor;

    private WifiManager wifiManager;
    WiFiScanReceiver wifiReceiver;
    private TextView textAcc,textGyr,textMag,textView;
    private EditText xCoordinate, yCoordinate;
    private Button button;

    ConnectionFactory factory = new ConnectionFactory();
    private BlockingDeque queue = new LinkedBlockingDeque();
    Thread subscribeThread;
    Thread publishThread;

    private float mAccel=1;
    private float timeStamp;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float d;
    float[] distance = new float[3];
    float[] velocity = new float[3];

    int[] rssiList = new int[10];
    int[] cnt = new int[10];
    String time;

    //mac addresses of access points in the ground floor front wing of AB3
    String[] macList = {
            "44:31:92:AF:A4:B0",
            "44:31:92:B0:16:D0",
            "44:31:92:B0:10:90",
            "44:31:92:9A:44:D0",
            "44:31:92:AF:BC:90"
    };

    //co ordinates of access points in the ground floor front wing of AB3
    //double[][] positions = new double[][] { { 22.5,11.5 }, { 10.4,26.8}, { 23.5,24.5 }, { 22.5,41.5 } };
    double[][] positions = new double[][] {  { 11,28.4} , { 26.6,41.5 }, { 24.5,27.6 },  { 41.5,28.4} ,  { 41.5,88.4} };
    //double[][] positions = new double[][] {  { 52.4,119.5} , { 23.5,24.5 }, { 12.5,11.5 },  { 41.5,28.4}  };
    //double[][] positions = new double[][] { { 12.5,20.5 }, { 100.4,26.8}, { 23.5,10.5 }, { 12.5,51.5 } };
    double[] distances = new double[] { 0, 0, 0, 0,0 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        button = (Button)findViewById(R.id.button);

        textAcc = (TextView)findViewById(R.id.textAcc);
        textGyr = (TextView)findViewById(R.id.textGyr);
        textMag = (TextView)findViewById(R.id.textMag);

        xCoordinate = (EditText)findViewById(R.id.xCoordinate);
        yCoordinate = (EditText) findViewById(R.id.yCoordinate);


        setupConnectionFactory();


        //Async task to get the application permissions to location and storage write
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        //Check if location is turned on and notify the user to turn on location
        LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!gpsEnabled)
        {
            Toast.makeText(this,"Please TURN ON location service to get data!",Toast.LENGTH_SHORT).show();
        }
        //define the sensor manager and the corresponding sensor variables
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //register the sensors
        mSensorManager.registerListener( this,accSensor,mSensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener( this,magSensor,mSensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener( this,gyroSensor,mSensorManager.SENSOR_DELAY_NORMAL);

        //wifi service enabling
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WiFiScanReceiver();
        int t =1;
        while(t <= 1) {




            // publishToAMQP();


            final Handler incomingMessageHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    String message = msg.getData().getString("msg");

                    Date now = new Date();
                    SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss");
                    //tv.append(ft.format(now) + ' ' + message + '\n');
                    Toast.makeText(getApplicationContext(),ft.format(now) + "test" + ' ' + message + '\n',Toast.LENGTH_SHORT).show();
                }
            };
            subscribe(incomingMessageHandler);

            scan();
            t++;
        }

    }
    public void onClickBtn(View v)
    {
        button.setBackgroundColor(Color.LTGRAY);
        if (xCoordinate.equals(null) && yCoordinate.equals(null))
        {
            Toast.makeText(getApplication(),"Enter the co-ordinates to begin SCAN",Toast.LENGTH_SHORT).show();
        }
        else
        {



            // scan();
        }
    }



    public void scan(){

        IntentFilter filterScanResult = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        IntentFilter filterRSSIChange = new IntentFilter(WifiManager.RSSI_CHANGED_ACTION);
        IntentFilter filterChange = new IntentFilter(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE);
        if(!wifiManager.isWifiEnabled())
        {
            Toast.makeText(this,"Wifi Turned On",Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }
        Toast.makeText(this,"Wifi Scan started",Toast.LENGTH_SHORT).show();
        this.registerReceiver(wifiReceiver, filterScanResult);
        this.registerReceiver(wifiReceiver, filterRSSIChange);
        this.registerReceiver(wifiReceiver, filterChange);
        wifiManager.startScan();

        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {
                    unregisterReceiver(wifiReceiver);
                    button.setBackgroundColor(Color.GREEN);
                    String msg="";
                    textView.append("The rssi list before avg is "+Arrays.toString(rssiList));

                    for(int i = 0;i<macList.length;i++){
                        if(cnt[i] != 0)
                        {
                            rssiList[i] = rssiList[i]/cnt[i];
                            msg = msg + String.valueOf(rssiList[i])+",";
                        }
                        else
                        {
                            msg = msg + String.valueOf(0)+";";
                        }
                    }
                    textView.append("The rssi list is "+Arrays.toString(rssiList));
                    try {
                        queue.putLast(msg);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (int i=0;i<macList.length;i++)
                    {
                        distances[i] = Math.pow(10,(rssiList[i]+38.667)/-16);
                    }
                    textView.append("The distance list is "+Arrays.toString(distances));
                    //multilateration
                    NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                    LeastSquaresOptimizer.Optimum optimum = solver.solve();

                    // the computed centroid
                    double[] centroid = optimum.getPoint().toArray();

                    WifiInfo info = wifiManager.getConnectionInfo();
                    String macaddress = info.getMacAddress();








                    //bwWifi.append("\n");
                    //bwWifi.close();
                    //plotOnMap((float)(centroid[0]*10),(float)(centroid[1]*10));

                    Toast.makeText(MainActivity.this,centroid[0]*10+" "+centroid[1]*10,Toast.LENGTH_SHORT).show();
                    if(centroid[0]*10 <0 || centroid[0]*10 >540 ||centroid[1]*10 <0 || centroid[1]*10 >1195)
                    {
                        plotOnMap(10,20);
                    }
                    else
                    {
                        plotOnMap((float)(centroid[0]*10),(float)(centroid[1]*10));
                    }

                    Date now = new Date();
                    SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
                    time = ft.format(now);

                    String body =getMacAddr()+","+xCoordinate.getText().toString()+"hi,"+yCoordinate.getText().toString()+","+(249336-((centroid[0]*10)*127.076))+","+(807286-((centroid[1]*10)*129.290))+','+time+","+msg;
                    Toast.makeText(MainActivity.this,body,Toast.LENGTH_SHORT).show();
                    try {
                        queue.putLast(body);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Arrays.fill(rssiList,new Integer(0));
                    Arrays.fill(cnt,new Integer(0));
                    Arrays.fill(distances,new Integer(0));
                    Toast.makeText(MainActivity.this,"5 seconds scan results done",Toast.LENGTH_SHORT).show();
                    scan();
                    publishToAMQP();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }


            }

        }, 5000);


    }



    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    // res1.append(Integer.toHexString(b & 0xFF) + ":");
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
    }

    private void plotOnMap(float x, float y){
        BitmapFactory.Options myOptions = new BitmapFactory.Options();
        myOptions.inDither = true;
        myOptions.inScaled = false;
        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important
        myOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cse,myOptions);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        Paint paintAP = new Paint();
        paintAP.setAntiAlias(true);
        paintAP.setColor(Color.BLACK);

        Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(mutableBitmap);
        canvas.drawCircle(x, y, 15, paint);
        for(int i = 0; i<macList.length;i++)
        {
            canvas.drawCircle(((float)positions[i][0]*10),((float) positions[i][1]*10), 5, paintAP);

        }
        ImageView imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setAdjustViewBounds(true);
        imageView.setImageBitmap(mutableBitmap);
    }

    @Override
    public void onSensorChanged(SensorEvent event){


        switch(event.sensor.getType()){

            case Sensor.TYPE_LINEAR_ACCELERATION:
                velocity[0] = 0;
                distance[0] = 0;
                velocity[1] = 0;
                distance[1] = 0;
                velocity[2] = 0;
                distance[2] = 0;
                float x,y,z;

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                /*
                final float dT = (event.timestamp - timeStamp) * NS2S;
                float mAccelCurrent = (float)(Math.sqrt(x*x + y*y + z*z));
                mAccel = mAccel * 0.9f + mAccelCurrent * 0.1f;
                d = mAccel * dT*dT/2;
                */
                velocity[0] = velocity[0]+ x;
                distance[0] = distance[0] + velocity[0];
                velocity[1] = velocity[1]+ x;
                distance[1] = distance[1] + velocity[1];
                velocity[2] = velocity[2]+ x;
                distance[2] = distance[2] + velocity[2];


//                textAcc.setText("Linear Acceleration: X = "+String.valueOf(x)+" Y = "+String.valueOf(y)+" Z = "+String.valueOf(z)
//                        //+"\n"+String.valueOf(mAccelCurrent)+"\nAcceleration"+String.valueOf(mAccel)+"\nDistance"+String.valueOf(d));
//                        +"\nVelocity: "+velocity[0]+"\nDistance: "+distance[0]);
//
                break;

            case Sensor.TYPE_GYROSCOPE:
                //textGyr.setText("GYROSCOPE: X = "+String.valueOf( event.values[0])+" Y = "+String.valueOf( event.values[1])+" Z = "+String.valueOf( event.values[2])+"\n");
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                //textMag.setText("MAGNETOMETER: X = "+String.valueOf( event.values[0])+" Y = "+String.valueOf( event.values[1])+" Z = "+String.valueOf( event.values[2])+"\n");
                break;

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }



    class WiFiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) || WifiManager.RSSI_CHANGED_ACTION.equals(action)|| WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE.equals(action))
            {
                List<ScanResult> wifiScanResultList = wifiManager.getScanResults();
                System.out.print(wifiScanResultList.toString());

                for(int i = 0; i < wifiScanResultList.size(); i++){
                    ScanResult accessPoint = wifiScanResultList.get(i);
                    for(int j = 0;j<macList.length;j++)
                    {
                        if(macList[j].equalsIgnoreCase(accessPoint.BSSID))
                        {
                            cnt[j] = cnt[j] + 1;
                            rssiList[j] = rssiList[j] + accessPoint.level;
                        }
                    }
                    //String listItem = "SSID: "+accessPoint.SSID + "\n" + "MAC Address: "+accessPoint.BSSID + "\n" + "RSSI Signal Level"+accessPoint.level;
                    // textView.append(listItem + "\n\n");

                }
                //textView.append("***********************************\n");
            }
            textView.append("\n From on receive rssi list is "+Arrays.toString(rssiList));
            textView.append("\n From on receive count list is "+Arrays.toString(cnt));
        }
    }

    private void setupConnectionFactory() {
        String uri = "amqp://amuda:amuda2017@172.17.137.160:5672/%2f";
        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUsername("amuda");
            factory.setPassword("amuda2017");
            factory.setHost("172.17.137.160");
            factory.setPort(5672);
            factory.setVirtualHost("amudavhost");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void publishToAMQP(){
        Log.i("","Reached publish to AMQP");
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Connection connection = factory.newConnection();
                        Channel ch = connection.createChannel();
                        //ch.queueDeclare("PQ", false, false, false, null);

                        //ch.queueDeclare("a",true,true,false,null);
                        ch.confirmSelect();
                        Log.i("","Reached publish to AMQP");

                        while(true) {

                            String message = queue.takeFirst().toString();
                            Log.i("",message+" is the message to be published");
                            try{
                                //ch.basicPublish("", "PQ", null, message.getBytes());
                                ch.basicPublish("amq.fanout","severity" , null, message.getBytes());
                                ch.waitForConfirmsOrDie();
                            } catch (Exception e){
                                queue.putFirst(message);
                                throw e;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.d("", "Connection broken: " + e.getClass().getName());
                        try {
                            Thread.sleep(1000); //sleep and then try again
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
            }
        });
        publishThread.start();
    }

    void subscribe(final Handler handler){
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Connection connection = factory.newConnection();
                        Channel channel = connection.createChannel();
                        channel.basicQos(1);
                        AMQP.Queue.DeclareOk q = channel.queueDeclare();
                        //channel.queueDeclare("a",true,true,false,null);
                        channel.queueBind(q.getQueue(), "amq.fanout", "severity");
                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        channel.basicConsume(q.getQueue(), true, consumer);

                        while (true) {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                            String message = new String(delivery.getBody());
                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString("msg", message);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e1) {
                        Log.d("", "Connection broken: " + e1.getClass().getName());
                        try {

                            Thread.sleep(1000); //sleep and then try again
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });
        subscribeThread.start();
    }
/*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        publishThread.interrupt();
        subscribeThread.interrupt();
    }
*/

}
