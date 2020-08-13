package de.maxhenkel.timelapse;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import de.maxhenkel.henkellib.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class VideoConverter {

    public static void convert(int frameRate, File output, File[] images) throws IOException {
        if (frameRate <= 0) {
            frameRate = 1;
        }

        long delay = TimeUnit.SECONDS.toNanos(1) / frameRate;
        delay = Math.max(delay, 1);

        IMediaWriter writer = ToolFactory.makeWriter(output.getAbsolutePath());

        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, 1280, 720);

        long time = 0;
        for (File file : images) {
            if (!FileUtils.getFileExtension(file).equalsIgnoreCase("jpg")) {
                continue;
            }
            BufferedImage screen = ImageIO.read(file);
            writer.encodeVideo(0, screen, time, TimeUnit.NANOSECONDS);
            time += delay;
        }

        writer.close();
    }

}
