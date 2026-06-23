# Upload UI State Management Fix

## Problem Summary

The file upload screen was getting stuck showing "Resume Processing Status" when an upload failed, with no way for users to start a new upload. This created a dead-end state that required a page refresh to recover.

## Root Cause

The issue was in [FileUpload.tsx](../src/main/frontend/src/pages/FileUpload/FileUpload.tsx):

1. **Conditional Rendering Logic (Line 95)**: The component showed the "Resume Processing Status" view whenever `tracker` existed, regardless of status.

2. **Button Visibility Logic (Original Line 128)**: The "Upload More Resumes" button **only** appeared when `tracker.status === 'COMPLETED'`.

3. **Result**: When uploads failed (`status === 'FAILED'`), the component:
   - Showed the processing status view
   - Did NOT show the "Upload More Resumes" button
   - Left users with no way to clear the tracker and start a new upload

## Fixes Applied

### 1. Primary Fix: Show Reset Button for FAILED Status

**File**: [src/main/frontend/src/pages/FileUpload/FileUpload.tsx](../src/main/frontend/src/pages/FileUpload/FileUpload.tsx#L132)

**Change**: Modified the button visibility condition to include FAILED status:

```typescript
// BEFORE (Line 128)
{tracker.status === 'COMPLETED' && (
  <button className={styles.newUploadButton} onClick={handleNewUpload}>
    Upload More Resumes
  </button>
)}

// AFTER (Line 132)
{(tracker.status === 'COMPLETED' || tracker.status === 'FAILED') && (
  <button className={styles.newUploadButton} onClick={handleNewUpload}>
    Upload More Resumes
  </button>
)}
```

**Impact**: Users can now reset and start new uploads even when previous uploads fail.

### 2. Field Name Alignment Fix

**Problem**: Backend uses `message` field, but frontend expected `errorMessage`, causing error messages not to display.

**Files Changed**:
- [src/main/frontend/src/store/slices/uploadSlice.ts](../src/main/frontend/src/store/slices/uploadSlice.ts#L11)
- [src/main/frontend/src/services/api.ts](../src/main/frontend/src/services/api.ts#L25)
- [src/main/frontend/src/pages/FileUpload/FileUpload.tsx](../src/main/frontend/src/pages/FileUpload/FileUpload.tsx#L127)

**Changes**:
```typescript
// ProcessTracker interface - renamed errorMessage → message
export interface ProcessTracker {
  id: string
  status: 'INITIATED' | 'EMBED_GENERATED' | 'VECTOR_DB_UPDATED' | 'RESUME_ANALYZED' | 'COMPLETED' | 'FAILED'
  totalFiles: number
  processedFiles: number
  failedFiles: number
  startTime: string
  endTime?: string
  message?: string  // ← Changed from errorMessage
}

// FileUpload.tsx - updated reference
{tracker.message && (  // ← Changed from tracker.errorMessage
  <p style={{ color: '#f56565', marginTop: '1rem' }}>
    {tracker.message}
  </p>
)}
```

**Impact**: Error messages from failed uploads now display correctly in the UI.

### 3. Cleanup: Remove Unused Import

**File**: [src/main/java/io/subbu/ai/firedrill/services/FileUploadService.java](../src/main/java/io/subbu/ai/firedrill/services/FileUploadService.java)

**Change**: Removed unused `@Transactional` import (functionality was removed in earlier fix)

## Testing

### Automated Test Setup

Run [test-failed-tracker.ps1](../test-failed-tracker.ps1) to:
1. Create a FAILED tracker in the database
2. Verify it's accessible via API
3. Provide manual UI verification steps

### Manual Verification Steps

1. **Navigate to**: https://localhost/upload

2. **Trigger a failed upload** (or use test script):
   - Upload an invalid file type (e.g., .txt file)
   - Or use the test script to inject a FAILED tracker

3. **Expected UI Behavior**:
   ```
   Resume Processing Status
   ┌─────────────────────────────────────┐
   │ Status: FAILED                      │
   │ Progress: 60% (3/5 files)           │
   │ Failed: 2 files                     │
   │                                     │
   │ Error: Processing failed: 2 files   │
   │        had invalid formats          │
   │                                     │
   │ [Upload More Resumes]  ← BUTTON!    │
   └─────────────────────────────────────┘
   ```

4. **Click "Upload More Resumes"**:
   - Tracker state clears from Redux
   - UI returns to upload form
   - Users can start a new upload

## Related Issues

### Previous Fix: Async Transaction Boundary
- **Issue**: "Tracker not found" error in async processing
- **Fix**: Removed `@Transactional` from `handleMultipleFileUpload` (commit earlier in session)
- **Link**: [FileUploadService.java](../src/main/java/io/subbu/ai/firedrill/services/FileUploadService.java#L60)

## Database Schema Reference

```sql
-- process_tracker table structure
Table "public.process_tracker"
      Column       |              Type              
-------------------+--------------------------------
 id                | uuid                          NOT NULL
 status            | character varying(255)        NOT NULL
 total_files       | integer                       
 processed_files   | integer                       
 failed_files      | integer                       
 message           | text                          -- ← Note: "message" not "error_message"
 uploaded_filename | character varying(255)        
 created_at        | timestamp(6) without time zone
 updated_at        | timestamp(6) without time zone
 completed_at      | timestamp(6) without time zone

-- Status constraint
CHECK (status IN ('INITIATED', 'EMBED_GENERATED', 'VECTOR_DB_UPDATED', 
                  'RESUME_ANALYZED', 'COMPLETED', 'FAILED'))
```

## Files Modified

1. ✅ [src/main/frontend/src/pages/FileUpload/FileUpload.tsx](../src/main/frontend/src/pages/FileUpload/FileUpload.tsx)
2. ✅ [src/main/frontend/src/store/slices/uploadSlice.ts](../src/main/frontend/src/store/slices/uploadSlice.ts)
3. ✅ [src/main/frontend/src/services/api.ts](../src/main/frontend/src/services/api.ts)
4. ✅ [src/main/java/io/subbu/ai/firedrill/services/FileUploadService.java](../src/main/java/io/subbu/ai/firedrill/services/FileUploadService.java)

## Build & Deployment

```powershell
# Rebuild and restart the application
cd docker
docker-compose down
docker-compose up --build -d

# Verify containers are running
docker ps

# Check application logs
docker logs resume-analyzer-app --tail 50
```

## Status

✅ **FIXED** - Upload UI now properly handles FAILED status with reset button
✅ **DEPLOYED** - Changes built and running in Docker container
⏳ **PENDING** - Manual UI verification recommended

---

**Date**: 2026-02-16  
**Session**: Upload state management debug session
