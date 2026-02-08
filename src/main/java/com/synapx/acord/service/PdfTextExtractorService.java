package com.synapx.acord.service;

import com.synapx.acord.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfTextExtractorService {

    private static final String SCANNED_PDF_MESSAGE =
            "PDF appears to be scanned/image-only. OCR is not supported. Please upload a text-based PDF or use /claims/process-text.";

    private final int minTextLengthForNonScanned;

    public PdfTextExtractorService(@Value("${acord.pdf.minTextLengthForNonScanned:50}") int minTextLengthForNonScanned) {
        this.minTextLengthForNonScanned = minTextLengthForNonScanned;
    }

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file received. Send a non-empty PDF in multipart field 'file'.");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !"application/pdf".equalsIgnoreCase(contentType)) {
            throw new BadRequestException("Invalid file type. Only PDF files are supported.");
        }

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            String text = textStripper.getText(document);
            if (isScannedOrLowSignalText(text)) {
                throw new BadRequestException(SCANNED_PDF_MESSAGE);
            }
            return text;
        } catch (IOException ex) {
            throw new BadRequestException("Invalid PDF input. Please upload a valid PDF document.");
        }
    }

    private boolean isScannedOrLowSignalText(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() < minTextLengthForNonScanned;
    }
}
