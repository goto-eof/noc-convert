package org.andreidodu.nocconvert.mapper;

import org.andreidodu.nocconvert.dto.FormatExtensionDTO;

import java.util.List;

public class FormatExtensionMapper {

    private static final List<FormatExtensionDTO> formatExtensionSTOList = List.of(
            new FormatExtensionDTO("bmp", "bmp"),
            new FormatExtensionDTO("wbmp", "wbmp"),
            new FormatExtensionDTO("png", "png"),
            new FormatExtensionDTO("pnm", "pnm"),
            new FormatExtensionDTO("bigtiff", "tiff"),
            new FormatExtensionDTO("pict", "pct"),
            new FormatExtensionDTO("tif", "tif"),
            new FormatExtensionDTO("ico", "ico"),
            new FormatExtensionDTO("pfm", "pfm"),
            new FormatExtensionDTO("pbm", "pbm"),
            new FormatExtensionDTO("pam", "pam"),
            new FormatExtensionDTO("psb", "psb"),
            new FormatExtensionDTO("psd", "psd"),
            new FormatExtensionDTO("targa", "tga"),
            new FormatExtensionDTO("gif", "gif"),
            new FormatExtensionDTO("webp", "webp"),
            new FormatExtensionDTO("pgm", "pgm"),
            new FormatExtensionDTO("ppm", "ppm"),
            new FormatExtensionDTO("iff", "iff"),
            new FormatExtensionDTO("jpeg", "jpg"),
            new FormatExtensionDTO("icns", "icns"),
            new FormatExtensionDTO("cur", "cur"),
            new FormatExtensionDTO("dcx", "dcx"),
            new FormatExtensionDTO("pcx", "pcx"),
            new FormatExtensionDTO("sgi", "sgi"),
            new FormatExtensionDTO("dds", "dds"),
            new FormatExtensionDTO("hdr", "hdr"),
            new FormatExtensionDTO("rgbe", "hdr"),
            new FormatExtensionDTO("xwd", "xwd"),
            new FormatExtensionDTO("thumbs db", "thumbs.db")

    );

    public static String getExtension(String format) {
        return formatExtensionSTOList.stream()
                .filter(dto -> dto.format().equalsIgnoreCase(format))
                .findFirst()
                .map(FormatExtensionDTO::extension)
                .orElseGet(() -> format);
    }


}
