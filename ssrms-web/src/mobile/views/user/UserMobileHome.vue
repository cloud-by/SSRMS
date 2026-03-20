<template>
  <div class="mobile-page">
    <div class="mobile-hero">
      <div class="mobile-split">
        <div>
          <div>👋 {{ greeting }}，{{ profile.name || profile.account || '同学' }}</div>
          <div class="mobile-hero-subtitle">今天也适合高效学习。这里集中显示今日剩余座位、下一条预约和最新公告。</div>
        </div>
        <span class="mobile-badge" :class="creditBadgeClass">信用 {{ profile.creditScore ?? '--' }}</span>
      </div>
    </div>

    <section class="mobile-section">
      <div class="mobile-grid-2">
        <div class="mobile-stat">
          <div class="mobile-stat-label">今日剩余座位</div>
          <div class="mobile-stat-value">{{ overview.remainingCount ?? '--' }}</div>
          <div class="mobile-stat-desc">总座位 {{ overview.totalSeats ?? '--' }}，已预约 {{ overview.reservedCount ?? '--' }}</div>
        </div>
        <div class="mobile-stat">
          <div class="mobile-stat-label">本月累计预约</div>
          <div class="mobile-stat-value">{{ monthBrief.monthReserveCount ?? 0 }}</div>
          <div class="mobile-stat-desc">最近一次到馆 {{ monthBrief.lastVisitTime || '暂无记录' }}</div>
        </div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">快捷入口</div>
      <div class="mobile-card">
        <div class="mobile-actions">
          <button type="button" class="mobile-action-btn" @click="$router.push('/m/user/reserve')">立即找座</button>
          <button type="button" class="mobile-outline-btn" @click="$router.push('/m/user/orders')">我的预约</button>
          <button type="button" class="mobile-outline-btn" @click="$router.push('/m/user/feedback')">评价投诉</button>
          <button type="button" class="mobile-outline-btn" @click="$router.push('/m/user/notices')">公告中心</button>
        </div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">下一条预约</div>
      <div v-if="nextReservation" class="mobile-list-item">
        <div class="mobile-split">
          <div class="mobile-item-title">{{ nextReservation.roomLabel || nextReservation.roomName || '未命名自习室' }}</div>
          <span class="mobile-badge info">{{ reservationStatusText(nextReservation.status) }}</span>
        </div>
        <div class="mobile-item-meta">{{ nextReservation.date }} {{ timeText(nextReservation.startTime) }}-{{ timeText(nextReservation.endTime) }}</div>
        <div class="mobile-item-meta">座位 {{ nextReservation.seatNo || '未指定' }} · 预约号 {{ nextReservation.reservationNo }}</div>
        <div class="mobile-actions" style="margin-top:12px;">
          <button v-if="nextReservation.status === 'reserved'" type="button" class="mobile-action-btn" @click="handleCheckin(nextReservation.id)">去签到</button>
          <button v-if="nextReservation.status === 'reserved'" type="button" class="mobile-outline-btn" @click="handleCancel(nextReservation.id)">取消预约</button>
          <button type="button" class="mobile-outline-btn" @click="$router.push('/m/user/orders')">查看全部</button>
        </div>
      </div>
      <div v-else class="mobile-card mobile-empty">你当前没有待处理预约，去预约一个新座位吧。</div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">最新公告</div>
      <div class="mobile-list">
        <div v-for="item in notices" :key="item.id" class="mobile-list-item">
          <div class="mobile-split">
            <div class="mobile-item-title">{{ item.title }}</div>
            <span class="mobile-badge" :class="noticeBadgeClass(item.level)">{{ item.level || '普通' }}</span>
          </div>
          <div class="mobile-item-meta">{{ item.summary || item.content?.slice(0, 48) || '暂无摘要' }}</div>
          <div class="mobile-item-meta">{{ formatTime(item.publishTime) }} · {{ item.targetText || '全体学生' }}</div>
        </div>
        <div v-if="!notices.length" class="mobile-card mobile-empty">暂无公告。</div>
      </div>
    </section>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import '../../components/mobile.css'
import { getLoginUser } from '../../components/mobileAuth'

export default {
  name: 'UserMobileHome',
  data () {
    return {
      profile: {},
      overview: {},
      monthBrief: {},
      nextReservation: null,
      notices: []
    }
  },
  computed: {
    greeting () {
      const hour = new Date().getHours()
      if (hour < 12) return '早上好'
      if (hour < 18) return '下午好'
      return '晚上好'
    },
    creditBadgeClass () {
      const score = Number(this.profile.creditScore || 0)
      if (score >= 90) return 'success'
      if (score >= 75) return 'warning'
      return 'danger'
    }
  },
  mounted () {
    this.loadData()
  },
  methods: {
    async loadData () {
      const user = getLoginUser()
      if (!user || !user.id) return
      const res = await request.get('/mobile/user/home', { params: { userId: user.id } })
      if (res.code !== 200) {
        ElMessage.error(res.msg || '移动首页加载失败')
        return
      }
      const data = res.data || {}
      this.profile = data.profile || {}
      this.overview = data.todayOverview || {}
      this.monthBrief = data.monthBrief || {}
      this.nextReservation = data.nextReservation || null
      this.notices = data.notices || []
    },
    formatTime (value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').slice(0, 16)
    },
    timeText (value) {
      return value ? String(value).slice(0, 5) : '--:--'
    },
    reservationStatusText (status) {
      return {
        reserved: '待签到',
        checked_in: '已签到',
        late: '迟到签到',
        cancelled: '已取消',
        no_show: '未到场'
      }[status] || status
    },
    noticeBadgeClass (level) {
      if (level === 'HIGH' || level === '重要') return 'danger'
      if (level === 'MEDIUM' || level === '提醒') return 'warning'
      return 'info'
    },
    async handleCheckin (id) {
      const res = await request.post(`/reservation/checkin/${id}`)
      if (res.code === 200) {
        ElMessage.success('签到成功')
        this.loadData()
      } else {
        ElMessage.error(res.msg || '签到失败')
      }
    },
    async handleCancel (id) {
      try {
        await ElMessageBox.confirm('确认取消这条预约吗？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      const res = await request.post(`/reservation/cancel/${id}`)
      if (res.code === 200) {
        ElMessage.success('已取消预约')
        this.loadData()
      } else {
        ElMessage.error(res.msg || '取消失败')
      }
    }
  }
}
</script>