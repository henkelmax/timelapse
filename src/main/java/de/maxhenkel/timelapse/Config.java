package de.maxhenkel.timelapse;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.ConfigEntry;

public class Config {

    public final ConfigEntry<String> webcam;
    public final ConfigEntry<String> apiToken;
    public final ConfigEntry<String> fileDateFormat;
    public final ConfigEntry<String> telegramDateFormat;
    public final ConfigEntry<String> frameDateFormat;
    public final ConfigEntry<Integer> imageWidth;
    public final ConfigEntry<Integer> imageHeight;
    public final ConfigEntry<Double> compression;
    public final ConfigEntry<Long> delay;
    public final ConfigEntry<Long> adminUserId;
    public final ConfigEntry<Long> maxMessageDelay;

    public Config(ConfigBuilder builder) {
        fileDateFormat = builder.stringEntry("file_date_format", "yyyy-MM-dd-HH-mm-ss");
        telegramDateFormat = builder.stringEntry("telegram_date_format", "dd.MM.yyyy HH:mm:ss");
        frameDateFormat = builder.stringEntry("frame_date_format", "dd.MM.yyyy HH:mm:ss");
        apiToken = builder.stringEntry("api_token", "");
        imageWidth = builder.integerEntry("image_width", 1920, 1, Integer.MAX_VALUE);
        imageHeight = builder.integerEntry("image_height", 1080, 1, Integer.MAX_VALUE);
        compression = builder.doubleEntry("compression", 1D, 0D, 1D);
        webcam = builder.stringEntry("webcam", "");
        delay = builder.longEntry("delay", 60000, 0, Long.MAX_VALUE);
        adminUserId = builder.longEntry("admin_user_id", -1, -1, Long.MAX_VALUE);
        maxMessageDelay = builder.longEntry("max_message_delay", 60000, 0, Long.MAX_VALUE);
    }

}
