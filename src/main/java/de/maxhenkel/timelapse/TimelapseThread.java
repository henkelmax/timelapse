package de.maxhenkel.timelapse;

import de.maxhenkel.henkellib.logging.Log;

public class TimelapseThread extends Thread{

    private TimelapseEngine timelapseEngine;
    private long lastImage;
    private boolean stopped;

    public TimelapseThread(TimelapseEngine timelapseEngine){
        this.timelapseEngine=timelapseEngine;
        setName("TimelapseThread");
    }

    @Override
    public void run() {
        while (true){
            try{
                if(stopped){
                    break;
                }

                lastImage=System.currentTimeMillis();

                //Log.d("Taking picture");
                timelapseEngine.takePicture();
            }catch (Throwable e){
                e.printStackTrace();
            }

            try{
                Thread.sleep(calculateTime());
            }catch (InterruptedException e){}
        }
        Log.i("Timelapse Thread stopped");
    }

    private long calculateTime(){
        return Math.max(timelapseEngine.getDelay() - (System.currentTimeMillis()-lastImage), 0L);
    }

    public void stopTimelapse(){
        stopped=true;
        interrupt();
    }
}
