package de.maxhenkel.timelapse;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;

public class TimelapseFrame extends JFrame implements TimelapseEngine.TimelapseListener {

    private ImageLabel imageLabel;
    private SimpleDateFormat simpleDateFormat;

    public TimelapseFrame() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        simpleDateFormat = new SimpleDateFormat(Main.CONFIG.frameDateFormat.get());

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
        imageLabel.setLabel(Main.format(simpleDateFormat, time));
    }

}
