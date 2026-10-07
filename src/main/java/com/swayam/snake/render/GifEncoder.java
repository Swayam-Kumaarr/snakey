package com.swayam.snake.render;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Minimal animated-GIF writer on top of the GIF plugin that ships with the
 * JDK ({@code javax.imageio}). There's no simple "write animated gif" call in
 * the standard library — you have to hand-build a bit of the GIF89a metadata
 * tree yourself (frame delay + the Netscape loop extension), which is what
 * this class does.
 */
public final class GifEncoder implements AutoCloseable {

    private final ImageWriter writer;
    private final ImageWriteParam params;
    private final IIOMetadata metadata;
    private final ImageOutputStream output;
    private boolean sequenceStarted = false;

    public GifEncoder(Path outputFile, int delayMs, boolean loopForever) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("gif");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No GIF writer available in this JDK");
        }
        writer = writers.next();
        params = writer.getDefaultWriteParam();

        ImageTypeSpecifier typeSpecifier =
                ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
        metadata = writer.getDefaultImageMetadata(typeSpecifier, params);
        configureMetadata(delayMs, loopForever);

        output = ImageIO.createImageOutputStream(outputFile.toFile());
        writer.setOutput(output);
    }

    private void configureMetadata(int delayMs, boolean loopForever) throws IOException {
        String formatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(formatName);

        IIOMetadataNode graphicControl = getOrCreateChild(root, "GraphicControlExtension");
        graphicControl.setAttribute("disposalMethod", "none");
        graphicControl.setAttribute("userInputFlag", "FALSE");
        graphicControl.setAttribute("transparentColorFlag", "FALSE");
        graphicControl.setAttribute("delayTime", Integer.toString(delayMs / 10)); // GIF delay unit = 1/100s
        graphicControl.setAttribute("transparentColorIndex", "0");

        if (loopForever) {
            IIOMetadataNode appExtensions = getOrCreateChild(root, "ApplicationExtensions");
            IIOMetadataNode appExtension = new IIOMetadataNode("ApplicationExtension");
            appExtension.setAttribute("applicationID", "NETSCAPE");
            appExtension.setAttribute("authenticationCode", "2.0");
            // sub-block: 1 (loop sub-block id) + 2 bytes little-endian loop count, 0 = forever
            appExtension.setUserObject(new byte[]{0x1, 0x0, 0x0});
            appExtensions.appendChild(appExtension);
        }

        metadata.setFromTree(formatName, root);
    }

    private static IIOMetadataNode getOrCreateChild(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode child = new IIOMetadataNode(name);
        root.appendChild(child);
        return child;
    }

    public void addFrame(BufferedImage frame) throws IOException {
        if (!sequenceStarted) {
            writer.prepareWriteSequence(null);
            sequenceStarted = true;
        }
        writer.writeToSequence(new IIOImage(frame, null, metadata), params);
    }

    @Override
    public void close() throws IOException {
        if (sequenceStarted) {
            writer.endWriteSequence();
        }
        output.close();
        writer.dispose();
    }
}
