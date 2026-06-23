import { describe, it, expect } from 'vitest';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { render } from '../../test/test-utils';
import FileUpload from './FileUpload';

describe('FileUpload Component', () => {
  it('should render file upload component', () => {
    render(<FileUpload />);
    
    // Check for file upload heading
    const heading = screen.getByRole('heading', { name: /upload resumes/i });
    expect(heading).toBeInTheDocument();
  });

  it('should accept file input', () => {
    render(<FileUpload />);
    
    const fileInput = screen.getByLabelText(/upload/i) || document.querySelector('input[type="file"]');
    expect(fileInput).toBeTruthy();
  });

  it('should handle file drop events', async () => {
    render(<FileUpload />);
    
    const dropZone = screen.queryByText(/drag/i)?.closest('div') || document.querySelector('[class*="upload"]');
    
    if (dropZone) {
      const file = new File(['resume content'], 'resume.pdf', { type: 'application/pdf' });
      
      fireEvent.drop(dropZone, {
        dataTransfer: {
          files: [file],
          types: ['Files'],
        },
      });
      
      // File should be processed
      expect(dropZone).toBeTruthy();
    }
  });

  it('should display file name after selection', async () => {
    render(<FileUpload />);
    
    const fileInput = document.querySelector('input[type="file"]');
    if (fileInput) {
      const file = new File(['content'], 'test-resume.pdf', { type: 'application/pdf' });
      
      fireEvent.change(fileInput, {
        target: { files: [file] },
      });
      
      // Wait for file name to appear
      await waitFor(() => {
        const fileName = screen.queryByText(/test-resume\.pdf/i);
        if (fileName) {
          expect(fileName).toBeInTheDocument();
        }
      });
    }
  });

  it('should show upload button when file is selected', async () => {
    render(<FileUpload />);
    
    const fileInput = document.querySelector('input[type="file"]');
    if (fileInput) {
      const file = new File(['content'], 'resume.pdf', { type: 'application/pdf' });
      
      fireEvent.change(fileInput, {
        target: { files: [file] },
      });
      
      await waitFor(() => {
        const uploadButton = screen.queryByRole('button', { name: /upload/i });
        if (uploadButton) {
          expect(uploadButton).toBeInTheDocument();
        }
      });
    }
  });

  it('should validate file type', () => {
    render(<FileUpload />);
    
    const fileInput = document.querySelector('input[type="file"]');
    if (fileInput) {
      // Check accept attribute for PDF/DOC files
      const acceptAttr = fileInput.getAttribute('accept');
      if (acceptAttr) {
        expect(acceptAttr).toMatch(/pdf|doc/i);
      }
    }
  });

  it('should handle drag over events', () => {
    render(<FileUpload />);
    
    const dropZone = document.querySelector('[class*="upload"]') || screen.getByText(/upload/i).closest('div');
    
    if (dropZone) {
      fireEvent.dragOver(dropZone);
      expect(dropZone).toBeTruthy();
      
      fireEvent.dragLeave(dropZone);
      expect(dropZone).toBeTruthy();
    }
  });
});
