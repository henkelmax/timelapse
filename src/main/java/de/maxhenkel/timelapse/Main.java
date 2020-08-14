package de.maxhenkel.timelapse;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import de.maxhenkel.henkellib.args.Arguments;
import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.config.PropertyConfiguration;
import de.maxhenkel.henkellib.io.InputHandler;
import de.maxhenkel.henkellib.io.InputStreamInputHandler;
import de.maxhenkel.henkellib.logging.Log;
import de.maxhenkel.timelapse.telegram.TelegramBotAPI;
import org.apache.commons.lang3.SystemUtils;

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
     * --private (false by default)
     * --save-images (save images true by default)
     */
    public static void main(String[] args) throws IOException, SQLException {
        if (SystemUtils.IS_OS_LINUX) {
            Webcam.setDriver(new V4l4jDriver());
        }

        Arguments arguments = new Arguments(args);

        if (arguments.getBooleanValue("debug-log", false)) {
            Log.setLogLevel(Log.LogLevel.DEBUG);
        }

        printArguments();

        String configPath = arguments.getValue("config-location", "config.properties");

        Configuration config = new PropertyConfiguration(configPath);

        File outputFolder = new File(config.getString("output_folder", new File("timelapse/").getPath()));

        if (arguments.hasKey("convert")) {
            int frameRate = arguments.getIntValue("frame-rate", 30);
            Log.i("Converting timelapse...");
            VideoConverter.convert(frameRate, new File("timelapse-" + System.currentTimeMillis() + ".mp4"), outputFolder.listFiles());
            Log.i("Timelapse converted");
            return;
        }

        TimelapseEngine timelapseEngine = new TimelapseEngine(config, outputFolder, arguments.getBooleanValue("save-images", true));

        timelapseEngine.printInfo();

        TimelapseThread thread = new TimelapseThread(timelapseEngine);

        TelegramBotAPI t = null;
        if (arguments.getBooleanValue("telegram-bot", true)) {
            t = new TelegramBotAPI(config, timelapseEngine, arguments.getBooleanValue("private", false));
        }
        TelegramBotAPI telegramBotAPI = t;

        InputHandler inputHandler = new InputStreamInputHandler();

        TimelapseFrame frame = null;
        if (arguments.getBooleanValue("frame", true)) {
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
        inputHandler.registerCommands((s, strings) -> {

            inputHandler.stop();

            if (finalFrame != null) {
                finalFrame.dispose();
            }

            if (telegramBotAPI != null) {
                telegramBotAPI.stop();
            }
            thread.stopTimelapse();
            try {
                thread.join(10000); // Wait max 10 seconds for thread to stop
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
        }, "stop", "exit");

        inputHandler.start();
        thread.start();
    }

    public static void printArguments() {
        Log.i("POSSIBLE ARGUMENTS");
        Log.i("--config-location [path]");
        Log.i("--telegram-bot [true/false]");
        Log.i("--save-images [true/false]");
        Log.i("--debug-log [true/false]");
        Log.i("--frame [true/false]");
        Log.i("--private [true/false]");
        Log.i("");
        Log.i("--convert");
        Log.i("--frame-rate [fps]");
        Log.i("");
    }

}
