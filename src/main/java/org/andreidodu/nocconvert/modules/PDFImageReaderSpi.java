package org.andreidodu.nocconvert.modules;

import org.andreidodu.nocconvert.modules.pdf.reader.PDFImageReader;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;

public class PDFImageReaderSpi extends ImageReaderSpi {

    public PDFImageReaderSpi() {
        super(
                "PDFSpi",
                "1.0",
                new String[]{"pdf"},
                new String[]{"pdf"},
                new String[]{"application/pdf"},
                "PDFImageReader",
                new Class[]{ImageInputStream.class, File.class, InputStream.class},
                null,
                false,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public boolean canDecodeInput(Object source) {
        try {
            if (source instanceof ImageInputStream iis) {
                iis.mark();
                byte[] header = new byte[4];
                iis.readFully(header);
                iis.reset();
                return header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            } else if (source instanceof File file) {
                return file.getName().toLowerCase().endsWith(".pdf");
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        return new PDFImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "PDF Image Reader";
    }
}
