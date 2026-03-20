<template>
  <div class="mobile-page">
    <div class="mobile-hero">
      <div class="mobile-split mobile-split-start">
        <div>
          <div class="mobile-hero-title">{{ greeting }}，{{ profile.name || profile.account || '同学' }}</div>
          <div class="mobile-hero-subtitle">移动端首页改为按房间实时查询余座，避免展示错误总座位数，同时保留下一条预约与公告提醒。</div>
        </div>
        <span class="mobile-badge" :class="creditBadgeClass">信用 {{ profile.creditScore ?? '--' }}</span>
      </div>
    </div>

    <section class="mobile-section">
      <div class="mobile-grid-2">
        <div class="mobile-stat">
          <div class="mobile-stat-label">房间实时余座</div>
          <div class="mobile-stat-value">{{ roomAvailabilityText }}</div>
          <div class="mobile-stat-desc">{{ roomAvailabilityDesc }}</div>
        </div>
        <div class="mobile-stat">
          <div class="mobile-stat-label">本月累计预约</div>
          <div class="mobile-stat-value">{{ monthBrief.monthReserveCount ?? 0 }}</div>
          <div class="mobile-stat-desc">最近一次到馆 {{ monthBrief.lastVisitTime || '暂无记录' }}</div>
        </div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">余座速查</div>
      <div class="mobile-form-card">
        <div class="mobile-grid-2">
          <div class="mobile-field">
            <label>校区</label>
            <select v-model="query.campus" @change="handleCampusChange">
              <option value="">请选择校区</option>
              <option v-for="item in campuses" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="mobile-field">
            <label>楼栋</label>
            <select v-model="query.building" :disabled="!query.campus" @change="handleBuildingChange">
              <option value="">请选择楼栋</option>
              <option v-for="item in buildings" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
        </div>
        <div class="mobile-field">
          <label>自习室</label>
          <select v-model="query.roomId" :disabled="!query.building">
            <option value="">请选择自习室</option>
            <option v-for="item in rooms" :key="item.id" :value="item.id">{{ item.roomName }}</option>
          </select>
        </div>
        <div class="mobile-grid-2">
          <div class="mobile-field">
            <label>日期</label>
            <input v-model="query.date" type="date" />
          </div>
          <div class="mobile-field">
            <label>开始时间</label>
            <input v-model="query.startTime" type="time" />
          </div>
        </div>
        <div class="mobile-grid-2">
          <div class="mobile-field">
            <label>结束时间</label>
            <input v-model="query.endTime" type="time" />
          </div>
          <div class="mobile-field">
            <label>查询结果</label>
            <input :value="queryResultLabel" type="text" readonly />
          </div>
        </div>
        <div class="mobile-actions mobile-actions-between">
          <button type="button" class="mobile-outline-btn" @click="goReservePage">去预约页</button>
          <button type="button" class="mobile-action-btn" @click="checkRoomAvailability">查询余座</button>
        </div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">快捷入口</div>
      <div class="mobile-card">
        <div class="mobile-actions compact-four">
          <button type="button" class="mobile-action-btn" @click="$router.push('/m/user/reserve')">找座</button>
          <button type="button" class="mobile-outline-btn" @click="$router.push('/m/user/orders')">我的预约</button>
          <button type="button" class="mobile-outline-btn" @click="$router.push('/m/user/feedback')">反馈</button>
          <button type="button" class="mobile-outline-btn" @click="$router.push('/m/user/notices')">公告</button>
        </div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">下一条预约</div>
      <div v-if="nextReservation" class="mobile-list-item">
        <div class="mobile-split mobile-split-start">
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
          <div class="mobile-split mobile-split-start">
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

function todayString () {
  const now = new Date()
  const month = `${now.getMonth() + 1}`.padStart(2, '0')
  const date = `${now.getDate()}`.padStart(2, '0')
  return `${now.getFullYear()}-${month}-${date}`
}

export default {
  name: 'UserMobileHome',
  data () {
    return {
      profile: {},
      overview: {},
      monthBrief: {},
      nextReservation: null,
      notices: [],
      campuses: [],
      buildings: [],
      rooms: [],
      selectedRoom: null,
      roomRemaining: null,
      roomOccupied: 0,
      query: {
        campus: '',
        building: '',
        roomId: '',
        date: todayString(),
        startTime: '09:00',
        endTime: '11:00'
      }
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
    },
    roomAvailabilityText () {
      return this.roomRemaining === null ? '待查询' : `${this.roomRemaining} 座`
    },
    roomAvailabilityDesc () {
      if (!this.selectedRoom) return '请选择校区、楼栋和自习室后查询实时余座。'
      return `${this.selectedRoom.roomName} · 已占用 ${this.roomOccupied} 座 · 当前开放 ${this.selectedRoom.openSeats || this.selectedRoom.totalSeats || 0} 座`
    },
    queryResultLabel () {
      if (this.roomRemaining === null) return '未查询'
      return `剩余 ${this.roomRemaining} 座`
    }
  },
  mounted () {
    this.loadData()
    this.loadCampuses()
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
    async loadCampuses () {
      const res = await request.get('/room/campuses')
      this.campuses = res.code === 200 ? (res.data || []) : []
    },
    async handleCampusChange () {
      this.query.building = ''
      this.query.roomId = ''
      this.selectedRoom = null
      this.roomRemaining = null
      this.rooms = []
      const res = await request.get('/room/buildings', { params: { campus: this.query.campus } })
      this.buildings = res.code === 200 ? (res.data || []) : []
    },
    async handleBuildingChange () {
      this.query.roomId = ''
      this.selectedRoom = null
      this.roomRemaining = null
      const res = await request.get('/room/rooms', { params: { campus: this.query.campus, building: this.query.building } })
      this.rooms = res.code === 200 ? (res.data || []) : []
    },
    async checkRoomAvailability () {
      if (!this.query.roomId) return ElMessage.warning('请先选择自习室')
      const res = await request.get('/reservation/seatConflicts', {
        params: {
          roomId: this.query.roomId,
          date: this.query.date,
          startTime: this.query.startTime,
          endTime: this.query.endTime
        }
      })
      if (res.code !== 200) {
        ElMessage.error(res.msg || '查询余座失败')
        return
      }
      this.selectedRoom = this.rooms.find(item => String(item.id) === String(this.query.roomId)) || null
      const occupied = Array.isArray(res.data) ? res.data.length : ((res.data?.seatNos || []).length)
      const openSeats = Number(this.selectedRoom?.openSeats || this.selectedRoom?.totalSeats || 0)
      this.roomOccupied = occupied
      this.roomRemaining = Math.max(openSeats - occupied, 0)
      ElMessage.success('已更新房间余座')
    },
    goReservePage () {
      this.$router.push('/m/user/reserve')
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