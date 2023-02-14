//*******************************************************************
/**
 @file   MainActivity.java
 @author Thomas Breuer
 @ModifiedBy Nils Hoffmann
 @date   27.01.2022
 @brief
 **/

//*******************************************************************
package com.App;

//*******************************************************************
import static com.App.BallDetection.*;
import static com.App.R.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.App.Bluetooth.BluetoothDeviceListActivity;
import com.App.ORB.ORB;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Locale;

//*******************************************************************
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{
	//---------------------------------------------------------------
	private Menu        menuLocal;
	private ORB         orb;
    private final Handler     msgHandler;
	private RobotBehaviour robotBehaviour;
	private SpeechRecognition speechRecognition;
	private Circle lastBall = null;
	private boolean runObjectDetection = false;
	long lastDetection = 0;

	private final int ORB_REQUEST_CODE         = 0;
	private final int ORB_DATA_RECEIVED_MSG_ID = 999;

	//---------------------------------------------------------------
	private static final int SPEECH_REQUEST_CODE = 1;


	//---------------------------------------------------------------
	//---------------------------------------------------------------
	@SuppressLint("HandlerLeak")
	public MainActivity()
	{
        msgHandler = new Handler()
        {
            @Override
            public void handleMessage( Message msg )
            {
				if (msg.what == ORB_DATA_RECEIVED_MSG_ID) {
					setMsg();
				}
                super.handleMessage( msg );
            }
        };
    }

	//---------------------------------------------------------------
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);

		setContentView(layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(id.toolbar);
		setSupportActionBar(toolbar);

		//Initialize ORB
		orb     = new ORB();
		orb.init(this, msgHandler, ORB_DATA_RECEIVED_MSG_ID );
        orb.configMotor(0, 144, 50, 50, 30);
        orb.configMotor(1, 144, 50, 50, 30);

	    // Initialize OpenCV
        JavaCameraView mCamView = (JavaCameraView)findViewById( id.camera_view);
        mCamView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY);
        mCamView.setCvCameraViewListener(this);
        mCamView.enableView();

        OpenCVLoader.initDebug();


		// Initialize RobotBehaviour
		robotBehaviour = new RobotBehaviour(orb);

		//Initialize Speechrecognition
		speechRecognition = new  SpeechRecognition(this,robotBehaviour,75);
	}


    //-----------------------------------------------------------------
    @Override
    public void onDestroy()
    {
        orb.close();
        super.onDestroy();
    }

    //---------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		if (menuLocal == null)
		{
			menuLocal = menu;
			super.onCreateOptionsMenu(menu);
		}
		return true;
	}

	//---------------------------------------------------------------
	@Override
	public
	boolean onOptionsItemSelected( MenuItem item )
	{
		int id = item.getItemId();
		if (id == R.id.action_connect) {
			BluetoothDeviceListActivity.startBluetoothDeviceSelect(this, ORB_REQUEST_CODE);
		}
		return super.onOptionsItemSelected( item );
	}

	//-----------------------------------------------------------------
	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{

		super.onActivityResult( requestCode, resultCode, data );

		if (requestCode == ORB_REQUEST_CODE) {
			if (!orb.openBluetooth(BluetoothDeviceListActivity.onActivityResult(resultCode, data))) {
				Toast.makeText(getApplicationContext(), "Bluetooth not connected", Toast.LENGTH_LONG).show();
			}
		}
	}

	public void onClick_Speech(View view){
		speechRecognition.startSpeechRecognition();
	}

    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    public void onClick_Start( View view )
    {
        runObjectDetection = true;
    }

    //-----------------------------------------------------------------
    public void onClick_Stop( View view ) {
        runObjectDetection = false;
		robotBehaviour.stop();
    }

    //-----------------------------------------------------------------
	//-----------------------------------------------------------------

	private void setMsg()
	{
		TextView view;

		view = findViewById(id.msgVoltage);
		view.setText(String.format(Locale.GERMANY,"Batt: %.1f V", orb.getVcc()));

        view = findViewById(id.msgORB1);
        view.setText(String.format(Locale.GERMANY, "M0: %6d,%6d,%6d",orb.getMotorSpeed((byte)0),
                                                             orb.getMotorPos((byte)0),
                                                             orb.getMotorPwr((byte)0)) );

        view = findViewById(id.msgORB2);
        view.setText(String.format(Locale.GERMANY,"M1:%6d,%6d,%6d",orb.getMotorSpeed((byte)1),
                                                             orb.getMotorPos((byte)1),
                                                             orb.getMotorPwr((byte)1)) );
	}


	//OPEN CV (Default Project)
	//-----------------------------------------------------------------
	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
	{

		// Get frame
		Mat mImg = inputFrame.rgba();
		Mat hsv = new Mat();

		Imgproc.cvtColor(mImg,hsv,Imgproc.COLOR_BGR2HSV);

		//90-150 Farb bereich HSV Farbbereich Grün --->> hier durch BGR2HSV transformation Rot
		//eingrenz ung auf 100-130 für ein besseres ergebnis
		Core.inRange(hsv,new Scalar(100, 128, 128),new Scalar(130, 255, 255),hsv);

		Imgproc.medianBlur(hsv , hsv, 5);

		if (!runObjectDetection){
			return hsv;
		}

		int size = (int)(hsv.total()); //4*mImg.width()*mImg.height();
		byte[]  data = new byte[hsv.channels()*size];
		hsv.get(0,0,data);
		int videoWidth = hsv.cols();

		int[][] sortedCluster = findSortedCluster(data,videoWidth);
		Circle[] circles = findCircles(sortedCluster[0],sortedCluster[1], videoWidth, 512, 0.925d);
		int indexBall =  drawContestersAndDeterminMostLikely(circles, hsv);

		if(indexBall != -1) {
			lastDetection = System.currentTimeMillis();
			lastBall = circles[indexBall];
			robotBehaviour.followBall(circles[indexBall],videoWidth, false);
		}else{
			// We lost Tracking here
			if(lastBall != null){
				//i was following a ball before , keep tracking for 1 sek after tracking lost
				if(System.currentTimeMillis() < lastDetection){lastDetection = System.currentTimeMillis();}
					//if 1 sec passed , keep turning but don't drive forward
					if(System.currentTimeMillis()-lastDetection > 1000){
						robotBehaviour.followBall(lastBall, videoWidth, false);
					}
					robotBehaviour.followBall(lastBall, videoWidth, true);
			}
		}

		//this is in case the stop button is pressed while the data is being processed
		//this can only be reached if the detection is being canceled while data is processed
		if(!runObjectDetection) robotBehaviour.stop();

		return  hsv;
	}


	//-----------------------------------------------------------------
	@Override
	public void onCameraViewStarted(int width, int height)
	{
	}

	//-----------------------------------------------------------------
	@Override
	public void onCameraViewStopped()
	{
	}

}