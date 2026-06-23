import { useState, useEffect, useRef } from 'react'
import styles from './SkillsInput.module.css'

export interface Skill {
  id: string
  name: string
  category?: string
}

interface SkillsInputProps {
  selectedSkills: Skill[]
  onChange: (skills: Skill[]) => void
  onSearch: (query: string) => void
  suggestions: Skill[]
  placeholder?: string
}

const SkillsInput = ({
  selectedSkills,
  onChange,
  onSearch,
  suggestions,
  placeholder = 'Type to search skills...',
}: SkillsInputProps) => {
  const [inputValue, setInputValue] = useState('')
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [highlightedIndex, setHighlightedIndex] = useState(-1)
  const inputRef = useRef<HTMLInputElement>(null)
  const suggestionsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (inputValue.trim()) {
      onSearch(inputValue.trim())
      setShowSuggestions(true)
    } else {
      setShowSuggestions(false)
    }
  }, [inputValue, onSearch])

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(event.target as Node) &&
        inputRef.current &&
        !inputRef.current.contains(event.target as Node)
      ) {
        setShowSuggestions(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleAddSkill = (skill: Skill) => {
    // Check if skill is already selected
    if (!selectedSkills.some((s) => s.id === skill.id)) {
      onChange([...selectedSkills, skill])
    }
    setInputValue('')
    setShowSuggestions(false)
    setHighlightedIndex(-1)
    inputRef.current?.focus()
  }

  const handleRemoveSkill = (skillId: string) => {
    onChange(selectedSkills.filter((s) => s.id !== skillId))
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!showSuggestions || suggestions.length === 0) {
      return
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setHighlightedIndex((prev) =>
          prev < suggestions.length - 1 ? prev + 1 : prev
        )
        break
      case 'ArrowUp':
        e.preventDefault()
        setHighlightedIndex((prev) => (prev > 0 ? prev - 1 : -1))
        break
      case 'Enter':
        e.preventDefault()
        if (highlightedIndex >= 0 && highlightedIndex < suggestions.length) {
          handleAddSkill(suggestions[highlightedIndex])
        }
        break
      case 'Escape':
        setShowSuggestions(false)
        setHighlightedIndex(-1)
        break
    }
  }

  const filteredSuggestions = suggestions.filter(
    (suggestion) => !selectedSkills.some((s) => s.id === suggestion.id)
  )

  return (
    <div className={styles.skillsInput}>
      <div className={styles.selectedSkills}>
        {selectedSkills.map((skill) => (
          <div key={skill.id} className={styles.skillBadge}>
            <span className={styles.skillName}>{skill.name}</span>
            {skill.category && (
              <span className={styles.skillCategory}>{skill.category}</span>
            )}
            <button
              type="button"
              className={styles.removeButton}
              onClick={() => handleRemoveSkill(skill.id)}
              aria-label={`Remove ${skill.name}`}
            >
              ×
            </button>
          </div>
        ))}
      </div>
      
      <div className={styles.inputContainer}>
        <input
          ref={inputRef}
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => inputValue && setShowSuggestions(true)}
          placeholder={placeholder}
          className={styles.input}
        />
        
        {showSuggestions && filteredSuggestions.length > 0 && (
          <div ref={suggestionsRef} className={styles.suggestions}>
            {filteredSuggestions.map((suggestion, index) => (
              <div
                key={suggestion.id}
                className={`${styles.suggestionItem} ${
                  index === highlightedIndex ? styles.highlighted : ''
                }`}
                onClick={() => handleAddSkill(suggestion)}
                onMouseEnter={() => setHighlightedIndex(index)}
              >
                <span className={styles.suggestionName}>{suggestion.name}</span>
                {suggestion.category && (
                  <span className={styles.suggestionCategory}>
                    {suggestion.category}
                  </span>
                )}
              </div>
            ))}
          </div>
        )}
        
        {showSuggestions && inputValue && filteredSuggestions.length === 0 && (
          <div ref={suggestionsRef} className={styles.suggestions}>
            <div className={styles.noResults}>No skills found</div>
          </div>
        )}
      </div>
    </div>
  )
}

export default SkillsInput
