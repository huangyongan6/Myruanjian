<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const canvasRef = ref<HTMLCanvasElement | null>(null)
const cursorRef = ref<HTMLDivElement | null>(null)

// 主题模式
const isDark = ref(true)

// 动画阶段
const phase = ref(0) // 0: 第一幕(粒子布满), 1: 第二幕(主题+Slogan), 2: 第三幕(功能+CTA)
const sloganText = ref('')
const fullSlogan = '探索智能学习的无限可能'
const showCTA = ref(false)
const showFeatures = ref([false, false, false])
const rippleList = ref<{ x: number; y: number; id: number }[]>([])
let rippleId = 0

// 鼠标位置
let mouseX = 0
let mouseY = 0
let animationId: number

// 粒子系统
interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  alpha: number
  targetAlpha: number
  color: string
  pulse: number
  pulseSpeed: number
  // 初始分散位置
  initX: number
  initY: number
  // 星尘
  isStarDust: boolean
  starAngle: number
  starDist: number
}

const particles: Particle[] = []
const starDustParticles: Particle[] = []
const particleCount = 500
const starDustCount = 40

const colors = ['#00d4ff', '#a855f7', '#667eea', '#00ffcc']
const darkBg = '#0a0a1a'
const lightBg = '#f8fafc'

// 3D几何体状态
const geometryRotation = ref({ x: 0, y: 0 })
const geometryOpacity = ref(0)

// Canvas尺寸
let canvasWidth = 0
let canvasHeight = 0

// 打字机
let typeInterval: number | null = null

function initParticles(): void {
  if (!canvasRef.value) return
  particles.length = 0

  const centerX = canvasWidth / 2
  const centerY = canvasHeight / 2

  // 粒子初始位置：在画布范围内随机分布
  for (let i = 0; i < particleCount; i++) {
    const x = Math.random() * canvasWidth
    const y = Math.random() * canvasHeight

    particles.push({
      x,
      y,
      initX: x,
      initY: y,
      vx: (Math.random() - 0.5) * 0.2,
      vy: (Math.random() - 0.5) * 0.2,
      size: Math.random() * 2 + 1,
      alpha: 0,
      targetAlpha: Math.random() * 0.5 + 0.3,
      color: colors[Math.floor(Math.random() * colors.length)],
      pulse: Math.random() * Math.PI * 2,
      pulseSpeed: Math.random() * 0.015 + 0.005,
      isStarDust: false,
      starAngle: 0,
      starDist: 0
    })
  }

  // 星尘粒子
  for (let i = 0; i < starDustCount; i++) {
    starDustParticles.push({
      x: 0,
      y: 0,
      initX: 0,
      initY: 0,
      vx: 0,
      vy: 0,
      size: Math.random() * 1.5 + 0.8,
      alpha: 0,
      targetAlpha: 0,
      color: colors[Math.floor(Math.random() * colors.length)],
      pulse: Math.random() * Math.PI * 2,
      pulseSpeed: Math.random() * 0.025 + 0.015,
      isStarDust: true,
      starAngle: Math.random() * Math.PI * 2,
      starDist: 0
    })
  }
}

function drawBackground(ctx: CanvasRenderingContext2D): void {
  const grad = ctx.createRadialGradient(
    canvasWidth / 2, canvasHeight / 2, 0,
    canvasWidth / 2, canvasHeight / 2, canvasWidth * 0.7
  )

  if (isDark.value) {
    grad.addColorStop(0, 'rgba(20, 20, 45, 1)')
    grad.addColorStop(0.5, 'rgba(15, 15, 35, 1)')
    grad.addColorStop(1, 'rgba(10, 10, 26, 1)')
  } else {
    grad.addColorStop(0, 'rgba(248, 250, 252, 1)')
    grad.addColorStop(0.5, 'rgba(241, 245, 249, 1)')
    grad.addColorStop(1, 'rgba(226, 232, 240, 1)')
  }

  ctx.fillStyle = grad
  ctx.fillRect(0, 0, canvasWidth, canvasHeight)
}

