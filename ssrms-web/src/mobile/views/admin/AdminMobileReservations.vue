<template>
  <div class="mobile-page admin-mobile-page">
    <div class="mobile-topbar mobile-topbar-sticky">
      <div>
        <div class="mobile-topbar-title">预约处理</div>
        <div class="mobile-topbar-subtitle">筛选今日或指定日期预约，在手机上快速补录签到、取消或标记违约。</div>
      </div>
    </div>

    <div class="mobile-form-card mobile-filter-card">
      <div class="mobile-field"><label>关键词</label><input v-model="query.keyword" type="text" placeholder="姓名 / 学号 / 预约号" /></div>
      <div class="mobile-grid-2">
        <div class="mobile-field"><label>状态</label><select v-model="query.status"><option value="">全部</option><option value="reserved">待签到</option><option value="checked_in">已签到</option><option value="late">迟到签到</option><option value="cancelled">已取消</option><option value="no_show">未到场</option></select></div>
        <div class="mobile-field"><label>日期</label><input v-model="query.date" type="date" /></div>
      </div>
      <div class="mobile-actions mobile-actions-between">
        <button type="button" class="mobile-outline-btn" @click="resetQuery">重置</button>
        <button type="button" class="mobile-action-btn" @click="loadReservations">查询</button>
      </div>
    </div>

    <section class="mobile-section">
      <div class="mobile-chip-row mobile-chip-row-spacious">
        <span class="mobile-chip">日期 {{ query.date || "全部" }}</span>
        <span class="mobile-chip mobile-chip-muted">共 {{ reservations.length }} 条</span>
      </div>
      <div class="mobile-list">
        <div v-for="item in reservations" :key="item.id" class="mobile-list-item mobile-admin-card">
          <div class="mobile-split">
            <div class="mobile-item-title">{{ item.userName || item.name || item.studentNo || item.reservationNo }}</div>
            <span class="mobile-badge" :class="statusBadgeClass(item.status)">{{ statusText(item.status) }}</span>
          </div>
          <div class="mobile-chip-row mobile-chip-row-spacious">
            <span class="mobile-chip">{{ item.roomLabel || item.roomName || '未命名自习室' }}</span>
            <span class="mobile-chip mobile-chip-muted">座位 {{ item.seatNo || '未指定' }}</span>
          </div>
          <div class="mobile-detail-list">
            <div class="mobile-detail-item"><span class="label">预约时间</span><span class="value">{{ item.date }} {{ timeText(item.startTime) }}-{{ timeText(item.endTime) }}</span></div>
            <div class="mobile-detail-item"><span class="label">预约号</span><span class="value">{{ item.reservationNo || item.code || '-' }}</span></div>
          </div>
          <div class="mobile-actions" style="margin-top:12px;">
            <button v-if="canForceCheckin(item.status)" type="button" class="mobile-action-btn" @click="handleAction('checkin', item.id)">补录签到</button>
            <button v-if="item.status === 'reserved'" type="button" class="mobile-outline-btn" @click="handleAction('cancel', item.id)">取消预约</button>
            <button v-if="canMarkViolation(item.status)" type="button" class="mobile-outline-btn" @click="handleAction('violation', item.id)">标记违约</button>
          </div>
        </div>
        <div v-if="!reservations.length" class="mobile-card mobile-empty">暂无预约数据。</div>
      </div>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import '../../components/mobile.css'

function todayString () {
  const now = new Date()
  const month = `${now.getMonth() + 1}`.padStart(2, '0')
  const date = `${now.getDate()}`.padStart(2, '0')
  return `${now.getFullYear()}-${month}-${date}`
}

export default {
  name: 'AdminMobileReservations',
  data () {
    return {
      defaultDate: todayString(),
      query: { keyword: '', status: '', date: todayString() },
      reservations: []
    }
  },
  mounted () {
    this.loadReservations()
  },
  methods: {
    async loadReservations () {
      const res = await request.get('/reservation/admin/page', {
        params: {
          page: 1,
          size: 20,
          keyword: this.query.keyword || undefined,
          date: this.query.date || undefined,
          status: this.query.status || undefined
        }
      })
      const data = res.data || {}
      this.reservations = Array.isArray(data.records) ? data.records : (Array.isArray(data) ? data : [])
    },
    resetQuery () {
      this.query = { keyword: '', status: '', date: this.defaultDate }
      this.loadReservations()
    },
    async handleAction (action, id) {
      const mapping = {
        checkin: { url: `/reservation/admin/checkin/${id}`, ok: '已补录签到' },
        cancel: { url: `/reservation/admin/cancel/${id}`, ok: '已取消预约' },
        violation: { url: `/reservation/admin/markViolation/${id}`, ok: '已标记违约' }
      }
      const config = mapping[action]
      if (!config) return
      const res = await request.post(config.url)
      if (res.code === 200) {
        ElMessage.success(config.ok)
        this.loadReservations()
      } else {
        ElMessage.error(res.msg || '处理失败')
      }
    },
    canForceCheckin (status) {
      return ['late', 'no_show'].includes(status)
    },
    canMarkViolation (status) {
      return status === 'checked_in'
    },
    timeText (value) {
      return value ? String(value).slice(0, 5) : '--:--'
    },
    statusText (status) {
      return {
        reserved: '待签到',
        checked_in: '已签到',
        late: '迟到签到',
        cancelled: '已取消',
        cancel_overdue: '逾期取消',
        no_show: '未到场'
      }[status] || status
    },
    statusBadgeClass (status) {
      if (status === 'reserved') return 'info'
      if (status === 'checked_in') return 'success'
      if (status === 'late') return 'warning'
      return 'danger'
    }
  }
}
</script>