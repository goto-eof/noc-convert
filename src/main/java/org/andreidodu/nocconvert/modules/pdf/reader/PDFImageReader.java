package org.andreidodu.nocconvert.modules.pdf.reader;

import org.andreidodu.nocconvert.modules.PDFImageReaderSpi;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.IIOImage;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

public class PDFImageReader extends ImageReader {

    private PDDocument document;
    private PDFRenderer renderer;
    private ImageInputStream iis;
    private BufferedImage lastPage;
    private int lastIndex = -1;

    public PDFImageReader(PDFImageReaderSpi origin) {
        super(origin);
    }


    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        try {
            RandomAccessRead raf;

            if (input instanceof ImageInputStream stream) {
                this.iis = stream;
                File tmpPdf = copyToTempFile(iis, "pdfinput", ".pdf");
                raf = new RandomAccessReadBufferedFile(tmpPdf);
            } else if (input instanceof File file) {
                raf = new RandomAccessReadBufferedFile(file);
            } else if (input instanceof InputStream in) {
                File tmpPdf = copyToTempFile(iis, "pdfinput", ".pdf");
                raf = new RandomAccessReadBufferedFile(tmpPdf);
            } else {
                throw new IllegalArgumentException("Unsupported input type: " + input.getClass());
            }

            this.document = Loader.loadPDF(raf);
            this.renderer = new PDFRenderer(document);

        } catch (IOException e) {
            throw new RuntimeException("Cannot load PDF", e);
        }
    }

    public static File copyToTempFile(ImageInputStream iis, String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            iis.seek(0);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = iis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }

    private BufferedImage getPage(int index) throws IOException {
        if (index != lastIndex) {
            lastPage = renderer.renderImage(index);
            lastIndex = index;
        }
        return lastPage;
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        return document.getNumberOfPages();
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        return getPage(imageIndex).getWidth();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        return getPage(imageIndex).getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        return Collections.singletonList(new ImageTypeSpecifier(getPage(imageIndex))).iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) {
        return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        return getPage(imageIndex);
    }

    @Override
    public IIOImage readAll(int imageIndex, ImageReadParam param) throws IOException {
        BufferedImage image = getPage(imageIndex);
        return new IIOImage(image, null, null);
    }

    @Override
    public void dispose() {
        super.dispose();
        try {
            if (document != null) document.close();
            if (iis != null) iis.close();
        } catch (IOException ignored) {
        }
    }
}
