package de.maxhenkel.timelapse;

import com.github.sarxos.webcam.Webcam;
import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.logging.Log;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TimelapseEngine {

    private Webcam webcam;
    private Configuration config;
    private long delay;
    private File outputFolder;
    private int width, height;
    private float compression;
    private SimpleDateFormat simpleDateFormat;
    private TimelapseListener listener;

    public TimelapseEngine(Configuration config) {
        this.config = config;
        this.simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        this.width = config.getInt("image_width", 1920);
        this.height = config.getInt("image_height", 1080);
        this.compression=config.getFloat("compression", 1.0F);
        setWebcam(Webcam.getWebcamByName(config.getString("webcam", "")));
        if (webcam == null) {
            setWebcam(Webcam.getDefault());
        }
        delay = config.getLong("delay", 60000);
        this.outputFolder = new File(config.getString("output_folder", new File("timelapse/").getPath()));
    }

    public void setTimelapseListener(TimelapseListener listener){
        this.listener=listener;
    }

    public void printInfo() {
        List<Webcam> webcams = Webcam.getWebcams();

        Log.i("Available Webcams:");

        for (Webcam webcam : webcams) {
            Log.i(webcam.getDevice().getName());
        }

        if (webcams.isEmpty()) {
            Log.i("No webcams");
            return;
        }

        if (webcam == null) {
            return;
        }

        Log.i("");

        Log.i("Selected Webcam");

        Log.i(webcam.getName());

        Log.i("");

        Log.i("View sizes:");

        for (Dimension dim : webcam.getViewSizes()) {
            Log.i((int) dim.getWidth() + "x" + (int) dim.getHeight());
        }
        Log.i("");

        Log.i("Custom view sizes");

        for (Dimension dim : webcam.getCustomViewSizes()) {
            Log.i((int) dim.getWidth() + "x" + (int) dim.getHeight());
        }
        Log.i("");
    }

    public void setResolution(int width, int height) {
        this.width = width;
        this.height = height;
        config.putInt("image_width", width);
        config.putInt("image_height", height);

        if (webcam != null) {
            webcam.setCustomViewSizes(new Dimension[]{new Dimension(width, height)});
            webcam.setViewSize(new Dimension(width, height));
        }
    }

    public void setCompression(float compression){
        this.compression=compression;
        config.putFloat("compression", compression);
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
        config.putLong("delay", delay);
    }

    public List<Webcam> getWebcams() {
        return Webcam.getWebcams();
    }

    public Webcam getWebcam() {
        return webcam;
    }

    public void setWebcam(Webcam webcam) {
        if(webcam!=null){
            webcam.close();
        }
        this.webcam = webcam;
        config.putString("webcam", webcam.getName());
        setResolution(width, height);
    }

    public void takePicture() throws IOException {
        if (webcam == null) {
            Log.e("Cant take picture. No Webcam");
            return;
        }

        if(!outputFolder.exists()){
            outputFolder.mkdirs();
        }

        if(!outputFolder.isDirectory()){
            Log.e("Output Folder is no Directory");
            return;
        }

        if(!webcam.isOpen()){
            if (!webcam.open()) {
                Log.e("Could not open Webcam");
                return;
            }
        }

        BufferedImage bi=webcam.getImage();
        Date time=Calendar.getInstance().getTime();

        if(bi==null){
            Log.e("Failed to capture Image");
            return;
        }

        int i=0;

        File image;

        while (true){
            image=new File(outputFolder, generateFileName(i, "jpg", time));
            if(!image.exists()){
                break;
            }
            i++;
        }

        //ImageIO.write(webcam.getImage(), "png", image);
        saveImage(bi, image, compression);

        if(listener!=null){
            listener.onImage(bi, time);
        }
    }

    private String generateFileNameDate(Date date) {
        return simpleDateFormat.format(date);
    }

    private String generateFileName(int i, String fileEnding, Date date) {
        return generateFileNameDate(date) + (i <= 0 ? "" : "-" +i) + "." +fileEnding;
    }

    public static void saveImage(BufferedImage image, File file, float compression) throws IOException {
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(compression);

        ImageOutputStream outputStream = new FileImageOutputStream(file);
        jpgWriter.setOutput(outputStream);
        IIOImage outputImage = new IIOImage(image, null, null);
        jpgWriter.write(null, outputImage, jpgWriteParam);
        jpgWriter.dispose();
        outputStream.close();
    }

    public void close(){
        if(webcam!=null){
            webcam.close();
        }
    }

    public static interface TimelapseListener{
        public void onImage(BufferedImage image, Date time);
    }

}
