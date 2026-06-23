import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import {
  fetchCandidates,
  searchCandidatesByName,
  searchCandidatesBySkill,
  deleteCandidate,
} from '@/store/slices/candidatesSlice'
import {
  fetchExternalProfiles,
  enrichProfile,
  enrichFromUrl,
  refreshProfile,
  type ExternalProfileSource,
  type CandidateExternalProfile,
} from '@/store/slices/enrichmentSlice'
import { RootState } from '@/store'
import FeedbackList from '@/components/FeedbackList/FeedbackList'
import FeedbackForm from '@/components/FeedbackForm/FeedbackForm'
import { EntityType } from '@/components/FeedbackForm/FeedbackForm'
import styles from './CandidateList.module.css'

const CandidateList = () => {
  const dispatch = useDispatch()
  const { candidates, loading } = useSelector((state: RootState) => state.candidates)
  const { profilesByCandidateId, enrichingCandidateId } = useSelector(
    (state: RootState) => state.enrichment
  )
  const [searchType, setSearchType] = useState<'all' | 'name' | 'skill'>('all')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCandidateId, setSelectedCandidateId] = useState<string | null>(null)
  const [showFeedbackModal, setShowFeedbackModal] = useState(false)
  const [showFeedbackForm, setShowFeedbackForm] = useState(false)
  const [feedbackRefreshTrigger, setFeedbackRefreshTrigger] = useState(0)
  const [expandedEnrichmentId, setExpandedEnrichmentId] = useState<string | null>(null)
  const [urlInputByCandidateId, setUrlInputByCandidateId] = useState<Record<string, string>>({})

  useEffect(() => {
    dispatch(fetchCandidates())
  }, [dispatch])

  const handleSearch = () => {
    if (searchType === 'all' || !searchQuery.trim()) {
      dispatch(fetchCandidates())
    } else if (searchType === 'name') {
      dispatch(searchCandidatesByName(searchQuery))
    } else if (searchType === 'skill') {
      dispatch(searchCandidatesBySkill(searchQuery))
    }
  }

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this candidate?')) {
      dispatch(deleteCandidate(id))
    }
  }

  const handleOpenFeedback = (candidateId: string) => {
    setSelectedCandidateId(candidateId)
    setShowFeedbackModal(true)
    setShowFeedbackForm(false)
  }

  const handleCloseFeedback = () => {
    setShowFeedbackModal(false)
    setSelectedCandidateId(null)
    setShowFeedbackForm(false)
  }

  const handleFeedbackSuccess = () => {
    setShowFeedbackForm(false)
    setFeedbackRefreshTrigger((prev) => prev + 1)
  }

  const handleToggleEnrichment = (candidateId: string) => {
    if (expandedEnrichmentId === candidateId) {
      setExpandedEnrichmentId(null)
      return
    }
    setExpandedEnrichmentId(candidateId)
    // Fetch profiles if not loaded yet
    if (!profilesByCandidateId[candidateId]) {
      dispatch(fetchExternalProfiles(candidateId))
    }
  }

  const handleEnrich = (candidateId: string, source: ExternalProfileSource) => {
    dispatch(enrichProfile({ candidateId, source }))
    if (expandedEnrichmentId !== candidateId) {
      setExpandedEnrichmentId(candidateId)
    }
  }

  const handleRefreshProfile = (profile: CandidateExternalProfile, candidateId: string) => {
    dispatch(refreshProfile({ profileId: profile.id, candidateId }))
  }

  const handleEnrichFromUrl = (candidateId: string) => {
    const url = (urlInputByCandidateId[candidateId] ?? '').trim()
    if (!url) return
    dispatch(enrichFromUrl({ candidateId, profileUrl: url }))
    setUrlInputByCandidateId((prev) => ({ ...prev, [candidateId]: '' }))
    if (expandedEnrichmentId !== candidateId) setExpandedEnrichmentId(candidateId)
  }

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'SUCCESS': return styles.statusSuccess
      case 'PENDING': return styles.statusPending
      case 'FAILED': return styles.statusFailed
      case 'NOT_FOUND': return styles.statusNotFound
      default: return styles.statusNotFound
    }
  }

  const getSourceIcon = (source: ExternalProfileSource) => {
    switch (source) {
      case 'GITHUB': return '🐙'
      case 'LINKEDIN': return '💼'
      case 'TWITTER': return '🐦'
      case 'INTERNET_SEARCH': return '🌐'
      default: return '🔗'
    }
  }

  return (
    <div className={styles.candidateList}>
      <div className={styles.header}>
        <h2>Candidates ({candidates.length})</h2>
      </div>

      <div className={styles.searchBar}>
        <select
          value={searchType}
          onChange={(e) => setSearchType(e.target.value as any)}
          className={styles.searchSelect}
          aria-label="Search type selector"
        >
          <option value="all">All Candidates</option>
          <option value="name">Search by Name</option>
          <option value="skill">Search by Skill</option>
        </select>
        {searchType !== 'all' && (
          <input
            type="text"
            placeholder={`Enter ${searchType}...`}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            className={styles.searchInput}
          />
        )}
        <button onClick={handleSearch} className={styles.searchButton} disabled={loading}>
          {loading ? 'Searching...' : 'Search'}
        </button>
      </div>

      {loading && <p className={styles.loading}>Loading candidates...</p>}

      {!loading && candidates.length === 0 && (
        <div className={styles.empty}>
          <p>No candidates found. Upload some resumes to get started!</p>
        </div>
      )}

      {!loading && candidates.length > 0 && (
        <div className={styles.grid}>
          {candidates.map((candidate) => (
            <div key={candidate.id} className={styles.card}>
              <div className={styles.cardHeader}>
                <h3>{candidate.name}</h3>
                {candidate.yearsOfExperience && (
                  <span className={styles.experience}>{candidate.yearsOfExperience} yrs</span>
                )}
              </div>
              <div className={styles.cardBody}>
                <div className={styles.info}>
                  <strong>Email:</strong> {candidate.email}
                </div>
                {candidate.mobile && (
                  <div className={styles.info}>
                    <strong>Mobile:</strong> {candidate.mobile}
                  </div>
                )}
                {candidate.academicBackground && (
                  <div className={styles.info}>
                    <strong>Education:</strong> {candidate.academicBackground}
                  </div>
                )}
                {candidate.skills && (
                  <div className={styles.skills}>
                    <strong>Skills:</strong>
                    <p>{candidate.skills}</p>
                  </div>
                )}
                {candidate.experienceSummary && (
                  <div className={styles.summary}>
                    <strong>Summary:</strong>
                    <p>{candidate.experienceSummary}</p>
                  </div>
                )}
              </div>
              <div className={styles.cardFooter}>
                <button
                  className={styles.feedbackButton}
                  onClick={() => handleOpenFeedback(candidate.id)}
                  title="Feedback"
                >
                  💬
                </button>
                <button
                  className={styles.enrichButton}
                  onClick={() => handleToggleEnrichment(candidate.id)}
                  title="External Profiles"
                >
                  🔍
                  {profilesByCandidateId[candidate.id]?.some((p) => p.status === 'SUCCESS') && (
                    <span className={styles.enrichedBadge}>✓</span>
                  )}
                </button>
                <button
                  className={styles.deleteButton}
                  onClick={() => handleDelete(candidate.id)}
                  title="Delete candidate"
                >
                  🗑️
                </button>
                <span className={styles.date}>
                  {new Date(candidate.createdAt).toLocaleDateString()}
                </span>
              </div>

              {/* External Profile Enrichment Panel */}
              {expandedEnrichmentId === candidate.id && (
                <div className={styles.enrichmentPanel}>
                  <div className={styles.enrichmentHeader}>
                    <strong>🌐 External Profiles</strong>
                    <div className={styles.enrichmentActions}>
                      <button
                        className={styles.enrichSourceBtn}
                        onClick={() => handleEnrich(candidate.id, 'GITHUB')}
                        disabled={enrichingCandidateId === candidate.id}
                        title="Fetch from GitHub"
                      >
                        {enrichingCandidateId === candidate.id ? '⏳' : '🐙'} GitHub
                      </button>
                      <button
                        className={styles.enrichSourceBtn}
                        onClick={() => handleEnrich(candidate.id, 'TWITTER')}
                        disabled={enrichingCandidateId === candidate.id}
                        title="Fetch from Twitter/X"
                      >
                        {enrichingCandidateId === candidate.id ? '⏳' : '🐦'} Twitter
                      </button>
                      <button
                        className={styles.enrichSourceBtn}
                        onClick={() => handleEnrich(candidate.id, 'INTERNET_SEARCH')}
                        disabled={enrichingCandidateId === candidate.id}
                        title="Aggregate from internet"
                      >
                        {enrichingCandidateId === candidate.id ? '⏳' : '🌐'} Web
                      </button>
                    </div>
                    <div className={styles.enrichFromUrlRow}>
                      <input
                        type="url"
                        placeholder="Paste a profile URL (github.com, x.com, linkedin.com…)"
                        className={styles.enrichUrlInput}
                        value={urlInputByCandidateId[candidate.id] ?? ''}
                        onChange={(e) =>
                          setUrlInputByCandidateId((prev) => ({ ...prev, [candidate.id]: e.target.value }))
                        }
                        onKeyDown={(e) => e.key === 'Enter' && handleEnrichFromUrl(candidate.id)}
                        disabled={enrichingCandidateId === candidate.id}
                      />
                      <button
                        className={styles.enrichSourceBtn}
                        onClick={() => handleEnrichFromUrl(candidate.id)}
                        disabled={enrichingCandidateId === candidate.id || !(urlInputByCandidateId[candidate.id] ?? '').trim()}
                        title="Enrich from URL"
                      >
                        🔗 Enrich
                      </button>
                    </div>
                  </div>

                  {/* Loading state */}
                  {enrichingCandidateId === candidate.id && (
                    <p className={styles.enrichingText}>Fetching profile data…</p>
                  )}

                  {/* No profiles yet */}
                  {!enrichingCandidateId &&
                    (!profilesByCandidateId[candidate.id] ||
                      profilesByCandidateId[candidate.id].length === 0) && (
                      <p className={styles.noProfilesText}>
                        No external profiles yet. Click a source button above to enrich this candidate's profile.
                      </p>
                    )}

                  {/* Profile cards */}
                  {(profilesByCandidateId[candidate.id] ?? []).map((profile) => (
                    <div key={profile.id} className={styles.externalProfileCard}>
                      <div className={styles.externalProfileHeader}>
                        <span className={styles.sourceLabel}>
                          {getSourceIcon(profile.source)} {profile.source.replace('_', ' ')}
                        </span>
                        <span className={`${styles.statusBadge} ${getStatusBadgeClass(profile.status)}`}>
                          {profile.status}
                        </span>
                        <button
                          className={styles.refreshProfileBtn}
                          onClick={() => handleRefreshProfile(profile, candidate.id)}
                          disabled={enrichingCandidateId === candidate.id}
                          title="Refresh this profile"
                        >
                          🔄
                        </button>
                      </div>

                      {profile.status === 'SUCCESS' && (
                        <div className={styles.externalProfileBody}>
                          {profile.profileUrl && (
                            <div className={styles.profileField}>
                              <strong>Profile:</strong>{' '}
                              <a
                                href={profile.profileUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className={styles.profileLink}
                              >
                                {profile.profileUrl}
                              </a>
                            </div>
                          )}
                          {profile.displayName && (
                            <div className={styles.profileField}>
                              <strong>Name:</strong> {profile.displayName}
                            </div>
                          )}
                          {profile.bio && (
                            <div className={styles.profileField}>
                              <strong>Bio:</strong> {profile.bio}
                            </div>
                          )}
                          {profile.location && (
                            <div className={styles.profileField}>
                              <strong>Location:</strong> {profile.location}
                            </div>
                          )}
                          {profile.company && (
                            <div className={styles.profileField}>
                              <strong>Company:</strong> {profile.company}
                            </div>
                          )}
                          {profile.source === 'GITHUB' && profile.publicRepos !== null && (
                            <div className={styles.profileStats}>
                              <span title="Public repositories">📦 {profile.publicRepos} repos</span>
                              {profile.followers !== null && (
                                <span title="Followers">👥 {profile.followers} followers</span>
                              )}
                            </div>
                          )}
                          {profile.repositories && (
                            <div className={styles.profileField}>
                              <strong>Notable Projects:</strong>
                              <p className={styles.repoList}>{profile.repositories}</p>
                            </div>
                          )}
                          {profile.lastFetchedAt && (
                            <div className={styles.lastUpdated}>
                              Last updated: {new Date(profile.lastFetchedAt).toLocaleString()}
                            </div>
                          )}
                        </div>
                      )}

                      {(profile.status === 'FAILED' || profile.status === 'NOT_AVAILABLE') && (
                        <p className={styles.errorText}>
                          {profile.errorMessage ?? 'Enrichment failed.'}
                        </p>
                      )}

                      {profile.status === 'NOT_FOUND' && (
                        <p className={styles.notFoundText}>
                          No matching profile found on {profile.source.replace('_', ' ')}.
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Feedback Modal */}
      {showFeedbackModal && selectedCandidateId && (
        <div className={styles.modal} onClick={handleCloseFeedback}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2>Candidate Feedback</h2>
              <button onClick={handleCloseFeedback} className={styles.closeButton}>
                ✕
              </button>
            </div>

            <div className={styles.modalBody}>
              {!showFeedbackForm ? (
                <>
                  <button
                    onClick={() => setShowFeedbackForm(true)}
                    className={styles.addFeedbackButton}
                  >
                    + Add Feedback
                  </button>
                  <FeedbackList
                    entityId={selectedCandidateId}
                    entityType={EntityType.CANDIDATE}
                    refreshTrigger={feedbackRefreshTrigger}
                  />
                </>
              ) : (
                <FeedbackForm
                  entityId={selectedCandidateId}
                  entityType={EntityType.CANDIDATE}
                  onSuccess={handleFeedbackSuccess}
                  onCancel={() => setShowFeedbackForm(false)}
                />
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default CandidateList
