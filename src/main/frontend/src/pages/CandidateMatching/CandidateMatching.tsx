import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { fetchCandidates } from '@/store/slices/candidatesSlice'
import { fetchJobs } from '@/store/slices/jobsSlice'
import {
  fetchMatchesForJob,
  matchAllCandidatesToJob,
  updateMatchStatus,
} from '@/store/slices/matchesSlice'
import { fetchExternalProfiles } from '@/store/slices/enrichmentSlice'
import { RootState } from '@/store'
import styles from './CandidateMatching.module.css'

const CandidateMatching = () => {
  const dispatch = useDispatch()
  const { candidates } = useSelector((state: RootState) => state.candidates)
  const { jobs } = useSelector((state: RootState) => state.jobs)
  const { matches, matchingInProgress, error } = useSelector((state: RootState) => state.matches)
  const { profilesByCandidateId } = useSelector((state: RootState) => state.enrichment)
  const [selectedJobId, setSelectedJobId] = useState<string>('')
  const [alreadyMatchingWarning, setAlreadyMatchingWarning] = useState(false)

  useEffect(() => {
    dispatch(fetchCandidates())
    dispatch(fetchJobs())
  }, [dispatch])

  useEffect(() => {
    if (selectedJobId) {
      dispatch(fetchMatchesForJob({ jobId: selectedJobId, limit: 50 }))
    }
  }, [selectedJobId, dispatch])

  // When matches load, pre-fetch external profiles for matched candidates
  useEffect(() => {
    if (matches.length > 0) {
      const uniqueCandidateIds = [...new Set(matches.map((m) => m.candidateId))]
      uniqueCandidateIds.forEach((candidateId) => {
        if (candidateId && !profilesByCandidateId[candidateId]) {
          dispatch(fetchExternalProfiles(candidateId))
        }
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [matches.length, dispatch])

  // Block browser tab close / page reload while matching is in progress
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (matchingInProgress) {
        e.preventDefault()
        e.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [matchingInProgress])

  const handleMatchAll = () => {
    if (!selectedJobId) return
    if (matchingInProgress) {
      setAlreadyMatchingWarning(true)
      setTimeout(() => setAlreadyMatchingWarning(false), 5000)
      return
    }
    dispatch(matchAllCandidatesToJob(selectedJobId))
  }

  const handleShortlist = (matchId: string, currentStatus: boolean) => {
    dispatch(updateMatchStatus({ matchId, isShortlisted: !currentStatus }))
  }

  const handleSelect = (matchId: string, currentStatus: boolean) => {
    dispatch(updateMatchStatus({ matchId, isSelected: !currentStatus }))
  }

  const getScoreColor = (score: number) => {
    if (score >= 80) return styles.excellent
    if (score >= 70) return styles.good
    if (score >= 50) return styles.average
    return styles.poor
  }

  const selectedJob = jobs.find((j) => j.id === selectedJobId)
  const matchesWithCandidates = matches.map((match) => ({
    ...match,
    candidate: candidates.find((c) => c.id === match.candidateId),
  }))

  return (
    <div className={styles.matching}>
      <h2>Candidate Matching</h2>

      {/* Full-screen loading overlay while matching */}
      {matchingInProgress && (
        <div className={styles.loadingOverlay}>
          <div className={styles.loadingCard}>
            <div className={styles.spinner}></div>
            <h3>Matching Candidates…</h3>
            <p>
              Our AI is evaluating each candidate against this job requirement.
              This may take a minute depending on the number of candidates.
            </p>
            <div className={styles.progressDots}>
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      )}

      {/* "Already in progress" inline warning */}
      {alreadyMatchingWarning && (
        <div className={styles.warningBanner} role="alert">
          ⚠️ A match run is already in progress. Please wait for it to finish
          before starting a new one.
        </div>
      )}

      {/* Error banner */}
      {error && !matchingInProgress && (
        <div className={styles.errorBanner} role="alert">
          ❌ Matching failed: {error}
        </div>
      )}

      <div className={styles.controls}>
        <div className={styles.jobSelect}>
          <label htmlFor="job-selector">Select Job Requirement:</label>
          <select
            id="job-selector"
            value={selectedJobId}
            onChange={(e) => setSelectedJobId(e.target.value)}
            className={styles.select}
            aria-label="Job requirement selector"
          >
            <option value="">Choose a job...</option>
            {jobs
              .filter((job) => job.isActive)
              .map((job) => (
                <option key={job.id} value={job.id}>
                  {job.title} ({job.minExperienceYears}-{job.maxExperienceYears} yrs)
                </option>
              ))}
          </select>
        </div>
        {selectedJobId && (
          <button
            onClick={handleMatchAll}
            className={`${styles.matchButton} ${matchingInProgress ? styles.matchButtonBusy : ''}`}
            disabled={matchingInProgress}
            title={matchingInProgress ? 'Match already in progress' : 'Run AI match for all candidates'}
            aria-busy={matchingInProgress}
          >
            {matchingInProgress ? (
              <span className={styles.matchButtonInner}>
                <span className={styles.buttonSpinner}></span>
                Matching in Progress…
              </span>
            ) : (
              '🎯 Match All Candidates'
            )}
          </button>
        )}
      </div>

      {selectedJob && (
        <div className={styles.jobInfo}>
          <h3>{selectedJob.title}</h3>
          <p>
            <strong>Experience:</strong> {selectedJob.minExperienceYears} -{' '}
            {selectedJob.maxExperienceYears} years
          </p>
          <p>
            <strong>Required Skills:</strong> {selectedJob.requiredSkills}
          </p>
        </div>
      )}

      {matchesWithCandidates.length === 0 && selectedJobId && !matchingInProgress && (
        <div className={styles.empty}>
          <p>No matches found. Click &quot;🎯 Match All Candidates&quot; to generate AI-powered matches.</p>
        </div>
      )}

      {matchesWithCandidates.length > 0 && (
        <div className={styles.matchList}>
          {matchesWithCandidates.map((match) => (
            <div key={match.id} className={styles.matchCard}>
              <div className={styles.matchHeader}>
                <div>
                  <h4>{match.candidate?.name || 'Unknown Candidate'}</h4>
                  <span className={styles.email}>{match.candidate?.email}</span>
                  {/* External profile badges */}
                  <div className={styles.profileBadges}>
                    {(profilesByCandidateId[match.candidateId] ?? [])
                      .filter((p) => p.status === 'SUCCESS' && p.profileUrl)
                      .map((p) => (
                        <a
                          key={p.id}
                          href={p.profileUrl!}
                          target="_blank"
                          rel="noopener noreferrer"
                          className={styles.profileBadge}
                          title={`View ${p.source} profile`}
                        >
                          {p.source === 'GITHUB' ? '🐙' : p.source === 'LINKEDIN' ? '💼' : '🌐'}{' '}
                          {p.source === 'GITHUB' ? 'GitHub' : p.source.replace('_', ' ')}
                        </a>
                      ))}
                  </div>
                </div>
                <div className={`${styles.score} ${getScoreColor(match.matchScore)}`}>
                  {match.matchScore}%
                </div>
              </div>

              <div className={styles.scoreBreakdown}>
                <div className={styles.scoreItem}>
                  <span>Skills</span>
                  <div className={styles.scoreBar}>
                    <div
                      className={styles.scoreFill}
                      style={{ width: `${match.skillsScore}%` }}
                    />
                  </div>
                  <span>{match.skillsScore}%</span>
                </div>
                <div className={styles.scoreItem}>
                  <span>Experience</span>
                  <div className={styles.scoreBar}>
                    <div
                      className={styles.scoreFill}
                      style={{ width: `${match.experienceScore}%` }}
                    />
                  </div>
                  <span>{match.experienceScore}%</span>
                </div>
                <div className={styles.scoreItem}>
                  <span>Education</span>
                  <div className={styles.scoreBar}>
                    <div
                      className={styles.scoreFill}
                      style={{ width: `${match.educationScore}%` }}
                    />
                  </div>
                  <span>{match.educationScore}%</span>
                </div>
                <div className={styles.scoreItem}>
                  <span>Domain</span>
                  <div className={styles.scoreBar}>
                    <div
                      className={styles.scoreFill}
                      style={{ width: `${match.domainScore}%` }}
                    />
                  </div>
                  <span>{match.domainScore}%</span>
                </div>
              </div>

              {match.explanation && (
                <div className={styles.explanation}>
                  <strong>AI Analysis:</strong>
                  <p>{match.explanation}</p>
                </div>
              )}

              <div className={styles.matchActions}>
                <button
                  className={match.isShortlisted ? styles.shortlisted : styles.shortlistButton}
                  onClick={() => handleShortlist(match.id, match.isShortlisted)}
                >
                  {match.isShortlisted ? '✓ Shortlisted' : 'Shortlist'}
                </button>
                <button
                  className={match.isSelected ? styles.selected : styles.selectButton}
                  onClick={() => handleSelect(match.id, match.isSelected)}
                >
                  {match.isSelected ? '✓ Selected' : 'Select'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default CandidateMatching
