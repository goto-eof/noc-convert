package org.andreidodu.nocconvert.modules.pdf.writer;

import org.andreidodu.nocconvert.modules.PDFImageWriterSpi;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PDFImageWriter extends ImageWriter {

    private final PDDocument document;
    private final PDFImageWriterSpi pdfImageWriterSpi;

    public PDFImageWriter(PDFImageWriterSpi spi) {
        super(spi);
        pdfImageWriterSpi = spi;
        this.document = new PDDocument();
    }

    public void addImage(BufferedImage image) throws IOException {
        PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(LosslessFactory.createFromImage(document, image), 0, 0);
        }
    }

    public void addImages(List<BufferedImage> images) throws IOException {
        for (BufferedImage image : images) {
            addImage(image);
        }
    }

    public void save(File file) throws IOException {
        document.save(file);
    }

    public void save(ImageOutputStream ios) throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        BufferedImage input = ImageIO.read(ios);
        String output = tmpDir + File.separator + "test.pdf";
        document.save(writeToFile(input, output));
    }

    private File writeToFile(BufferedImage input, String output) throws IOException {


        File outputFile = new File(output);

        try (ImageOutputStream ios = new FileImageOutputStream(outputFile)) {
            PDFImageWriter writer = new PDFImageWriter(pdfImageWriterSpi);
            writer.addImage(input);
            writer.save(ios);
            writer.close();
        }

        return outputFile;
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

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {

    }

    @Override
    public void writeToSequence(IIOImage image, ImageWriteParam param) throws IOException {

    }

    public void close() throws IOException {
        document.close();
    }

    public void saveToStream(ImageOutputStream ios) throws IOException {
        save(ios);
    }
}

