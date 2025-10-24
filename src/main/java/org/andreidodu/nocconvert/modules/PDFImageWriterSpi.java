package org.andreidodu.nocconvert.modules;


import org.andreidodu.nocconvert.modules.pdf.writer.PDFImageWriterImageIO;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import java.util.Locale;

public class PDFImageWriterSpi extends ImageWriterSpi {

    private static final String[] NAMES = {"pdf", "PDF"};
    private static final String[] SUFFIXES = {"pdf"};
    private static final String[] MIME_TYPES = {"application/pdf"};
    private static final String WRITER_CLASS_NAME = "org.andreidodu.nocconvert.modules.pdf.writer.PDFImageWriterImageIO";
    private static final String VENDOR_NAME = "andreidodu";
    private static final String VERSION = "1.0";

    public PDFImageWriterSpi() {
        super(VENDOR_NAME, VERSION, NAMES, SUFFIXES, MIME_TYPES,
                WRITER_CLASS_NAME, STANDARD_OUTPUT_TYPE, null, false, null, null, null, null, false, null, null, null, null);
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        return true;
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) {
        return new PDFImageWriterImageIO(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "PDF Image Writer";
    }
}

