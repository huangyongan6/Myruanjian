import { motion, useInView } from 'framer-motion'
import { useRef } from 'react'

interface Segment {
  text: string
  className?: string
  italic?: boolean
}

interface WordsPullUpMultiStyleProps {
  segments: Segment[]
  className?: string
}

export function WordsPullUpMultiStyle({ segments, className = '' }: WordsPullUpMultiStyleProps) {
  const ref = useRef(null)
  const isInView = useInView(ref, { once: true })

  const allWords: { text: string; className?: string; italic?: boolean }[] = []
  segments.forEach((segment) => {
    const words = segment.text.split(' ')
    words.forEach((word, i) => {
      allWords.push({
        text: word,
        className: segment.className,
        italic: segment.italic,
      })
      if (i < words.length - 1) {
        allWords.push({ text: ' ' })
      }
    })
  })

  let globalIndex = 0

  return (
    <span ref={ref} className={`inline-flex flex-wrap justify-center ${className}`}>
      {allWords.map((item, i) => {
        if (item.text === ' ') {
          return <span key={i}> </span>
        }
        const currentIndex = globalIndex
        globalIndex++
        return (
          <span key={i} className="inline-flex overflow-hidden">
            <motion.span
              className="inline-block"
              style={{
                ...(item.italic ? { fontFamily: '"Instrument Serif"', fontStyle: 'italic' } : {}),
                color: item.className?.includes('text-gray-500') ? undefined : '#E1E0CC',
              }}
              initial={{ y: 20, opacity: 0 }}
              animate={isInView ? { y: 0, opacity: 1 } : { y: 20, opacity: 0 }}
              transition={{
                duration: 0.5,
                delay: currentIndex * 0.08,
                ease: [0.16, 1, 0.3, 1],
              }}
            >
              {item.text}
            </motion.span>
          </span>
        )
      })}
    </span>
  )
}