function drawGrid(ctx: CanvasRenderingContext2D): void {
  const gridColor = isDark.value ? 'rgba(100, 116, 139, 0.05)' : 'rgba(100, 116, 139, 0.06)'
  const gridSpacing = 60

  ctx.strokeStyle = gridColor
  ctx.lineWidth = 0.5

  for (let x = gridSpacing; x < canvasWidth; x += gridSpacing) {
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, canvasHeight)
    ctx.stroke()
  }

  for (let y = gridSpacing; y < canvasHeight; y += gridSpacing) {
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(canvasWidth, y)
    ctx.stroke()
  }
}

function drawGeometry(ctx: CanvasRenderingContext2D): void {
  if (geometryOpacity.value === 0) return

  const centerX = canvasWidth / 2
  const centerY = canvasHeight / 2 - 20
  const baseSize = 70

  ctx.save()
  ctx.globalAlpha = geometryOpacity.value
  ctx.translate(centerX, centerY)

  const rotX = geometryRotation.value.x
  const rotY = geometryRotation.value.y

  // 外层光环
  ctx.strokeStyle = isDark.value ? 'rgba(0, 212, 255, 0.12)' : 'rgba(0, 212, 255, 0.15)'
  ctx.lineWidth = 1
  for (let i = 0; i < 3; i++) {
    const ringSize = baseSize * (1.6 + i * 0.35)
    const rotation = rotY + i * 0.4
    ctx.save()
    ctx.rotate(rotation)
    ctx.beginPath()
    ctx.ellipse(0, 0, ringSize, ringSize * 0.35, 0, 0, Math.PI * 2)
    ctx.stroke()
    ctx.restore()
  }

  // 八面体
  ctx.strokeStyle = isDark.value ? 'rgba(168, 85, 247, 0.35)' : 'rgba(168, 85, 247, 0.4)'
  ctx.lineWidth = 1.5

  const topY = -baseSize * 0.8
  const bottomY = baseSize * 0.8
  const midAngle = rotY
  const midRadius = baseSize * 0.65

  const vertices = [
    { x: 0, y: topY },
    { x: Math.cos(midAngle) * midRadius, y: -baseSize * 0.15 },
    { x: Math.cos(midAngle + Math.PI / 2) * midRadius, y: -baseSize * 0.15 },
    { x: Math.cos(midAngle + Math.PI) * midRadius, y: -baseSize * 0.15 },
    { x: Math.cos(midAngle + Math.PI * 1.5) * midRadius, y: -baseSize * 0.15 },
    { x: 0, y: bottomY }
  ]

  const edges = [
    [0, 1], [0, 2], [0, 3], [0, 4],
    [5, 1], [5, 2], [5, 3], [5, 4],
    [1, 2], [2, 3], [3, 4], [4, 1]
  ]

  edges.forEach(([from, to]) => {
    ctx.beginPath()
    ctx.moveTo(vertices[from].x, vertices[from].y)
    ctx.lineTo(vertices[to].x, vertices[to].y)
    ctx.stroke()
  })

  // 顶点发光
  vertices.forEach((v, i) => {
    const pulse = Math.sin(rotX + i) * 0.25 + 0.75
    const glow = ctx.createRadialGradient(v.x, v.y, 0, v.x, v.y, 6)
    glow.addColorStop(0, isDark.value ? `rgba(0, 212, 255, ${pulse})` : `rgba(0, 212, 255, ${pulse * 0.8})`)
    glow.addColorStop(1, 'transparent')
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(v.x, v.y, 6, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(v.x, v.y, 1.5, 0, Math.PI * 2)
    ctx.fill()
  })

  // 能量核心
  const coreGlow = ctx.createRadialGradient(0, -baseSize * 0.1, 0, 0, -baseSize * 0.1, baseSize * 0.45)
  coreGlow.addColorStop(0, isDark.value ? 'rgba(0, 212, 255, 0.25)' : 'rgba(0, 212, 255, 0.2)')
  coreGlow.addColorStop(0.5, isDark.value ? 'rgba(168, 85, 247, 0.08)' : 'rgba(168, 85, 247, 0.06)')
  coreGlow.addColorStop(1, 'transparent')
  ctx.fillStyle = coreGlow
  ctx.beginPath()
  ctx.arc(0, -baseSize * 0.1, baseSize * 0.45, 0, Math.PI * 2)
  ctx.fill()

  ctx.restore()
}

function drawParticle(ctx: CanvasRenderingContext2D, p: Particle): void {
  if (p.alpha <= 0) return

  const pulse = Math.sin(p.pulse) * 0.25 + 0.75
  const size = p.size * pulse

  const glow = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, size * 3.5)
  glow.addColorStop(0, p.color)
  glow.addColorStop(0.4, `${p.color}66`)
  glow.addColorStop(1, 'transparent')

  ctx.globalAlpha = p.alpha * pulse
  ctx.fillStyle = glow
  ctx.beginPath()
  ctx.arc(p.x, p.y, size * 3.5, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = isDark.value ? '#fff' : '#1e293b'
  ctx.beginPath()
  ctx.arc(p.x, p.y, size * 0.35, 0, Math.PI * 2)
  ctx.fill()

  ctx.globalAlpha = 1
}

function drawConnections(ctx: CanvasRenderingContext2D): void {
  if (phase.value < 1) return

  const maxDist = 70
  const alpha = isDark.value ? 0.06 : 0.04

  for (let i = 0; i < particles.length; i += 3) {
    const p1 = particles[i]
    if (p1.alpha <= 0) continue

    for (let j = i + 1; j < Math.min(i + 20, particles.length); j++) {
      const p2 = particles[j]
      if (p2.alpha <= 0) continue

      const dx = p1.x - p2.x
      const dy = p1.y - p2.y
      const dist = Math.sqrt(dx * dx + dy * dy)

      if (dist < maxDist) {
        ctx.beginPath()
        ctx.strokeStyle = isDark.value
          ? `rgba(0, 212, 255, ${(1 - dist / maxDist) * alpha})`
          : `rgba(100, 116, 139, ${(1 - dist / maxDist) * alpha})`
        ctx.lineWidth = 0.4
        ctx.moveTo(p1.x, p1.y)
        ctx.lineTo(p2.x, p2.y)
        ctx.stroke()
      }
    }
  }
}

function drawRipples(ctx: CanvasRenderingContext2D): void {
  rippleList.value.forEach(ripple => {
    const age = Date.now() - ripple.id
    const maxAge = 1500
    const progress = age / maxAge

    if (progress >= 1) return

    const radius = progress * 200
    const alpha = (1 - progress) * 0.15

    ctx.beginPath()
    ctx.arc(ripple.x, ripple.y, radius, 0, Math.PI * 2)
    ctx.strokeStyle = isDark.value
      ? `rgba(0, 212, 255, ${alpha})`
      : `rgba(0, 212, 255, ${alpha * 0.7})`
    ctx.lineWidth = 1.5
    ctx.stroke()
  })
}

function animate(): void {
  if (!canvasRef.value) return
  const ctx = canvasRef.value.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, canvasWidth, canvasHeight)
  drawBackground(ctx)
  drawGrid(ctx)

  const time = Date.now() * 0.001

  // 几何体旋转
  if (phase.value >= 1) {
    geometryRotation.value.x += 0.004
    geometryRotation.value.y += 0.006
    if (geometryOpacity.value < 0.8) {
      geometryOpacity.value += 0.015
    }
  }

  // 更新粒子
  particles.forEach(p => {
    p.pulse += p.pulseSpeed

    if (phase.value === 0) {
      // 第一幕：粒子淡入
      if (p.alpha < p.targetAlpha) {
        p.alpha += 0.008
      }
      // 轻微漂浮
      p.x += Math.sin(time + p.pulse) * 0.15
      p.y += Math.cos(time + p.pulse) * 0.15
    } else {
      // 第二幕之后：粒子缓慢移动
      p.x += p.vx * 0.3
      p.y += p.vy * 0.3

      // 边界反弹
      if (p.x < 0 || p.x > canvasWidth) p.vx *= -1
      if (p.y < 0 || p.y > canvasHeight) p.vy *= -1

      // 靠近鼠标时散开
      const dx = mouseX - p.x
      const dy = mouseY - p.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 80 && phase.value >= 2) {
        p.vx -= (dx / dist) * 0.02
        p.vy -= (dy / dist) * 0.02
      }
    }
  })

  // 星尘粒子
  if (phase.value >= 1) {
    starDustParticles.forEach((p, i) => {
      p.starAngle += 0.015 + i * 0.0008
      p.starDist = 35 + Math.sin(time * 1.5 + i) * 18
      p.x = mouseX + Math.cos(p.starAngle) * p.starDist
      p.y = mouseY + Math.sin(p.starAngle) * p.starDist
      p.alpha = Math.max(0, 0.5 - p.starDist / 100)
    })
  }

  // 绘制
  drawGeometry(ctx)
  particles.forEach(p => drawParticle(ctx, p))
  drawConnections(ctx)

  if (phase.value >= 1) {
    starDustParticles.forEach(p => drawParticle(ctx, p))
  }

  // 共鸣波纹
  drawRipples(ctx)

  // 鼠标光晕
  if (phase.value >= 1) {
    const cursorGlow = ctx.createRadialGradient(mouseX, mouseY, 0, mouseX, mouseY, 80)
    cursorGlow.addColorStop(0, isDark.value ? 'rgba(0, 212, 255, 0.1)' : 'rgba(0, 212, 255, 0.06)')
    cursorGlow.addColorStop(1, 'transparent')
    ctx.fillStyle = cursorGlow
    ctx.beginPath()
    ctx.arc(mouseX, mouseY, 80, 0, Math.PI * 2)
    ctx.fill()
  }

  // 清理过期波纹
  const now = Date.now()
  rippleList.value = rippleList.value.filter(r => now - r.id < 1500)

  animationId = requestAnimationFrame(animate)
}

