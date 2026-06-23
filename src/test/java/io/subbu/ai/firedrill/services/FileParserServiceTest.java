package io.subbu.ai.firedrill.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for FileParserService.
 * Tests text extraction from PDF, DOCX, and DOC files.
 */
@DisplayName("FileParserService Unit Tests")
class FileParserServiceTest {

    private FileParserService fileParserService;

    @BeforeEach
    void setUp() {
        fileParserService = new FileParserService();
    }

    @Test
    @DisplayName("Should extract text from PDF successfully")
    void shouldExtractTextFromPdf() throws IOException {
        // Given
        String expectedText = "John Doe\nSenior Software Engineer\nExperience with Java and Spring Boot";
        byte[] pdfData = createMockPdfFile(expectedText);

        // When
        String extractedText = fileParserService.extractText(pdfData, "resume.pdf");

        // Then
        assertThat(extractedText).isNotNull();
        assertThat(extractedText).contains("John Doe");
        assertThat(extractedText).contains("Senior Software Engineer");
        assertThat(extractedText).contains("Spring Boot");
    }

    @Test
    @DisplayName("Should extract text from DOCX successfully")
    void shouldExtractTextFromDocx() throws IOException {
        // Given
        String[] paragraphs = {
                "John Doe",
                "Senior Software Engineer",
                "Skills: Java, Spring Boot, Kubernetes"
        };
        byte[] docxData = createMockDocxFile(paragraphs);

        // When
        String extractedText = fileParserService.extractText(docxData, "resume.docx");

        // Then
        assertThat(extractedText).isNotNull();
        assertThat(extractedText).contains("John Doe");
        assertThat(extractedText).contains("Java");
        assertThat(extractedText).contains("Spring Boot");
    }

    @Test
    @DisplayName("Should handle PDF with multiple pages")
    void shouldHandlePdfWithMultiplePages() throws IOException {
        // Given
        byte[] pdfData = createMultiPagePdfFile();

        // When
        String extractedText = fileParserService.extractText(pdfData, "multi-page-resume.pdf");

        // Then
        assertThat(extractedText).isNotNull();
        assertThat(extractedText).contains("Page 1");
        assertThat(extractedText).contains("Page 2");
    }

