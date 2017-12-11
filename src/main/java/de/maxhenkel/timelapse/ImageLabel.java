package de.maxhenkel.timelapse;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageLabel extends JPanel {

    private BufferedImage image;
    private JLabel label;

    public ImageLabel() {
        setBorder(null);
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());
        this.label=new JLabel("", SwingConstants.CENTER);
        label.setFont(new Font(label.getFont().getName(), Font.PLAIN, 24));
        label.setForeground(Color.WHITE);
        add(label, BorderLayout.SOUTH);
    }

    public void setLabel(String txt){
        this.label.setText(txt);
    }

    public void setImage(File file) throws IOException {
        setImage(ImageIO.read(file));
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(image==null){
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int w = getWidth();
        int h = getHeight();
        int iw = image.getWidth();
        int ih = image.getHeight();
        double xScale = (double) w / iw;
        double yScale = (double) h / ih;
        double scale = Math.min(xScale, yScale);
        int width = (int) (scale * iw);
        int height = (int) (scale * ih);
        int x = (w - width) / 2;
        int y = (h - height) / 2;
        g2.drawImage(image, x, y, width, height, this);
    }

}
