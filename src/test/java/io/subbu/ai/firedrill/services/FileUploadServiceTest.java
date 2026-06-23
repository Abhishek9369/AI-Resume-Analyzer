package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.entities.ProcessTracker;
import io.subbu.ai.firedrill.models.JobPriority;
import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import io.subbu.ai.firedrill.models.ProcessStatus;
import io.subbu.ai.firedrill.repos.ProcessTrackerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private FileParserService fileParserService;

    @Mock
    private ResumeProcessingService resumeProcessingService;

    @Mock
    private ProcessTrackerRepository trackerRepository;

    @Mock
    private JobQueueService jobQueueService;

    @InjectMocks
    private FileUploadService fileUploadService;

    private UUID testTrackerId;

    @BeforeEach
    void setUp() {
        testTrackerId = UUID.randomUUID();
        
        // Set default properties
        ReflectionTestUtils.setField(fileUploadService, "uploadDirectory", "./uploads");
        ReflectionTestUtils.setField(fileUploadService, "allowedExtensions", ".doc,.docx,.pdf");
        ReflectionTestUtils.setField(fileUploadService, "schedulerEnabled", false);
    }

    // ==================== Single File Upload Tests ====================

    @Test
    void testHandleFileUpload_SchedulerMode_SingleFile_Success() throws IOException {
        // Arrange
        ReflectionTestUtils.setField(fileUploadService, "schedulerEnabled", true);
        
        MultipartFile file = createMockFile("resume.pdf", "PDF content");
        ProcessTracker savedTracker = createMockTracker(testTrackerId, "resume.pdf");
        
        JobQueue mockJobQueue = createMockJobQueue();
        
        when(fileParserService.isValidFileFormat("resume.pdf")).thenReturn(true);
        when(trackerRepository.save(any(ProcessTracker.class))).thenReturn(savedTracker);
        when(jobQueueService.createJob(any(), any(), any(), any(), any())).thenReturn(mockJobQueue);

        // Act
        UUID result = fileUploadService.handleFileUpload(file);

        // Assert
        assertEquals(testTrackerId, result);
        
        // Verify tracker creation (3 saves: initial, correlationId update, status update after job creation)
        verify(trackerRepository, times(3)).save(any(ProcessTracker.class));
        
        // Verify job creation (scheduler mode)
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService).createJob(
            eq(JobType.RESUME_PROCESSING),
            any(byte[].class),
            metadataCaptor.capture(),
            eq(JobPriority.NORMAL),
            anyString()
        );
        
        Map<String, Object> metadata = metadataCaptor.getValue();
        assertEquals("resume.pdf", metadata.get("filename"));
        assertEquals(testTrackerId.toString(), metadata.get("trackerId"));
        
        // Verify async processing NOT called
        verify(resumeProcessingService, never()).processSingleResume(any(), any(), any());
    }

    @Test
    void testHandleFileUpload_AsyncMode_SingleFile_Success() throws IOException {
        // Arrange
        ReflectionTestUtils.setField(fileUploadService, "schedulerEnabled", false);
        
        MultipartFile file = createMockFile("resume.docx", "DOCX content");
        ProcessTracker savedTracker = createMockTracker(testTrackerId, "resume.docx");
        
        when(fileParserService.isValidFileFormat("resume.docx")).thenReturn(true);
        when(trackerRepository.save(any(ProcessTracker.class))).thenReturn(savedTracker);

        // Act
        UUID result = fileUploadService.handleFileUpload(file);

        // Assert
        assertEquals(testTrackerId, result);
        
        // Verify tracker creation
        verify(trackerRepository, times(2)).save(any(ProcessTracker.class));
        
        // Verify async processing called
        verify(resumeProcessingService).processSingleResume(
            any(byte[].class),
            eq("resume.docx"),
            eq(testTrackerId)
        );
        
        // Verify job queue NOT used
        verify(jobQueueService, never()).createJob(any(), any(), any(), any(), any());
    }

    @Test
    void testHandleFileUpload_SchedulerMode_ZipFile_FallbackToAsync() throws IOException {
        // Arrange
        ReflectionTestUtils.setField(fileUploadService, "schedulerEnabled", true);
        
        MultipartFile file = createMockFile("resumes.zip", "ZIP content");
        ProcessTracker savedTracker = createMockTracker(testTrackerId, "resumes.zip");
        
        when(fileParserService.isValidFileFormat("resumes.zip")).thenReturn(true);
        when(trackerRepository.save(any(ProcessTracker.class))).thenReturn(savedTracker);

        // Act
        UUID result = fileUploadService.handleFileUpload(file);

        // Assert
        assertEquals(testTrackerId, result);
        
        // Verify async ZIP processing called (fallback)
        verify(resumeProcessingService).processZipFile(
            any(byte[].class),
            eq("resumes.zip"),
            eq(testTrackerId)
        );
        
        // Verify job queue NOT used for ZIP in scheduler mode
        verify(jobQueueService, never()).createJob(any(), any(), any(), any(), any());
    }

    @Test
    void testHandleFileUpload_AsyncMode_ZipFile_Success() throws IOException {
        // Arrange
        ReflectionTestUtils.setField(fileUploadService, "schedulerEnabled", false);
        
        MultipartFile file = createMockFile("batch.zip", "ZIP content");
        ProcessTracker savedTracker = createMockTracker(testTrackerId, "batch.zip");
        
        when(fileParserService.isValidFileFormat("batch.zip")).thenReturn(true);
        when(trackerRepository.save(any(ProcessTracker.class))).thenReturn(savedTracker);

        // Act
        UUID result = fileUploadService.handleFileUpload(file);

        // Assert
        assertEquals(testTrackerId, result);
        
        // Verify async ZIP processing called
        verify(resumeProcessingService).processZipFile(
            any(byte[].class),
            eq("batch.zip"),
            eq(testTrackerId)
        );
    }

    // ==================== Multiple File Upload Tests ====================

    @Test
    void testHandleMultipleFileUpload_SchedulerMode_Success() throws IOException {
        // Arrange
        ReflectionTestUtils.setField(fileUploadService, "schedulerEnabled", true);
        
        List<MultipartFile> files = Arrays.asList(
            createMockFile("resume1.pdf", "PDF1"),
            createMockFile("resume2.docx", "DOCX"),
            createMockFile("resume3.pdf", "PDF2")
        );
        
        ProcessTracker savedTracker = createMockTracker(testTrackerId, "3 files");
        
        JobQueue mockJobQueue = createMockJobQueue();
        
        when(fileParserService.isValidFileFormat(anyString())).thenReturn(true);
        when(trackerRepository.save(any(ProcessTracker.class))).thenReturn(savedTracker);
        when(jobQueueService.createJob(any(), any(), any(), any(), any())).thenReturn(mockJobQueue);

        // Act
        UUID result = fileUploadService.handleMultipleFileUpload(files);

        // Assert
        assertEquals(testTrackerId, result);
        
        // Verify 3 jobs created in queue
        verify(jobQueueService, times(3)).createJob(
            eq(JobType.RESUME_PROCESSING),
            any(byte[].class),
            any(),
            eq(JobPriority.NORMAL),
            anyString()
        );
        
        // Verify async processing NOT called
        verify(resumeProcessingService, never()).processMultipleResumes(any(), any(), any());
    }

    @Test
    void testHandleMultipleFileUpload_AsyncMode_Success() throws IOException {
        // Arrange
        ReflectionTestUtils.setField(fileUploadService, "schedulerEnabled", false);
        
        List<MultipartFile> files = Arrays.asList(
            createMockFile("resume1.pdf", "PDF1"),
            createMockFile("resume2.docx", "DOCX")
        );
        
        ProcessTracker savedTracker = createMockTracker(testTrackerId, "2 files");
        
        when(fileParserService.isValidFileFormat(anyString())).thenReturn(true);
        when(trackerRepository.save(any(ProcessTracker.class))).thenReturn(savedTracker);

        // Act
        UUID result = fileUploadService.handleMultipleFileUpload(files);

        // Assert
        assertEquals(testTrackerId, result);
        
        // Verify async processing called with 2 files
        verify(resumeProcessingService).processMultipleResumes(
            argThat(dataList -> dataList.size() == 2),
            argThat(names -> names.size() == 2 && names.contains("resume1.pdf")),
            eq(testTrackerId)
        );
        
        // Verify job queue NOT used
        verify(jobQueueService, never()).createJob(any(), any(), any(), any(), any());
    }

    @Test
    void testHandleMultipleFileUpload_EmptyList_ThrowsException() {
        // Arrange
        List<MultipartFile> emptyFiles = Collections.emptyList();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> fileUploadService.handleMultipleFileUpload(emptyFiles)
        );
        
        assertEquals("No files provided", ex.getMessage());
        verify(trackerRepository, never()).save(any());
    }

    // ==================== Validation Tests ====================

    @Test
    void testHandleFileUpload_EmptyFile_ThrowsException() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> fileUploadService.handleFileUpload(file)
        );
        
        assertEquals("File is empty", ex.getMessage());
    }

    @Test
    void testHandleFileUpload_NoFilename_ThrowsException() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> fileUploadService.handleFileUpload(file)
        );
        
        assertEquals("Filename is required", ex.getMessage());
    }

    @Test
    void testHandleFileUpload_InvalidFormat_ThrowsException() {
        // Arrange
        MultipartFile file = createMockFile("document.txt", "TXT content");
        when(fileParserService.isValidFileFormat("document.txt")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> fileUploadService.handleFileUpload(file)
        );
        
        assertTrue(ex.getMessage().contains("Unsupported file format"));
    }

    @Test
    void testHandleFileUpload_FileTooLarge_ThrowsException() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("large.pdf");
        when(file.getSize()).thenReturn(60L * 1024 * 1024); // 60 MB
        when(fileParserService.isValidFileFormat("large.pdf")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> fileUploadService.handleFileUpload(file)
        );
        
        assertTrue(ex.getMessage().contains("exceeds maximum allowed size"));
    }

    // ==================== Get Process Status Tests ====================

    @Test
    void testGetProcessStatus_Found() {
        // Arrange
        ProcessTracker tracker = createMockTracker(testTrackerId, "test.pdf");
        when(trackerRepository.findById(testTrackerId)).thenReturn(Optional.of(tracker));

        // Act
        ProcessTracker result = fileUploadService.getProcessStatus(testTrackerId);

        // Assert
        assertNotNull(result);
        assertEquals(testTrackerId, result.getId());
        verify(trackerRepository).findById(testTrackerId);
    }

    @Test
    void testGetProcessStatus_NotFound_ThrowsException() {
        // Arrange
        when(trackerRepository.findById(testTrackerId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> fileUploadService.getProcessStatus(testTrackerId)
        );
        
        assertTrue(ex.getMessage().contains("Tracker not found"));
    }

    // ==================== Helper Methods ====================

    private MultipartFile createMockFile(String filename, String content) {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.isEmpty()).thenReturn(false);
        lenient().when(file.getOriginalFilename()).thenReturn(filename);
        lenient().when(file.getSize()).thenReturn((long) content.length());
        try {
            lenient().when(file.getBytes()).thenReturn(content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
    
    private JobQueue createMockJobQueue() {
        return JobQueue.builder()
                .id(UUID.randomUUID())
                .jobType(JobType.RESUME_PROCESSING)
                .status(JobStatus.PENDING)
                .priority(JobPriority.NORMAL.getValue())
                .build();
    }

    private ProcessTracker createMockTracker(UUID id, String filename) {
        ProcessTracker tracker = ProcessTracker.builder()
                .id(id)
                .status(ProcessStatus.INITIATED)
                .uploadedFilename(filename)
                .totalFiles(1)
                .processedFiles(0)
                .failedFiles(0)
                .build();
        tracker.setCorrelationId("test-correlation-" + id);
        return tracker;
    }
}
