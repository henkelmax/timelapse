package de.maxhenkel.timelapse;

import de.maxhenkel.henkellib.config.PropertyConfiguration;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        //Log.setLogLevel(Log.LogLevel.ALL);
        TimelapseEngine timelapseEngine=new TimelapseEngine(new PropertyConfiguration("config.properties"));

        timelapseEngine.printInfo();

        TimelapseThread thread=new TimelapseThread(timelapseEngine);

        TimelapseFrame frame=new TimelapseFrame(timelapseEngine, thread);
        frame.setVisible(true);

        timelapseEngine.setTimelapseListener(frame);

        thread.start();
    }
}
