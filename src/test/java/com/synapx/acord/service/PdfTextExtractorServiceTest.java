package com.synapx.acord.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.synapx.acord.exception.BadRequestException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PdfTextExtractorServiceTest {

    private final PdfTextExtractorService pdfTextExtractorService = new PdfTextExtractorService(50);

    @Test
    void throwsScannedPdfErrorWhenExtractedTextIsTooShort() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "short.pdf",
                "application/pdf",
                createPdfBytesWithText("Too short"));

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> pdfTextExtractorService.extractText(file));

        assertThat(exception.getMessage()).isEqualTo(
                "PDF appears to be scanned/image-only. OCR is not supported. Please upload a text-based PDF or use /claims/process-text.");
    }

    @Test
    void throwsScannedPdfErrorWhenExtractedTextIsEmpty() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blank.pdf",
                "application/pdf",
                createBlankPdfBytes());

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> pdfTextExtractorService.extractText(file));

        assertThat(exception.getMessage()).isEqualTo(
                "PDF appears to be scanned/image-only. OCR is not supported. Please upload a text-based PDF or use /claims/process-text.");
    }

    @Test
    void throwsInvalidPdfErrorWhenPdfBoxLoadFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "broken.pdf",
                "application/pdf",
                "not-a-real-pdf".getBytes(StandardCharsets.UTF_8));

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> pdfTextExtractorService.extractText(file));

        assertThat(exception.getMessage()).isEqualTo("Invalid PDF input. Please upload a valid PDF document.");
    }

    @Test
    void extractsTextFromValidPdfEvenWhenFilenameIsNotPdf() throws IOException {
        String text = "This ACORD claim document contains enough textual signal for extraction to succeed.";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "claim.txt",
                "application/pdf",
                createPdfBytesWithText(text));

        String extracted = pdfTextExtractorService.extractText(file);

        assertThat(extracted).contains("ACORD claim document");
    }

    private byte[] createPdfBytesWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                document.save(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    private byte[] createBlankPdfBytes() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                document.save(outputStream);
                return outputStream.toByteArray();
            }
        }
    }
}
