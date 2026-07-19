import { motion, useInView } from 'framer-motion'
import { useRef } from 'react'
import { ArrowRight, Check } from 'lucide-react'
import { WordsPullUpMultiStyle } from './WordsPullUpMultiStyle'

const features = [
  {
    id: 'video',
    title: 'Your creative canvas.',
    content: null,
    videoUrl: 'https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260406_133058_0504132a-0cf3-4450-a370-8ea3b05c95d4.mp4',
  },
  {
    id: '01',
    title: 'Project Storyboard.',
    icon: 'https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOottwIXH07lWA1P%2Fhf_20260405_171918_4a5edc79-d78f-4637-ac8b-53c43c220606.png&w=1280&q=85',
    checklist: [
      'Pre-production planning',
      'Scene composition tools',
      'Shot list management',
      'Collaboration workflow',
    ],
  },
  {
    id: '02',
    title: 'Smart Critiques.',
    icon: 'https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOottwIXH07lWA1P%2Fhf_20260405_171741_ed9845ab-f5b2-4018-8ce7-07cc01823522.png&w=1280&q=85',
    checklist: [
      'AI-powered frame analysis',
      'Creative feedback notes',
      'Tool integrations',
    ],
  },
  {
    id: '03',
    title: 'Immersion Capsule.',
    icon: 'https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOottwIXH07lWA1P%2Fhf_20260405_171809_f56666dc-c099-4778-ad82-9ad4f209567b.png&w=1280&q=85',
    checklist: [
      'Notification silencing',
      'Ambient soundscapes',
      'Schedule syncing',
    ],
  },
]

export function Features() {
  const ref = useRef(null)
  const isInView = useInView(ref, { once: true, margin: '-100px' })

  return (
    <section className="min-h-screen bg-black relative">
      <div className="bg-noise" />

      <div ref={ref} className="relative z-10 px-4 md:px-6 py-20 md:py-32">
        {/* Header */}
        <div className="mb-12 md:mb-20">
          <WordsPullUpMultiStyle
            segments={[
              { text: 'Studio-grade workflows for visionary creators.', className: '' },
              { text: 'Built for pure vision. Powered by art.', className: 'text-gray-500' },
            ]}
            className="text-xl sm:text-2xl md:text-3xl lg:text-4xl font-normal flex-col gap-2 md:gap-3"
          />
        </div>

        {/* Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-2 md:gap-1 lg:h-[480px]">
          {features.map((feature, index) => (
            <motion.div
              key={feature.id}
              className={`relative rounded-2xl overflow-hidden ${
                feature.id === 'video' ? 'bg-transparent' : 'bg-[#212121]'
              }`}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={isInView ? { opacity: 1, scale: 1 } : { opacity: 0, scale: 0.95 }}
              transition={{
                duration: 0.6,
                delay: index * 0.15,
                ease: [0.22, 1, 0.36, 1],
              }}
            >
              {feature.id === 'video' ? (
                <>
                  <video
                    className="absolute inset-0 w-full h-full object-cover"
                    autoPlay
                    loop
                    muted
                    playsInline
                    src={feature.videoUrl}
                  />
                  <div className="absolute bottom-0 left-0 right-0 p-6">
                    <p className="text-[#E1E0CC] text-lg md:text-xl font-medium">
                      {feature.title}
                    </p>
                  </div>
                </>
              ) : (
                <div className="p-5 md:p-6 h-full flex flex-col">
                  {/* Icon */}
                  <div className="mb-4">
                    <img
                      src={feature.icon}
                      alt=""
                      className="w-10 h-10 sm:w-12 sm:h-12 rounded"
                    />
                  </div>

                  {/* Number + Title */}
                  <h3 className="text-[#E1E0CC] text-base sm:text-lg md:text-xl font-medium mb-4">
                    {feature.id} {feature.title}
                  </h3>

                  {/* Checklist */}
                  <ul className="flex-1 space-y-2 md:space-y-3">
                    {feature.checklist?.map((item, i) => (
                      <li key={i} className="flex items-start gap-2">
                        <Check className="w-4 h-4 sm:w-5 sm:h-5 text-[#DEDBC8] flex-shrink-0 mt-0.5" />
                        <span className="text-gray-400 text-xs sm:text-sm">{item}</span>
                      </li>
                    ))}
                  </ul>

                  {/* Learn More Link */}
                  <div className="mt-6">
                    <a
                      href="#"
                      className="inline-flex items-center gap-2 text-[#DEDBC8] text-xs sm:text-sm hover:gap-3 transition-all"
                    >
                      <span>Learn more</span>
                      <ArrowRight className="w-4 h-4 rotate-[-45deg]" />
                    </a>
                  </div>
                </div>
              )}
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
