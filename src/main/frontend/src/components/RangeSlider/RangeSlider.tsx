import { useState, useRef, useEffect } from 'react'
import styles from './RangeSlider.module.css'

interface RangeSliderProps {
  min: number
  max: number
  minValue: number
  maxValue: number
  step?: number
  label: string
  unit?: string
  onChange: (minValue: number, maxValue: number) => void
}

const RangeSlider = ({
  min,
  max,
  minValue,
  maxValue,
  step = 1,
  label,
  unit = '',
  onChange,
}: RangeSliderProps) => {
  const [localMinValue, setLocalMinValue] = useState(minValue)
  const [localMaxValue, setLocalMaxValue] = useState(maxValue)
  const minValRef = useRef<HTMLInputElement>(null)
  const maxValRef = useRef<HTMLInputElement>(null)
  const rangeRef = useRef<HTMLDivElement>(null)
  const sliderContainerRef = useRef<HTMLDivElement>(null)

  // Convert to percentage for styling
  const getPercent = (value: number) => Math.round(((value - min) / (max - min)) * 100)

  // Handle min value change
  const handleMinChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = Math.min(Number(e.target.value), localMaxValue - step)
    setLocalMinValue(value)
    onChange(value, localMaxValue)
  }

  // Handle max value change
  const handleMaxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = Math.max(Number(e.target.value), localMinValue + step)
    setLocalMaxValue(value)
    onChange(localMinValue, value)
  }

  // Determine which thumb should be on top based on values
  const getZIndices = () => {
    // Both thumbs at same level since only the thumb circles are clickable
    // The visual range bar is below them
    return { minZ: 4, maxZ: 4 }
  }

  const { minZ, maxZ } = getZIndices()

  // Update range styling
  useEffect(() => {
    if (rangeRef.current) {
      const minPercent = getPercent(localMinValue)
      const maxPercent = getPercent(localMaxValue)

      rangeRef.current.style.left = `${minPercent}%`
      rangeRef.current.style.width = `${maxPercent - minPercent}%`
    }
  }, [localMinValue, localMaxValue, min, max])

  return (
    <div className={styles.container}>
      <label className={styles.label}>{label}</label>
      <div 
        className={styles.sliderContainer}
        ref={sliderContainerRef}
      >
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={localMinValue}
          onChange={handleMinChange}
          ref={minValRef}
          className={`${styles.thumb} ${styles.thumbLeft}`}
          style={{ zIndex: minZ }}
        />
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={localMaxValue}
          onChange={handleMaxChange}
          ref={maxValRef}
          className={`${styles.thumb} ${styles.thumbRight}`}
          style={{ zIndex: maxZ }}
        />

        <div className={styles.slider}>
          <div className={styles.sliderTrack} />
          <div ref={rangeRef} className={styles.sliderRange} />
        </div>
      </div>

      <div className={styles.values}>
        <span className={styles.valueLabel}>
          {localMinValue} {unit}
        </span>
        <span className={styles.valueSeparator}>-</span>
        <span className={styles.valueLabel}>
          {localMaxValue} {unit}
        </span>
      </div>
    </div>
  )
}

export default RangeSlider
