package de.maxhenkel.timelapse;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import de.maxhenkel.simpleconfig.Configuration;
import de.maxhenkel.simpleconfig.PropertyConfiguration;
import de.maxhenkel.timelapse.telegram.TelegramBotAPI;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) throws IOException, SQLException, ParseException {
        if (SystemUtils.IS_OS_LINUX) {
            Webcam.setDriver(new V4l4jDriver());
        }

        Options options = new Options();
        options.addOption(Option.builder("h").longOpt("help").desc("Displays possible arguments").build());
        options.addOption(Option.builder("c").longOpt("config-location").hasArg().argName("path").desc("The config path").build());
        options.addOption(Option.builder("d").longOpt("debug-log").desc("Enables debug logs").build());
        options.addOption(Option.builder("s").longOpt("save-images").hasArg().argName("true|false").desc("Save images").build());
        options.addOption(Option.builder("t").longOpt("telegram-bot").desc("Enables the telegram bot").build());
        options.addOption(Option.builder("p").longOpt("private").desc("Enables private mode").build());
        options.addOption(Option.builder("F").longOpt("frame").hasArg().argName("true|false").desc("Shows a frame with preview images").build());
        options.addOption(Option.builder("o").longOpt("output-folder").hasArg().argName("path").desc("The image output folder path").build());
        options.addOption(Option.builder("D").longOpt("database-path").hasArg().argName("path").desc("The database path for the telegram bot").build());

        options.addOption(Option.builder("C").longOpt("convert").desc("Start only the video converter").build());
        options.addOption(Option.builder("f").longOpt("frame-rate").hasArg().argName("fps").desc("The frame rate for the converter").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("d")) {
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.ALL);
        } else {
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.ERROR);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("timelapse [arguments]", options);
            return;
        }

        Configuration config = new PropertyConfiguration(new File(cmd.getOptionValue("c", "config.properties")));

        File outputFolder = new File(cmd.getOptionValue("o", "timelapse"));

        if (cmd.hasOption("C")) {
            int frameRate = Integer.parseInt(cmd.getOptionValue("f", String.valueOf(30)));
            LOGGER.info("Converting timelapse...");
            VideoConverter.convert(frameRate, new File("timelapse-" + System.currentTimeMillis() + ".mp4"), outputFolder.listFiles());
            LOGGER.info("Timelapse converted");
            return;
        }

        TimelapseEngine timelapseEngine = new TimelapseEngine(config, outputFolder, Boolean.parseBoolean(cmd.getOptionValue("s", String.valueOf(true))));

        timelapseEngine.printInfo();

        TimelapseThread thread = new TimelapseThread(timelapseEngine);

        if (cmd.hasOption("t")) {
            new TelegramBotAPI(config, timelapseEngine, cmd.getOptionValue("D", "database.db"), cmd.hasOption("p"));
        }

        TimelapseFrame frame;
        if (Boolean.parseBoolean(cmd.getOptionValue("F", String.valueOf(true)))) {
            frame = new TimelapseFrame(config);
            frame.setVisible(true);
            timelapseEngine.setTimelapseListener(frame);
        }

        thread.start();
    }

    public static String format(SimpleDateFormat format, long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return format.format(cal.getTime());
    }

}