    @Test
    @DisplayName("Should handle empty PDF file")
    void shouldHandleEmptyPdf() throws IOException {
        // Given
        byte[] emptyPdfData = createEmptyPdfFile();

        // When
        String extractedText = fileParserService.extractText(emptyPdfData, "empty.pdf");

        // Then
        assertThat(extractedText).isNotNull();
        assertThat(extractedText.trim()).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty DOCX file")
    void shouldHandleEmptyDocx() throws IOException {
        // Given
        byte[] emptyDocxData = createMockDocxFile(new String[]{});

        // When
        String extractedText = fileParserService.extractText(emptyDocxData, "empty.docx");

        // Then
        assertThat(extractedText).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception for unsupported file format")
    void shouldThrowExceptionForUnsupportedFormat() {
        // Given
        byte[] textData = "Plain text content".getBytes();

        // When/Then
        assertThatThrownBy(() -> fileParserService.extractText(textData, "resume.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file format");
    }

    @Test
    @DisplayName("Should throw exception for invalid PDF data")
    void shouldThrowExceptionForInvalidPdfData() {
        // Given
        byte[] invalidData = "Not a valid PDF".getBytes();

        // When/Then
        assertThatThrownBy(() -> fileParserService.extractText(invalidData, "invalid.pdf"))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid DOCX data")
    void shouldThrowExceptionForInvalidDocxData() {
        // Given
        byte[] invalidData = "Not a valid DOCX".getBytes();

        // When/Then - Apache POI throws NotOfficeXmlFileException (not IOException)
        assertThatThrownBy(() -> fileParserService.extractText(invalidData, "invalid.docx"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("OOXML");
    }

    @Test
    @DisplayName("Should validate PDF format")
    void shouldValidatePdfFormat() {
        // When/Then
        assertThat(fileParserService.isValidFileFormat("resume.pdf")).isTrue();
    }

    @Test
    @DisplayName("Should validate DOCX format")
    void shouldValidateDocxFormat() {
        // When/Then
        assertThat(fileParserService.isValidFileFormat("resume.docx")).isTrue();
    }

    @Test
    @DisplayName("Should validate DOC format")
    void shouldValidateDocFormat() {
        // When/Then
        assertThat(fileParserService.isValidFileFormat("resume.doc")).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid file formats")
    void shouldRejectInvalidFormats() {
        // When/Then
        assertThat(fileParserService.isValidFileFormat("resume.txt")).isFalse();
        assertThat(fileParserService.isValidFileFormat("resume.rtf")).isFalse();
        assertThat(fileParserService.isValidFileFormat("resume.odt")).isFalse();
        assertThat(fileParserService.isValidFileFormat("resume")).isFalse();
    }

    @Test
    @DisplayName("Should handle filename without extension")
    void shouldHandleFilenameWithoutExtension() {
        // When/Then
        assertThat(fileParserService.isValidFileFormat("resume")).isFalse();
    }

    @Test
    @DisplayName("Should handle case-insensitive file extensions")
    void shouldHandleCaseInsensitiveExtensions() {
        // When/Then
        assertThat(fileParserService.isValidFileFormat("resume.PDF")).isTrue();
        assertThat(fileParserService.isValidFileFormat("resume.DOCX")).isTrue();
        assertThat(fileParserService.isValidFileFormat("resume.DOC")).isTrue();
    }

    @Test
    @DisplayName("Should extract text preserving line breaks")
    void shouldPreserveLineBreaks() throws IOException {
        // Given
        String[] paragraphs = {
                "Section 1",
                "",
                "Section 2",
                "Section 3"
        };
        byte[] docxData = createMockDocxFile(paragraphs);

        // When
        String extractedText = fileParserService.extractText(docxData, "resume.docx");

        // Then
        assertThat(extractedText).contains("\n");
        assertThat(extractedText).contains("Section 1");
        assertThat(extractedText).contains("Section 2");
    }

    @Test
    @DisplayName("Should handle large PDF files")
    void shouldHandleLargePdfFiles() throws IOException {
        // Given
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeContent.append("Line ").append(i).append(": This is a test line with some content. ");
        }
        byte[] largePdfData = createMockPdfFile(largeContent.toString());

        // When
        String extractedText = fileParserService.extractText(largePdfData, "large-resume.pdf");

        // Then
        assertThat(extractedText).isNotNull();
        assertThat(extractedText.length()).isGreaterThan(1000);
        assertThat(extractedText).contains("Line 0");
        assertThat(extractedText).contains("Line 99");
    }

    @Test
    @DisplayName("Should handle special characters in text")
    void shouldHandleSpecialCharacters() throws IOException {
        // Given
        String textWithSpecialChars = "John Döe\nEmail: john@test.com\nSkills: C++, C#, .NET";
        byte[] pdfData = createMockPdfFile(textWithSpecialChars);

        // When
        String extractedText = fileParserService.extractText(pdfData, "resume.pdf");

        // Then
        assertThat(extractedText).isNotNull();
        assertThat(extractedText).contains("@");
        assertThat(extractedText).contains(".");
    }

    // ==================== Helper Methods ====================

    /**
     * Create a mock PDF file with specified text content
     */
    private byte[] createMockPdfFile(String content) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);

                // Split content by newlines and write each line
                String[] lines = content.split("\n");
                for (String line : lines) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15);
                }

                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Create a multi-page PDF file
     */
    private byte[] createMultiPagePdfFile() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Page 1
            PDPage page1 = new PDPage();
            document.addPage(page1);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page1)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Page 1 Content");
                contentStream.endText();
            }

            // Page 2
            PDPage page2 = new PDPage();
            document.addPage(page2);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page2)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Page 2 Content");
                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Create an empty PDF file
     */
    private byte[] createEmptyPdfFile() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Create a mock DOCX file with specified paragraphs
     */
    private byte[] createMockDocxFile(String[] paragraphs) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            for (String paragraphText : paragraphs) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.createRun().setText(paragraphText);
            }

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
