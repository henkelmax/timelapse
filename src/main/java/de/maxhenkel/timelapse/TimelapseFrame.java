package de.maxhenkel.timelapse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimelapseFrame extends JFrame implements TimelapseEngine.TimelapseListener {

    private TimelapseEngine timelapseEngine;
    private ImageLabel imageLabel;
    private SimpleDateFormat simpleDateFormat;

   /* private JMenuBar menuBar;
    private JMenu menuOptions;
    private JMenu itemWebcam;*/

    public TimelapseFrame(final TimelapseEngine timelapseEngine, final TimelapseThread timelapseThread) {
        this.timelapseEngine = timelapseEngine;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        setTitle("Timelapse");
        setSize(800, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        this.imageLabel = new ImageLabel();
        this.add(imageLabel, BorderLayout.CENTER);

        /*this.menuBar=new JMenuBar();

        menuOptions=new JMenu("Options");
        itemWebcam=new JMenu("webcam");

        itemWebcam.add()
        //itemWebcam.
        menuOptions.add(itemWebcam);
        menuBar.add(menuOptions);

        this.setJMenuBar(menuBar);*/

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                timelapseThread.stopTimelapse();
                try {
                    timelapseThread.join(10000);//Wait max 10 seconds for thread to stop
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                timelapseEngine.close();
            }
        });
    }

    public void onImage(BufferedImage image, Date date) {
        imageLabel.setImage(image);
        imageLabel.setLabel(simpleDateFormat.format(date));
    }
}
