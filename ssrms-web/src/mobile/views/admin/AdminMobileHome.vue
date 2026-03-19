<template>
  <div class="mobile-page">
    <div class="mobile-hero">
      <div class="mobile-split">
        <div>
          <div>🧭 管理员移动工作台</div>
          <div class="mobile-hero-subtitle">用于值班巡检、处理预约与查看待办。复杂配置仍建议在桌面端完成。</div>
        </div>
        <span class="mobile-badge info">轻量版</span>
      </div>
    </div>

    <section class="mobile-section">
      <div class="mobile-grid-2">
        <div class="mobile-stat">
          <div class="mobile-stat-label">今日预约</div>
          <div class="mobile-stat-value">{{ metrics.todayReservations ?? 0 }}</div>
          <div class="mobile-stat-desc">较昨日 {{ metrics.todayTrend ?? 0 }}%</div>
        </div>
        <div class="mobile-stat">
          <div class="mobile-stat-label">今日签到率</div>
          <div class="mobile-stat-value">{{ metrics.todaySignRate ?? 0 }}%</div>
          <div class="mobile-stat-desc">开放自习室 {{ metrics.openRooms ?? 0 }} 间</div>
        </div>
        <div class="mobile-stat">
          <div class="mobile-stat-label">待处理反馈</div>
          <div class="mobile-stat-value">{{ feedback.pending ?? 0 }}</div>
          <div class="mobile-stat-desc">处理中 {{ feedback.processing ?? 0 }}</div>
        </div>
        <div class="mobile-stat">
          <div class="mobile-stat-label">开放座位数</div>
          <div class="mobile-stat-value">{{ metrics.totalSeats ?? 0 }}</div>
          <div class="mobile-stat-desc">本周违约率 {{ weekly.weekViolationRate ?? 0 }}%</div>
        </div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">快捷处理</div>
      <div class="mobile-card">
        <div class="mobile-actions">
          <button type="button" class="mobile-action-btn" @click="$router.push('/m/admin/reservations')">处理预约</button>
          <button type="button" class="mobile-outline-btn" @click="$router.push('/m/admin/users')">处理用户</button>
          <button type="button" class="mobile-outline-btn" @click="$router.push('/m/admin/notices')">公告管理</button>
        </div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">最近动态</div>
      <div class="mobile-list">
        <div v-for="item in activities" :key="item.id + '-' + item.time" class="mobile-list-item">
          <div class="mobile-split">
            <div class="mobile-item-title">{{ item.text }}</div>
            <span class="mobile-badge" :class="activityClass(item.type)">{{ item.typeLabel }}</span>
          </div>
          <div class="mobile-item-meta">{{ item.time }}</div>
        </div>
        <div v-if="!activities.length" class="mobile-card mobile-empty">暂无最新动态。</div>
      </div>
    </section>
  </div>
</template>

<script>
import request from '@/utils/request'
import '../../components/mobile.css'

export default {
  name: 'AdminMobileHome',
  data () {
    return {
      metrics: {},
      weekly: {},
      activities: [],
      feedback: {}
    }
  },
  mounted () {
    this.loadWorkbench()
  },
  methods: {
    async loadWorkbench () {
      const res = await request.get('/mobile/admin/workbench')
      if (res.code !== 200) return
      const data = res.data || {}
      this.metrics = data.metrics || {}
      this.weekly = data.weekly || {}
      this.activities = data.latestActivities || []
      this.feedback = data.feedbackStats || {}
    },
    activityClass (type) {
      if (type === 'sign') return 'success'
      if (type === 'violation') return 'danger'
      if (type === 'cancel') return 'warning'
      return 'info'
    }
  }
}
</script>