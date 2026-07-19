import { motion } from 'framer-motion'
import { ArrowRight } from 'lucide-react'
import { WordsPullUp } from './WordsPullUp'

const navItems = ['Our story', 'Collective', 'Workshops', 'Programs', 'Inquiries']

export function Hero() {
  return (
    <section className="h-screen relative">
      <div className="p-4 md:p-6 h-full">
        <div className="relative h-full rounded-2xl md:rounded-[2rem] overflow-hidden">
          {/* Background Video */}
          <video
            className="absolute inset-0 w-full h-full object-cover"
            autoPlay
            loop
            muted
            playsInline
            src="https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260405_170732_8a9ccda6-5cff-4628-b164-059c500a2b41.mp4"
          />

          {/* Noise Overlay */}
          <div className="noise-overlay" />

          {/* Gradient Overlay */}
          <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-transparent to-black/60" />

          {/* Navbar */}
          <nav className="absolute top-0 left-1/2 -translate-x-1/2 z-20">
            <div className="bg-black rounded-b-2xl md:rounded-b-3xl px-4 py-2 md:px-8">
              <ul className="flex gap-3 sm:gap-6 md:gap-12 lg:gap-14">
                {navItems.map((item) => (
                  <li key={item}>
                    <a
                      href="#"
                      style={{ color: 'rgba(225, 224, 204, 0.8)' }}
                      className="text-[10px] sm:text-xs md:text-sm transition-colors hover:text-[#E1E0CC]"
                    >
                      {item}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          </nav>

          {/* Hero Content */}
          <div className="absolute bottom-0 left-0 right-0 p-6 md:p-10">
            <div className="grid grid-cols-12 gap-4 items-end">
              {/* Giant Heading */}
              <div className="col-span-12 lg:col-span-8">
                <div className="relative">
                  <h1 className="text-[26vw] sm:text-[24vw] md:text-[22vw] lg:text-[20vw] xl:text-[19vw] 2xl:text-[20vw] font-medium leading-[0.85] tracking-[-0.07em]">
                    <WordsPullUp text="Prisma" showAsterisk textStyle={{ color: '#E1E0CC', position: 'relative' }} />
                  </h1>
                </div>
              </div>

              {/* Description + CTA */}
              <div className="col-span-12 lg:col-span-4">
                <motion.p
                  className="mb-6"
                  style={{ color: 'rgba(222, 219, 200, 0.7)' }}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.8, delay: 0.5, ease: [0.16, 1, 0.3, 1] }}
                >
                  Prisma is a worldwide network of visual artists, filmmakers and storytellers bound not by place, status or labels but by passion and hunger to unlock potential through our unique perspectives.
                </motion.p>

                <motion.button
                  className="group flex items-center gap-2 bg-[#DEDBC8] rounded-full px-6 py-3 text-black font-medium text-sm sm:text-base transition-all"
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.8, delay: 0.7, ease: [0.16, 1, 0.3, 1] }}
                  whileHover={{ gap: 12 }}
                >
                  <span>Join the lab</span>
                  <span className="bg-black rounded-full w-9 h-9 sm:w-10 sm:h-10 flex items-center justify-center transition-transform group-hover:scale-110">
                    <ArrowRight className="w-4 h-4 sm:w-5 sm:h-5 text-[#DEDBC8]" />
                  </span>
                </motion.button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
