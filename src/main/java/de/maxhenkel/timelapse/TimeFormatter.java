package de.maxhenkel.timelapse;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimeFormatter {

    private static Calendar calendar=Calendar.getInstance();

    public static String format(SimpleDateFormat format, long time){
        calendar.setTimeInMillis(time);
        return format.format(calendar.getTime());
    }

}
