import { useRef } from 'react'
import { motion, useScroll } from 'framer-motion'
import { WordsPullUpMultiStyle } from './WordsPullUpMultiStyle'
import { AnimatedLetter } from './AnimatedLetter'

export function About() {
  const containerRef = useRef<HTMLDivElement>(null)
  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ['start 0.8', 'end 0.2'],
  })

  const aboutText = "Over the last seven years, I have worked with Parallax, a Berlin-based production house that crafts cinema, series, and Noir Studio in Paris. Together, we have created work that has earned international acclaim at several major festivals."

  return (
    <section ref={containerRef} className="bg-black py-20 md:py-32">
      <div className="max-w-6xl mx-auto px-6">
        {/* Label */}
        <motion.p
          className="text-[#DEDBC8] text-[10px] sm:text-xs mb-8 md:mb-12"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          transition={{ duration: 0.6 }}
        >
          Visual arts
        </motion.p>

        {/* Main Heading */}
        <div className="max-w-3xl mx-auto mb-16 md:mb-24">
          <WordsPullUpMultiStyle
            segments={[
              { text: 'I am Marcus Chen,', className: '' },
              { text: 'a self-taught director.', className: '', italic: true },
              { text: 'I have skills in color grading, visual effects, and narrative design.', className: '' },
            ]}
            className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl xl:text-7xl leading-[0.95] sm:leading-[0.9]"
          />
        </div>

        {/* Body Paragraph */}
        <div className="max-w-3xl mx-auto">
          <p className="text-[#DEDBC8] text-xs sm:text-sm md:text-base leading-relaxed">
            {aboutText.split('').map((char, i, arr) => (
              <AnimatedLetter
                key={i}
                char={char}
                charProgress={scrollYProgress}
                index={i}
                totalChars={arr.length}
              />
            ))}
          </p>
        </div>
      </div>
    </section>
  )
}
