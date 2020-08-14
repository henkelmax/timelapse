package de.maxhenkel.timelapse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimelapseThread extends Thread {

    private static final Logger LOGGER = LogManager.getLogger();

    private TimelapseEngine timelapseEngine;
    private long lastImage;
    private boolean stopped;

    public TimelapseThread(TimelapseEngine timelapseEngine) {
        this.timelapseEngine = timelapseEngine;
        setName("TimelapseThread");
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (stopped) {
                    break;
                }
                lastImage = System.currentTimeMillis();
                timelapseEngine.takePicture();
            } catch (Throwable e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(calculateTime());
            } catch (InterruptedException e) {
            }
        }
        LOGGER.info("Timelapse Thread stopped");
    }

    private long calculateTime() {
        return Math.max(timelapseEngine.getDelay() - (System.currentTimeMillis() - lastImage), 0L);
    }

    public void stopTimelapse() {
        stopped = true;
        interrupt();
    }

}