function typeSlogan(): void {
  let charIndex = 0
  sloganText.value = ''
  typeInterval = window.setInterval(() => {
    if (charIndex < fullSlogan.length) {
      sloganText.value += fullSlogan[charIndex]
      charIndex++
    } else {
      if (typeInterval) clearInterval(typeInterval)
    }
  }, 70)
}

function triggerRipple(x: number, y: number): void {
  rippleList.value.push({ x, y, id: Date.now() })
}

function runSequence(): void {
  // 第一幕：粒子淡入布满虚空（0-0.1s）
  setTimeout(() => {
    phase.value = 0
  }, 100)

  // 第二幕：主题+Slogan（1s）
  setTimeout(() => {
    phase.value = 1
    geometryOpacity.value = 0.01
    typeSlogan()
  }, 1000)

  // 第三幕：功能+CTA（2.3s+）
  setTimeout(() => {
    phase.value = 2
    showFeatures.value = [true, false, false]
  }, 2300)

  setTimeout(() => {
    showFeatures.value = [true, true, false]
  }, 2700)

  setTimeout(() => {
    showFeatures.value = [true, true, true]
  }, 3100)

  setTimeout(() => {
    showCTA.value = true
    // 中心点触发共鸣波纹
    triggerRipple(canvasWidth / 2, canvasHeight / 2)
  }, 4000)
}

