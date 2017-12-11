package de.maxhenkel.timelapse;

import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.config.PropertyConfiguration;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        //Log.setLogLevel(Log.LogLevel.ALL);

        Configuration config=new PropertyConfiguration("config.properties");
        TimelapseEngine timelapseEngine=new TimelapseEngine(config);

        timelapseEngine.printInfo();

        TimelapseThread thread=new TimelapseThread(timelapseEngine);

        TimelapseFrame frame=new TimelapseFrame(timelapseEngine, thread);
        frame.setVisible(true);

        timelapseEngine.setTimelapseListener(frame);

        thread.start();

        TelegramBotAPI telegramBotAPI=new TelegramBotAPI(config, timelapseEngine);
    }
}
