/**
 @file   BallDetection.java
 @author Nils Hoffmann
 @date   11.12.2022
 @brief
 **/
package com.App;


import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;

public class BallDetection {
    public static int[][] findSortedCluster(byte[] imageData,int width){
        byte[] data = imageData.clone();
        int[] sortedCluster = new int[data.length];
        int[] volumeCluster = new int[1+data.length/2];

        int[] offsets = {1,width,
                -width, -1
                ,width-1,
                -1-width,
                1+width,
                1-width/*{0,0}*/};
        //queue umsetzen mit array pointer und % operator
        LinkedList<Integer> queue = new LinkedList<>();


        int clusterCount = -1;
        int posAbsolut = 0;

        //go through the whole data array
        for(int i = 0; i < data.length;i++) {
            int point = 0;

            //i found a red pixel that wasn't visited yet -> next cluster , start width search
            //visited pixel will be set to 0 after found
            if(((short) data[i] & 0xFF) > 0){
                queue.add(i);
                clusterCount++;

                data[i] = 0;
                sortedCluster[posAbsolut++] = i;
            }

            //this is our width search
            while(!queue.isEmpty()){
                int z = queue.pop();
                //search all pixels next to a pixel that was found to be red
                //offset array for ease of use
                for(int n=0;n<offsets.length;n++) {
                    int offset = z+offsets[n];
                    if(offset >= data.length || offset < 0) continue;

                    //did i find a red pixel -> add to queue, keep searching
                    if (((short) data[offset] & 0xFF) > 0) {
                        queue.add(offset);
                        data[offset] = 0;
                        point++;
                        sortedCluster[posAbsolut++] = offset;
                    }
                }
            }
            //did i find a cluster ? point will hold the amount of pixels for each cluster
            if(point > 0){
                //this is our cluster size
                volumeCluster[clusterCount] = point;
            }

        }

        return new int[][]{volumeCluster,sortedCluster};
    }

    public static Circle[] findCircles(int[]volumeCluster,int[] sortedCluster,int width,int minClusterSize, double threshold){
        Circle[] circles = new Circle[volumeCluster.length];
        //Kreis erkennung cluster aussortieren
        int posClusterOffset = 0;
        for(int i=0;i<volumeCluster.length;i++){
            if(volumeCluster[i] < minClusterSize){
                posClusterOffset += volumeCluster[i];
                continue;
            }

            long xsum = 0, ysum = 0;//, anzahl = 0;

            for(int z=0;z<volumeCluster[i];z++){
                int val = sortedCluster[posClusterOffset+z];
                //x durch rest
                xsum += val % width;
                //y durch ganz zahl division
                ysum += val / width;
            }

            int xCenter = (int) (xsum / (volumeCluster[i]));
            int yCenter = (int) (ysum / (volumeCluster[i]));

            double radius = volumeCluster[i] / Math.PI;


            long pointsInCircle = 0;
            //cound how many pixels are inside the perfect circle for given cluster size
            for(int z=0;z<volumeCluster[i];z++){
                int val = sortedCluster[posClusterOffset+z];
                int x = val % width;
                int y = val / width;
                //distance
                double distance = Math.pow(x-xCenter,2)+Math.pow(y-yCenter,2);
                if(distance <= radius) {
                    pointsInCircle += 1;
                }
            }

            double probability =  ((double)pointsInCircle)/((double) volumeCluster[i]);

            if(probability > threshold){
                circles[i] = new Circle(xCenter,yCenter,radius,probability);
            }

            posClusterOffset += volumeCluster[i];
        }

        return circles;
    }

    public static int drawContestersAndDeterminMostLikely(Circle[] circles, Mat hsv){
        if (circles == null || hsv == null) return -1;
        int indexBall = -1;
        double max = 0;
        //draw circles & find most circular object
        for(int i=0; i<circles.length;i++){
            if(circles[i] == null) continue;
            Imgproc.circle(hsv, new Point(circles[i].getX(), circles[i].getY()), (int) Math.sqrt(circles[i].getRadius()), new Scalar(128, 128, 128), 10);
            Imgproc.putText(hsv,""+circles[i].getProbability(),new Point(circles[i].getX(),circles[i].getY()), Core.FONT_ITALIC,1,new Scalar(128, 128, 128), 2);
            if(max < circles[i].getRadius()){
                max = circles[i].getRadius();
                indexBall = i;
            }
        }

        return indexBall;
    }

    public static class Circle{
        private int xPos;
        private int yPos;
        private double radius;
        private double probability;

        public Circle(int xPos,int yPos,double radius, double probability){
            this.xPos = xPos;
            this.yPos = yPos;
            this.radius = radius;
            this.probability = probability;
        }

        public int getX(){
            return xPos;
        }
        public int getY(){
            return yPos;
        }
        public double getRadius(){
            return radius;
        }
        public double getProbability(){return probability;}
    }
}
