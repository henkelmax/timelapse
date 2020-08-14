package de.maxhenkel.timelapse;

import com.github.sarxos.webcam.Webcam;
import de.maxhenkel.simpleconfig.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class TimelapseEngine {

    private static final Logger LOGGER = LogManager.getLogger();

    private Webcam webcam;
    private Configuration config;
    private long delay;
    private File outputFolder;
    private int width, height;
    private float compression;
    private SimpleDateFormat simpleDateFormat;
    private TimelapseListener listener;
    private byte[] lastImage;
    private long lastImageTime;
    private File lastImageFile;
    private boolean saveImages;

    public TimelapseEngine(Configuration config, File outputFolder, boolean saveImages) {
        this.saveImages = saveImages;
        this.config = config;
        this.outputFolder = outputFolder;
        String sdf = config.getString("file_date_format", "yyyy-MM-dd-HH-mm-ss");
        this.simpleDateFormat = new SimpleDateFormat(sdf);
        this.width = config.getInt("image_width", 1920);
        this.height = config.getInt("image_height", 1080);
        this.compression = config.getFloat("compression", 1.0F);
        setWebcam(Webcam.getWebcamByName(config.getString("webcam", "")));
        if (webcam == null) {
            setWebcam(Webcam.getDefault());
        }
        delay = config.getLong("delay", 60000);
    }

    public void setTimelapseListener(TimelapseListener listener) {
        this.listener = listener;
    }

    public void printInfo() {
        List<Webcam> webcams = Webcam.getWebcams();

        LOGGER.info("Available Webcams:");

        for (Webcam webcam : webcams) {
            LOGGER.info(webcam.getDevice().getName());
        }

        if (webcams.isEmpty()) {
            LOGGER.info("No webcams");
            return;
        }

        if (webcam == null) {
            return;
        }

        LOGGER.info("");

        LOGGER.info("Selected Webcam");

        LOGGER.info(webcam.getName());

        LOGGER.info("");

        LOGGER.info("View sizes:");

        for (Dimension dim : webcam.getViewSizes()) {
            LOGGER.info((int) dim.getWidth() + "x" + (int) dim.getHeight());
        }
        LOGGER.info("");

        LOGGER.info("Custom view sizes");

        for (Dimension dim : webcam.getCustomViewSizes()) {
            LOGGER.info((int) dim.getWidth() + "x" + (int) dim.getHeight());
        }
        LOGGER.info("");
    }

    public void setResolution(int width, int height) {
        this.width = width;
        this.height = height;
        config.put("image_width", width);
        config.put("image_height", height);

        if (webcam != null) {
            webcam.setCustomViewSizes(new Dimension[]{new Dimension(width, height)});
            webcam.setViewSize(new Dimension(width, height));
        }
    }

    public File getOutputFolder() {
        return outputFolder;
    }

    public byte[] getLastImage() {
        return lastImage;
    }

    public long getLastImageTime() {
        return lastImageTime;
    }

    public File getLastImageFile() {
        return lastImageFile;
    }

    public void setCompression(float compression) {
        this.compression = compression;
        config.put("compression", compression);
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
        config.put("delay", delay);
    }

    public List<Webcam> getWebcams() {
        return Webcam.getWebcams();
    }

    public Webcam getWebcam() {
        return webcam;
    }

    public void setWebcam(Webcam w) {
        if (webcam != null) {
            webcam.close();
        }
        this.webcam = w;
        if (webcam == null) {
            return;
        }
        config.put("webcam", webcam.getName());
        setResolution(width, height);

    }

    public void takePicture() throws IOException {
        if (webcam == null) {
            LOGGER.error("Cant take picture. No Webcam");
            return;
        }

        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        if (!outputFolder.isDirectory()) {
            LOGGER.error("Output Folder is no Directory");
            return;
        }

        if (!webcam.isOpen()) {
            if (!webcam.open()) {
                LOGGER.error("Could not open Webcam");
                return;
            }
        }

        BufferedImage bi = webcam.getImage();
        long time = System.currentTimeMillis();

        if (bi == null) {
            LOGGER.error("Failed to capture Image");
            return;
        }

        lastImage = getImage(bi, compression);
        lastImageTime = time;

        int i = 0;

        if (saveImages) {
            File imageFile;

            while (true) {
                imageFile = new File(outputFolder, generateFileName(i, "jpg", time));
                if (!imageFile.exists()) {
                    break;
                }
                i++;
            }

            save(lastImage, imageFile);

            lastImageFile = imageFile;
        }

        if (listener != null) {
            listener.onImage(bi, time);
        }
    }

    private String generateFileName(int i, String fileEnding, long time) {
        return Main.format(simpleDateFormat, time) + (i <= 0 ? "" : "-" + i) + "." + fileEnding;
    }

    public static byte[] getImage(BufferedImage image, float compression) throws IOException {
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(compression);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageOutputStream outputStream = new MemoryCacheImageOutputStream(byteArrayOutputStream);

        jpgWriter.setOutput(outputStream);
        IIOImage outputImage = new IIOImage(image, null, null);
        jpgWriter.write(null, outputImage, jpgWriteParam);
        jpgWriter.dispose();
        outputStream.close();

        byteArrayOutputStream.flush();
        byte[] data = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        return data;
    }

    public static void save(byte[] image, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(image);
        fos.flush();
        fos.close();
    }

    public void close() {
        if (webcam != null) {
            webcam.close();
        }
    }

    public interface TimelapseListener {
        void onImage(BufferedImage image, long time);
    }

}
