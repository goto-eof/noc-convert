package org.andreidodu.nocconvert.modules.pdf.writer;

import org.andreidodu.nocconvert.modules.PDFImageWriterSpi;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFImageWriterImageIO extends ImageWriter {

    private PDFImageWriter pdfWriter;
    private final PDFImageWriterSpi pdfImageWriterSpi;
    private final List<BufferedImage> images;

    public PDFImageWriterImageIO(PDFImageWriterSpi originatingProvider) {
        super(originatingProvider);
        images = new ArrayList<>();
        pdfWriter = new PDFImageWriter(originatingProvider);
        pdfImageWriterSpi = originatingProvider;
    }

    @Override
    public void setOutput(Object output) {
        super.setOutput(output);
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        return null;
    }

    // Add a single image to the PDF
    public void write(BufferedImage image) throws IOException {
        pdfWriter.addImage(image);
    }

    @Override
    public void write(IIOImage image) throws IOException {
        if (image.getRenderedImage() instanceof BufferedImage bi) {
            write(bi);
        } else {
            throw new IOException("Unsupported image type for PDF writing");
        }
    }

    @Override
    public void write(IIOMetadata streamMetadata,
                      IIOImage image,
                      javax.imageio.ImageWriteParam param) throws IOException {
        write(image);
    }


    @Override
    public void writeToSequence(IIOImage image, javax.imageio.ImageWriteParam param) throws IOException {
        write(image);
    }

    @Override
    public void endWriteSequence() throws IOException {
        if (getOutput() instanceof ImageOutputStream ios) {
            pdfWriter.saveToStream(ios);
            pdfWriter.close();
        }
    }

    @Override
    public void reset() {
        images.clear();
        pdfWriter = new PDFImageWriter(pdfImageWriterSpi);
    }
}
