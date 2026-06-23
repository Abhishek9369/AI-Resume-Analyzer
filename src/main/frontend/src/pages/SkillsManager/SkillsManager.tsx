import { useEffect, useState } from 'react'
import { graphqlClient, GET_ALL_SKILLS, CREATE_SKILL, UPDATE_SKILL, DELETE_SKILL } from '@/services/graphql'
import styles from './SkillsManager.module.css'

interface Skill {
  id: string
  name: string
  category?: string
  description?: string
  isActive: boolean
  createdAt?: string
  updatedAt?: string
}

interface EditingSkill {
  id?: string
  name: string
  category: string
  description: string
  isActive: boolean
}

const SkillsManager = () => {
  const [skills, setSkills] = useState<Skill[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editForm, setEditForm] = useState<EditingSkill>({
    name: '',
    category: '',
    description: '',
    isActive: true,
  })
  const [showAddForm, setShowAddForm] = useState(false)
  const [currentPage, setCurrentPage] = useState(1)
  const itemsPerPage = 10

  useEffect(() => {
    fetchSkills()
  }, [])

  const fetchSkills = async () => {
    setLoading(true)
    setError(null)
    try {
      const data: { allSkills: Skill[] } = await graphqlClient.request(GET_ALL_SKILLS)
      setSkills(data.allSkills)
    } catch (err) {
      setError('Failed to load skills')
      console.error('Error fetching skills:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleEdit = (skill: Skill) => {
    setEditingId(skill.id)
    setEditForm({
      id: skill.id,
      name: skill.name,
      category: skill.category || '',
      description: skill.description || '',
      isActive: skill.isActive,
    })
    setShowAddForm(false)
  }

  const handleCancelEdit = () => {
    setEditingId(null)
    setEditForm({
      name: '',
      category: '',
      description: '',
      isActive: true,
    })
  }

  const handleSaveEdit = async () => {
    if (!editForm.name.trim()) {
      alert('Skill name is required')
      return
    }

    setLoading(true)
    try {
      const data: { updateSkill: Skill } = await graphqlClient.request(UPDATE_SKILL, {
        id: editingId,
        name: editForm.name.trim(),
        category: editForm.category.trim() || null,
        description: editForm.description.trim() || null,
        isActive: editForm.isActive,
      })
      
      setSkills(skills.map(s => s.id === editingId ? data.updateSkill : s))
      handleCancelEdit()
    } catch (err) {
      setError('Failed to update skill')
      console.error('Error updating skill:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleAdd = () => {
    setShowAddForm(true)
    setEditingId(null)
    setEditForm({
      name: '',
      category: '',
      description: '',
      isActive: true,
    })
  }

  const handleCancelAdd = () => {
    setShowAddForm(false)
    setEditForm({
      name: '',
      category: '',
      description: '',
      isActive: true,
    })
  }

  const handleSaveAdd = async () => {
    if (!editForm.name.trim()) {
      alert('Skill name is required')
      return
    }

    setLoading(true)
    try {
      const data: { createSkill: Skill } = await graphqlClient.request(CREATE_SKILL, {
        name: editForm.name.trim(),
        category: editForm.category.trim() || null,
        description: editForm.description.trim() || null,
      })
      
      setSkills([...skills, data.createSkill])
      handleCancelAdd()
    } catch (err) {
      setError('Failed to create skill. Skill may already exist.')
      console.error('Error creating skill:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id: string, name: string) => {
    if (!confirm(`Are you sure you want to delete "${name}"?`)) {
      return
    }

    setLoading(true)
    try {
      await graphqlClient.request(DELETE_SKILL, { id })
      setSkills(skills.filter(s => s.id !== id))
    } catch (err) {
      setError('Failed to delete skill')
      console.error('Error deleting skill:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleFormChange = (field: keyof EditingSkill, value: string | boolean) => {
    setEditForm(prev => ({ ...prev, [field]: value }))
  }

  // Pagination logic
  const totalPages = Math.ceil(skills.length / itemsPerPage)
  const startIndex = (currentPage - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  const paginatedSkills = skills.slice(startIndex, endIndex)

  const handlePageChange = (page: number) => {
    setCurrentPage(page)
    setEditingId(null)
    setShowAddForm(false)
  }

  // Reset to page 1 when skills list changes (add/delete)
  useEffect(() => {
    if (currentPage > totalPages && totalPages > 0) {
      setCurrentPage(totalPages)
    }
  }, [totalPages, currentPage])

  return (
    <div className={styles.skillsManager}>
      <div className={styles.header}>
        <h2>Skills Master Data ({skills.length})</h2>
        <button onClick={handleAdd} className={styles.addButton} disabled={showAddForm}>
          + Add New Skill
        </button>
      </div>

      {error && (
        <div className={styles.error}>
          {error}
          <button onClick={() => setError(null)}>×</button>
        </div>
      )}

      {showAddForm && (
        <div className={styles.addForm}>
          <h3>Add New Skill</h3>
          <div className={styles.formGrid}>
            <div className={styles.formGroup}>
              <label htmlFor="add-name">Skill Name *</label>
              <input
                id="add-name"
                type="text"
                value={editForm.name}
                onChange={(e) => handleFormChange('name', e.target.value)}
                placeholder="e.g., Java, Spring Boot"
              />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="add-category">Category</label>
              <input
                id="add-category"
                type="text"
                value={editForm.category}
                onChange={(e) => handleFormChange('category', e.target.value)}
                placeholder="e.g., Programming Language"
              />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="add-description">Description</label>
              <input
                id="add-description"
                type="text"
                value={editForm.description}
                onChange={(e) => handleFormChange('description', e.target.value)}
                placeholder="Brief description"
              />
            </div>
          </div>
          <div className={styles.formActions}>
            <button onClick={handleCancelAdd} className={styles.cancelButton} title="Cancel">
              ✕ Cancel
            </button>
            <button onClick={handleSaveAdd} className={styles.saveButton} disabled={loading} title="Save Skill">
              ✓ Save Skill
            </button>
          </div>
        </div>
      )}

      {loading && !showAddForm && !editingId && (
        <div className={styles.loading}>Loading skills...</div>
      )}

      {!loading && skills.length === 0 && (
        <div className={styles.empty}>
          <p>No skills found. Add your first skill to get started!</p>
        </div>
      )}

      {skills.length > 0 && (
        <div className={styles.tableContainer}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Name</th>
                <th>Category</th>
                <th>Description</th>
                <th>Status</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {paginatedSkills.map((skill) => (
                <tr key={skill.id} className={editingId === skill.id ? styles.editing : ''}>
                  {editingId === skill.id ? (
                    <>
                      <td>
                        <input
                          type="text"
                          value={editForm.name}
                          onChange={(e) => handleFormChange('name', e.target.value)}
                          className={styles.editInput}
                          aria-label="Edit skill name"
                        />
                      </td>
                      <td>
                        <input
                          type="text"
                          value={editForm.category}
                          onChange={(e) => handleFormChange('category', e.target.value)}
                          className={styles.editInput}
                          aria-label="Edit skill category"
                        />
                      </td>
                      <td>
                        <input
                          type="text"
                          value={editForm.description}
                          onChange={(e) => handleFormChange('description', e.target.value)}
                          className={styles.editInput}
                          aria-label="Edit skill description"
                        />
                      </td>
                      <td>
                        <label className={styles.checkboxLabel}>
                          <input
                            type="checkbox"
                            checked={editForm.isActive}
                            onChange={(e) => handleFormChange('isActive', e.target.checked)}
                            aria-label="Toggle skill active status"
                          />
                          Active
                        </label>
                      </td>
                      <td>
                        {skill.createdAt
                          ? new Date(skill.createdAt).toLocaleDateString()
                          : '-'}
                      </td>
                      <td>
                        <div className={styles.actions}>
                          <button
                            onClick={handleSaveEdit}
                            className={styles.saveBtn}
                            disabled={loading}
                            title="Save changes"
                          >
                            ✓
                          </button>
                          <button onClick={handleCancelEdit} className={styles.cancelBtn} title="Cancel editing">
                            ✕
                          </button>
                        </div>
                      </td>
                    </>
                  ) : (
                    <>
                      <td className={styles.nameCell}>{skill.name}</td>
                      <td>{skill.category || '-'}</td>
                      <td className={styles.descCell}>{skill.description || '-'}</td>
                      <td>
                        <span className={skill.isActive ? styles.active : styles.inactive}>
                          {skill.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td>
                        {skill.createdAt
                          ? new Date(skill.createdAt).toLocaleDateString()
                          : '-'}
                      </td>
                      <td>
                        <div className={styles.actions}>
                          <button
                            onClick={() => handleEdit(skill)}
                            className={styles.editBtn}
                            disabled={loading || showAddForm}
                            title="Edit skill"
                          >
                            ✏️
                          </button>
                          <button
                            onClick={() => handleDelete(skill.id, skill.name)}
                            className={styles.deleteBtn}
                            disabled={loading || showAddForm}
                            title="Delete skill"
                          >
                            🗑️
                          </button>
                        </div>
                      </td>
                    </>
                  )}
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div className={styles.pagination}>
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 1}
                className={styles.pageButton}
                title="Previous Page"
              >
                ← Previous
              </button>

              <div className={styles.pageNumbers}>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                  <button
                    key={page}
                    onClick={() => handlePageChange(page)}
                    className={`${styles.pageNumber} ${currentPage === page ? styles.active : ''}`}
                    title={`Go to page ${page}`}
                  >
                    {page}
                  </button>
                ))}
              </div>

              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
                className={styles.pageButton}
                title="Next Page"
              >
                Next →
              </button>

              <span className={styles.pageInfo}>
                Page {currentPage} of {totalPages} ({skills.length} total)
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default SkillsManager
