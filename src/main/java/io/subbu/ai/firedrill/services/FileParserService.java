package io.subbu.ai.firedrill.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Service for parsing resume files in various formats.
 * Supports PDF, DOC, and DOCX formats.
 */
@Service
@Slf4j
public class FileParserService {

    /**
     * Extract text content from a resume file based on its extension.
     * 
     * @param fileData Binary content of the file
     * @param filename Original filename (used to determine file type)
     * @return Extracted text content
     * @throws IOException if file parsing fails
     */
    public String extractText(byte[] fileData, String filename) throws IOException {
        log.debug("Extracting text from file: {}", filename);
        
        String extension = getFileExtension(filename).toLowerCase();
        
        return switch (extension) {
            case ".pdf" -> extractFromPdf(fileData);
            case ".docx" -> extractFromDocx(fileData);
            case ".doc" -> extractFromDoc(fileData);
            default -> throw new IllegalArgumentException("Unsupported file format: " + extension);
        };
    }

    /**
     * Extract text from PDF file using Apache PDFBox.
     * 
     * @param fileData Binary PDF content
     * @return Extracted text
     * @throws IOException if PDF parsing fails
     */
    private String extractFromPdf(byte[] fileData) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
             PDDocument document = org.apache.pdfbox.Loader.loadPDF(fileData)) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            log.debug("Extracted {} characters from PDF", text.length());
            return text;
        }
    }

    /**
     * Extract text from DOCX file using Apache POI.
     * 
     * @param fileData Binary DOCX content
     * @return Extracted text
     * @throws IOException if DOCX parsing fails
     */
    private String extractFromDocx(byte[] fileData) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            StringBuilder text = new StringBuilder();
            
            for (XWPFParagraph paragraph : paragraphs) {
                text.append(paragraph.getText()).append("\n");
            }
            
            String result = text.toString();
            log.debug("Extracted {} characters from DOCX", result.length());
            return result;
        }
    }

    /**
     * Extract text from DOC file using Apache POI.
     * 
     * @param fileData Binary DOC content
     * @return Extracted text
     * @throws IOException if DOC parsing fails
     */
    private String extractFromDoc(byte[] fileData) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
             HWPFDocument document = new HWPFDocument(inputStream)) {
            
            WordExtractor extractor = new WordExtractor(document);
            String text = extractor.getText();
            
            log.debug("Extracted {} characters from DOC", text.length());
            return text;
        }
    }

    /**
     * Get file extension from filename.
     * 
     * @param filename The filename
     * @return File extension including the dot (e.g., ".pdf")
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * Validate if the file format is supported.
     * 
     * @param filename The filename to validate
     * @return true if the format is supported
     */
    public boolean isValidFileFormat(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return extension.equals(".pdf") || extension.equals(".docx") || extension.equals(".doc");
    }
}
