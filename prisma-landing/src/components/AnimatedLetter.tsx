import { motion, useTransform, MotionValue } from 'framer-motion'

interface AnimatedLetterProps {
  char: string
  charProgress: MotionValue<number>
  index: number
  totalChars: number
}

export function AnimatedLetter({ char, charProgress, index, totalChars }: AnimatedLetterProps) {
  const charStart = index / totalChars
  const opacity = useTransform(
    charProgress,
    [charStart - 0.1, charStart + 0.05],
    [0.2, 1]
  )

  return (
    <span className="inline-block">
      <motion.span style={{ opacity }}>{char}</motion.span>
    </span>
  )
}
