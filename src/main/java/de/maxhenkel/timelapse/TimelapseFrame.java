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

    public TimelapseFrame(final TimelapseEngine timelapseEngine, final TimelapseThread timelapseThread, TelegramBotAPI telegramBotAPI, Configuration config) {
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
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        this.imageLabel = new ImageLabel();
        this.add(imageLabel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                telegramBotAPI.stop();
                timelapseThread.stopTimelapse();
                try {
                    timelapseThread.join(10000);//Wait max 10 seconds for thread to stop
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
    }

    public void onImage(BufferedImage image, long time) {
        imageLabel.setImage(image);
        imageLabel.setLabel(TimeFormatter.format(simpleDateFormat, time));
    }
}