function toggleTheme(): void {
  isDark.value = !isDark.value
}

function startLearning(): void {
  // 触发多个共鸣波纹
  triggerRipple(mouseX, mouseY)
  setTimeout(() => triggerRipple(canvasWidth / 2, canvasHeight / 2), 150)
  setTimeout(() => {
    router.push({ name: 'Chat' })
  }, 400)
}

function handleResize(): void {
  if (!canvasRef.value) return
  canvasWidth = window.innerWidth
  canvasHeight = window.innerHeight
  canvasRef.value.width = canvasWidth
  canvasRef.value.height = canvasHeight
  initParticles()
}

function handleMouseMove(e: MouseEvent): void {
  mouseX = e.clientX
  mouseY = e.clientY
  if (cursorRef.value) {
    cursorRef.value.style.left = `${mouseX}px`
    cursorRef.value.style.top = `${mouseY}px`
  }
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  window.addEventListener('mousemove', handleMouseMove)
  animate()
  runSequence()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('mousemove', handleMouseMove)
  cancelAnimationFrame(animationId)
  if (typeInterval) clearInterval(typeInterval)
})

const features = [
  { icon: 'chat', title: '智能对话', desc: '随时提问，即刻解答', color: '#00d4ff' },
  { icon: 'book', title: '知识库', desc: '构建你的专属知识图谱', color: '#a855f7' },
  { icon: 'path', title: '学习路径', desc: '量身定制的成长方案', color: '#00ffcc' }
]

