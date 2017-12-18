package de.maxhenkel.timelapse;

import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.config.PropertyConfiguration;
import de.maxhenkel.henkellib.logging.Log;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws IOException, SQLException {
        Log.setLogLevel(Log.LogLevel.ALL);

        Configuration config=new PropertyConfiguration("config.properties");
        TimelapseEngine timelapseEngine=new TimelapseEngine(config);

        if(args.length>0&&args[0].equals("convert")){
            int frameRate=30;
            if(args.length>=2){
                try{
                    frameRate=Integer.parseInt(args[1]);
                }catch (NumberFormatException e){
                    e.printStackTrace();
                }
            }
            Log.i("Converting timelapse...");
            VideoConverter.convert(frameRate, new File("timelapse-" +System.currentTimeMillis() +".mp4"), timelapseEngine.getOutputFolder().listFiles());
            Log.i("Timelapse converted");
            return;
        }

        timelapseEngine.printInfo();

        TimelapseThread thread=new TimelapseThread(timelapseEngine);

        TelegramBotAPI telegramBotAPI=new TelegramBotAPI(config, timelapseEngine);

        TimelapseFrame frame=new TimelapseFrame(timelapseEngine, thread, telegramBotAPI, config);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                telegramBotAPI.stop();
                thread.stopTimelapse();
                try {
                    thread.join(10000);//Wait max 10 seconds for thread to stop
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                timelapseEngine.close();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                System.exit(0);
            }
        });
        frame.setVisible(true);

        timelapseEngine.setTimelapseListener(frame);

        thread.start();
    }
}
