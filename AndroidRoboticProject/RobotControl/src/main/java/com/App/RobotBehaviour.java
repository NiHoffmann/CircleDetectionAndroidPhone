/**
 @file   RobotBehaviour.java
 @author Nils Hoffmann
 @date   11.12.2022
 @brief
 **/
package com.App;

import android.util.Log;

import com.App.BallDetection.Circle;
import com.App.ORB.ORB;

public class RobotBehaviour {
    private ORB orb;
    private int speed1=0;
    private int speed2=0;

    public RobotBehaviour(ORB orb){
        this.orb = orb;
    }

    private void updateMotor(){
        orb.setMotor( 0, ORB.Mode.SPEED, speed1, 0);
        orb.setMotor( 1, ORB.Mode.SPEED, speed2, 0);
        Log.i("motor",""+speed1+speed2);
    }

    public void updateMotor(int speed1, int speed2){
        this.speed1 = -speed1;
        this.speed2 = speed2;
        updateMotor();
    }

    public void stop(){
        speed1=speed2=0;
        updateMotor();
    }

    public void followBall(Circle circle, int videoWidth, boolean trackingLost){

        Log.i("circle",""+Math.sqrt(circle.getRadius()));
        double vMax = 2000;
        //double forwardScale = 1.5f;
        double kS = 7500;
        double kD = 2000;
        double sollR = 170d;
        double sollX = 0.5d;

        double deltaR = ((sollR - Math.sqrt(circle.getRadius()))/videoWidth)*2.5;
        double deltaX = (double) (circle.getX())/videoWidth - sollX;


        double velocityForward = Math.max(Math.min(kS * deltaR, vMax), -vMax);
        double velocityTurning = Math.max(Math.min(kD * deltaX, vMax), -vMax);

        if(trackingLost) {
            velocityForward = 0;
        }

        int velocityRight = (int)(0.5d * (velocityForward + velocityTurning));
        int velocityLeft = (int)(0.5d * (velocityForward - velocityTurning));

        updateMotor(-velocityRight,velocityLeft);
    }
}