const sloganDisplay = computed(() => sloganText.value)
</script>

<template>
  <div class="welcome" :class="{ 'welcome--light': !isDark }">
    <canvas ref="canvasRef" class="welcome__canvas"></canvas>

    <div ref="cursorRef" class="welcome__cursor-glow"></div>

    <!-- 主题切换 -->
    <button class="welcome__theme-toggle" @click="toggleTheme">
      <svg v-if="isDark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="5"/>
        <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
      </svg>
    </button>

    <div class="welcome__content">
      <!-- 中心几何体 -->
      <div class="welcome__geometry"></div>

      <!-- 标题区域 -->
      <div class="welcome__header" :class="{ 'welcome__header--visible': phase >= 1 }">
        <h1 class="welcome__title">
          <span class="welcome__title-text">智能学习</span>
        </h1>
        <p class="welcome__slogan">
          {{ sloganDisplay }}<span class="welcome__cursor">|</span>
        </p>
      </div>

      <!-- 功能模块 -->
      <div class="welcome__features">
        <div
          v-for="(feature, index) in features"
          :key="feature.title"
          class="welcome__feature"
          :class="{ 'welcome__feature--visible': showFeatures[index] }"
          :style="{ '--delay': `${index * 0.18}s`, '--accent': feature.color }"
        >
          <div class="welcome__feature-icon">
            <svg v-if="feature.icon === 'chat'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <svg v-else-if="feature.icon === 'book'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
            </svg>
            <svg v-else-if="feature.icon === 'path'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/>
              <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/>
            </svg>
          </div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
          <div class="welcome__feature-glow"></div>
        </div>
      </div>

      <!-- CTA按钮 -->
      <div class="welcome__cta" :class="{ 'welcome__cta--visible': showCTA }">
        <button class="welcome__btn" @click="startLearning">
          <span class="welcome__btn-bg"></span>
          <span class="welcome__btn-content">
            <span>开启学习之旅</span>
            <svg class="welcome__btn-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </span>
          <span class="welcome__btn-ring"></span>
        </button>
      </div>
    </div>

    <!-- 底部装饰 -->
    <div class="welcome__footer">
      <div class="welcome__footer-line"></div>
      <span class="welcome__footer-text">Intelligent Learning System</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.welcome {
  --bg-primary: #0a0a1a;
  --text-primary: #ffffff;
  --text-secondary: rgba(255, 255, 255, 0.55);
  --accent-cyan: #00d4ff;
  --accent-purple: #a855f7;
  --accent-green: #00ffcc;
  --border-color: rgba(255, 255, 255, 0.07);
  --card-bg: rgba(255, 255, 255, 0.025);
  --card-hover: rgba(255, 255, 255, 0.05);

  min-height: 100vh;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  transition: background 0.5s ease;

  &--light {
    --bg-primary: #f8fafc;
    --text-primary: #1e293b;
    --text-secondary: rgba(30, 41, 59, 0.55);
    --border-color: rgba(30, 41, 59, 0.08);
    --card-bg: rgba(30, 41, 59, 0.02);
    --card-hover: rgba(30, 41, 59, 0.04);
  }

  &__canvas {
    position: fixed;
    inset: 0;
    z-index: 1;
    pointer-events: none;
  }

  &__cursor-glow {
    position: fixed;
    width: 140px;
    height: 140px;
    background: radial-gradient(circle, rgba(0, 212, 255, 0.1) 0%, rgba(168, 85, 247, 0.05) 40%, transparent 70%);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    pointer-events: none;
    z-index: 100;
    transition: background 0.3s ease;
  }

  &__theme-toggle {
    position: fixed;
    top: 28px;
    right: 28px;
    z-index: 50;
    width: 42px;
    height: 42px;
    padding: 9px;
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    cursor: pointer;
    color: var(--text-secondary);
    transition: all 0.3s ease;

    &:hover {
      background: var(--card-hover);
      color: var(--accent-cyan);
      transform: scale(1.05);
    }
  }

  &__content {
    position: relative;
    z-index: 10;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 0 40px;
    max-width: 960px;
    width: 100%;
  }

  &__geometry {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 280px;
    height: 280px;
    pointer-events: none;
  }

  &__header {
    text-align: center;
    margin-bottom: 50px;
    opacity: 0;
    transform: translateY(-25px);
    transition: all 1.2s cubic-bezier(0.16, 1, 0.3, 1);

    &--visible {
      opacity: 1;
      transform: translateY(0);
    }
  }

  &__title {
    margin: 0 0 20px;
    font-size: clamp(40px, 7vw, 64px);
    font-weight: 800;
    letter-spacing: 8px;
  }

  &__title-text {
    background: linear-gradient(135deg, var(--accent-cyan) 0%, var(--accent-purple) 50%, var(--accent-cyan) 100%);
    background-size: 200% 200%;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    animation: gradientShift 5s ease infinite;
    filter: drop-shadow(0 0 35px rgba(0, 212, 255, 0.35));

    .welcome--light & {
      background: linear-gradient(135deg, #0284c7 0%, #7c3aed 50%, #0284c7 100%);
      background-size: 200% 200%;
      -webkit-background-clip: text;
      background-clip: text;
      -webkit-text-fill-color: transparent;
      animation: gradientShift 5s ease infinite;
      filter: drop-shadow(0 0 25px rgba(2, 132, 199, 0.25));
    }
  }

  @keyframes gradientShift {
    0%, 100% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
  }

  &__slogan {
    margin: 0;
    font-size: clamp(15px, 2.5vw, 20px);
    font-weight: 400;
    color: var(--text-secondary);
    letter-spacing: 4px;
    min-height: 28px;
  }

  &__cursor {
    animation: blink 1s infinite;
    color: var(--accent-cyan);
    .welcome--light & { color: #0284c7; }
  }

  @keyframes blink {
    0%, 50% { opacity: 1; }
    51%, 100% { opacity: 0; }
  }

  &__features {
    display: flex;
    gap: 28px;
    justify-content: center;
    margin-bottom: 48px;
    flex-wrap: wrap;
  }

  &__feature {
    position: relative;
    flex: 1;
    min-width: 190px;
    max-width: 240px;
    padding: 30px 22px;
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 18px;
    text-align: center;
    overflow: hidden;
    opacity: 0;
    transform: translateY(35px) scale(0.92);
    transition: all 0.7s cubic-bezier(0.16, 1, 0.3, 1);
    transition-delay: var(--delay);
    cursor: default;

    &--visible {
      opacity: 1;
      transform: translateY(0) scale(1);
    }

    &:hover {
      background: var(--card-hover);
      transform: translateY(-6px) scale(1.02);
      border-color: var(--accent);

      .welcome__feature-icon {
        transform: scale(1.12);
        filter: drop-shadow(0 0 16px var(--accent));
      }

      .welcome__feature-glow { opacity: 1; }
    }

    h3 {
      margin: 14px 0 6px;
      font-size: 17px;
      font-weight: 600;
      color: var(--text-primary);
      letter-spacing: 1px;
    }

    p {
      margin: 0;
      font-size: 13px;
      color: var(--text-secondary);
    }
  }

  &__feature-icon {
    width: 44px;
    height: 44px;
    margin: 0 auto;
    color: var(--accent);
    transition: all 0.4s ease;

    svg { width: 100%; height: 100%; }
  }

  &__feature-glow {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 50% 0%, color-mix(in srgb, var(--accent) 12%, transparent), transparent 60%);
    opacity: 0;
    transition: opacity 0.4s ease;
    pointer-events: none;
  }

  &__cta {
    opacity: 0;
    transform: translateY(18px);
    transition: all 1s cubic-bezier(0.16, 1, 0.3, 1);

    &--visible {
      opacity: 1;
      transform: translateY(0);
    }
  }

  &__btn {
    position: relative;
    padding: 0;
    background: transparent;
    border: none;
    cursor: pointer;

    &:hover {
      .welcome__btn-bg {
        transform: scale(1.1);
        filter: blur(18px);
      }

      .welcome__btn-content {
        box-shadow: 0 14px 45px rgba(0, 212, 255, 0.4);
      }

      .welcome__btn-arrow { transform: translateX(5px); }

      .welcome__btn-ring { animation: ringPulse 0.7s ease-out; }
    }
  }

  &__btn-bg {
    position: absolute;
    inset: -10px;
    background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple));
    border-radius: 60px;
    opacity: 0.45;
    filter: blur(14px);
    transition: all 0.4s ease;

    .welcome--light & {
      background: linear-gradient(135deg, #0284c7, #7c3aed);
    }
  }

  &__btn-content {
    position: relative;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 17px 52px;
    background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple));
    border-radius: 60px;
    font-size: 16px;
    font-weight: 600;
    color: #fff;
    letter-spacing: 2px;
    box-shadow: 0 10px 35px rgba(0, 212, 255, 0.3);
    transition: all 0.4s ease;

    .welcome--light & {
      background: linear-gradient(135deg, #0284c7, #7c3aed);
      box-shadow: 0 10px 35px rgba(2, 132, 199, 0.25);
    }
  }

  &__btn-arrow {
    width: 18px;
    height: 18px;
    transition: transform 0.3s ease;
  }

  &__btn-ring {
    position: absolute;
    inset: -5px;
    border: 2px solid var(--accent-cyan);
    border-radius: 65px;
    opacity: 0;
    pointer-events: none;

    .welcome--light & { border-color: #0284c7; }
  }

  @keyframes ringPulse {
    0% { opacity: 0.7; transform: scale(1); }
    100% { opacity: 0; transform: scale(1.5); }
  }

  &__footer {
    position: absolute;
    bottom: 36px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    z-index: 10;
  }

  &__footer-line {
    width: 80px;
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--accent-cyan), transparent);
    opacity: 0.35;

    .welcome--light & {
      background: linear-gradient(90deg, transparent, #0284c7, transparent);
    }
  }

  &__footer-text {
    font-size: 10px;
    letter-spacing: 4px;
    color: var(--text-secondary);
    text-transform: uppercase;
  }
}

@media (max-width: 768px) {
  .welcome {
    &__content { padding: 0 20px; }

    &__features {
      flex-direction: column;
      align-items: center;
      gap: 18px;
    }

    &__feature {
      width: 100%;
      max-width: 300px;
    }

    &__theme-toggle {
      top: 18px;
      right: 18px;
      width: 38px;
      height: 38px;
    }

    &__cursor-glow { display: none; }
  }
}
</style>
