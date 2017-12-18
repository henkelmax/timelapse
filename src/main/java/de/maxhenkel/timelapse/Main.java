package de.maxhenkel.timelapse;

import de.maxhenkel.henkellib.args.Arguments;
import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.config.PropertyConfiguration;
import de.maxhenkel.henkellib.io.InputHandler;
import de.maxhenkel.henkellib.io.InputStreamInputHandler;
import de.maxhenkel.henkellib.logging.Log;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class Main {

    /**
     * --debug-log (true if you want to see the debug logs)
     * --config-location (The config path)
     * --convert (Starts only the video converter)
     * --frame-rate (30 FPS by default. Only applies if convert argument is present)
     * --telegram-bot (true by default. If you want to enable the telegram bot)
     * --frame (true by default. If you want the frame to be present)
     */
    public static void main(String[] args) throws IOException, SQLException {
        Arguments arguments=new Arguments(args);

        if(arguments.getBooleanValue("debug-log", false)){
            Log.setLogLevel(Log.LogLevel.ALL);
        }

        String configPath=arguments.getValue("config-location", "config.properties");

        Configuration config=new PropertyConfiguration(configPath);
        TimelapseEngine timelapseEngine=new TimelapseEngine(config);

        if(arguments.hasKey("convert")){
            int frameRate=arguments.getIntValue("frame-rate", 30);
            Log.i("Converting timelapse...");
            VideoConverter.convert(frameRate, new File("timelapse-" +System.currentTimeMillis() +".mp4"), timelapseEngine.getOutputFolder().listFiles());
            Log.i("Timelapse converted");
            return;
        }

        timelapseEngine.printInfo();

        TimelapseThread thread=new TimelapseThread(timelapseEngine);

        TelegramBotAPI t=null;
        if(arguments.getBooleanValue("telegram-bot", true)){
            t=new TelegramBotAPI(config, timelapseEngine);
        }
        TelegramBotAPI telegramBotAPI = t;

        InputHandler inputHandler=new InputStreamInputHandler();

        TimelapseFrame frame=null;
        if(arguments.getBooleanValue("frame", true)){
            frame = new TimelapseFrame(config);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    inputHandler.getCommands().get("stop").onCommand("", new String[0]);
                }
            });
            frame.setVisible(true);

            timelapseEngine.setTimelapseListener(frame);
        }


        TimelapseFrame finalFrame = frame;
        inputHandler.registerCommands(new InputHandler.Command() {
            @Override
            public void onCommand(String s, String[] strings) {

                inputHandler.stop();

                if(finalFrame !=null){
                    finalFrame.dispose();
                }

                if(telegramBotAPI !=null){
                    telegramBotAPI.stop();
                }
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
        }, "stop", "exit");

        inputHandler.start();
        thread.start();
    }
}
