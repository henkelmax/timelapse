package de.maxhenkel.timelapse;

import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.time.TimeFormatter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;

public class TimelapseFrame extends JFrame implements TimelapseEngine.TimelapseListener {

    private ImageLabel imageLabel;
    private SimpleDateFormat simpleDateFormat;

    public TimelapseFrame(Configuration config) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sdf=config.getString("frame_date_format", "dd.MM.yyyy HH:mm:ss");
        simpleDateFormat = new SimpleDateFormat(sdf);

        setTitle("Timelapse");
        setSize(800, 470);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        this.imageLabel = new ImageLabel();
        this.add(imageLabel, BorderLayout.CENTER);
    }

    public void onImage(BufferedImage image, long time) {
        imageLabel.setImage(image);
        imageLabel.setLabel(TimeFormatter.format(simpleDateFormat, time));
    }
}
