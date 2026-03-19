<template>
  <div class="mobile-page">
    <div class="mobile-topbar">
      <div>
        <div class="mobile-topbar-title">预约座位</div>
        <div class="mobile-topbar-subtitle">先选校区和自习室，再挑选时间段与座位号。</div>
      </div>
      <span class="mobile-chip">H5 用户完整链路</span>
    </div>

    <div class="mobile-form-card">
      <div class="mobile-field">
        <label>校区</label>
        <select v-model="form.campus" @change="handleCampusChange">
          <option value="">请选择校区</option>
          <option v-for="item in campuses" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="mobile-field">
        <label>楼栋</label>
        <select v-model="form.building" @change="handleBuildingChange" :disabled="!form.campus">
          <option value="">请选择楼栋</option>
          <option v-for="item in buildings" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="mobile-field">
        <label>自习室</label>
        <select v-model="form.roomId" :disabled="!form.building">
          <option value="">请选择自习室</option>
          <option v-for="item in rooms" :key="item.id" :value="item.id">{{ item.roomName }}（开放 {{ item.openSeats || item.totalSeats || 0 }} 座）</option>
        </select>
      </div>
      <div class="mobile-field">
        <label>日期</label>
        <input v-model="form.date" type="date" />
      </div>
      <div class="mobile-grid-2">
        <div class="mobile-field">
          <label>开始时间</label>
          <input v-model="form.startTime" type="time" />
        </div>
        <div class="mobile-field">
          <label>结束时间</label>
          <input v-model="form.endTime" type="time" />
        </div>
      </div>
      <div class="mobile-field">
        <label>座位号（可多个，逗号分隔）</label>
        <input v-model="form.seatNosText" type="text" placeholder="例如 A01,A02" />
      </div>
      <div class="mobile-actions">
        <button type="button" class="mobile-outline-btn" @click="checkConflicts">查看冲突</button>
        <button type="button" class="mobile-action-btn" @click="submitReservation">确认预约</button>
      </div>
    </div>

    <section class="mobile-section">
      <div class="mobile-section-title">可用提示</div>
      <div class="mobile-card">
        <div class="mobile-item-meta">当前冲突座位：{{ conflictSeatNos.length ? conflictSeatNos.join('、') : '暂无冲突' }}</div>
        <div class="mobile-item-meta" style="margin-top:8px;">预约建议：优先选择靠前空位并避免与冲突座位重复输入。</div>
      </div>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
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
  name: 'UserMobileReserve',
  data () {
    return {
      campuses: [],
      buildings: [],
      rooms: [],
      conflictSeatNos: [],
      form: {
        campus: '',
        building: '',
        roomId: '',
        date: todayString(),
        startTime: '09:00',
        endTime: '11:00',
        seatNosText: ''
      }
    }
  },
  mounted () {
    this.loadCampuses()
  },
  methods: {
    async loadCampuses () {
      const res = await request.get('/room/campuses')
      this.campuses = res.code === 200 ? (res.data || []) : []
    },
    async handleCampusChange () {
      this.form.building = ''
      this.form.roomId = ''
      this.rooms = []
      const res = await request.get('/room/buildings', { params: { campus: this.form.campus } })
      this.buildings = res.code === 200 ? (res.data || []) : []
    },
    async handleBuildingChange () {
      this.form.roomId = ''
      const res = await request.get('/room/rooms', { params: { campus: this.form.campus, building: this.form.building } })
      this.rooms = res.code === 200 ? (res.data || []) : []
    },
    async checkConflicts () {
      if (!this.form.roomId) return ElMessage.warning('请先选择自习室')
      const res = await request.get('/reservation/seatConflicts', {
        params: {
          roomId: this.form.roomId,
          date: this.form.date,
          startTime: this.form.startTime,
          endTime: this.form.endTime
        }
      })
      if (res.code !== 200) {
        ElMessage.error(res.msg || '查询冲突失败')
        return
      }
      const items = Array.isArray(res.data) ? res.data : (res.data?.seatNos || [])
      this.conflictSeatNos = items.map(item => typeof item === 'string' ? item : (item.seatNo || item))
      ElMessage.success('已刷新冲突座位')
    },
    async submitReservation () {
      const user = getLoginUser()
      if (!user || !user.id) return ElMessage.error('请先登录')
      if (!this.form.roomId) return ElMessage.warning('请选择自习室')
      const seatNos = this.form.seatNosText.split(',').map(item => item.trim()).filter(Boolean)
      if (!seatNos.length) return ElMessage.warning('请至少输入一个座位号')
      const payload = {
        userId: user.id,
        roomId: Number(this.form.roomId),
        date: this.form.date,
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        seatNos
      }
      const res = await request.post('/reservation/create', payload)
      if (res.code === 200) {
        ElMessage.success(res.msg || '预约成功')
        this.$router.push('/m/user/orders')
      } else {
        ElMessage.error(res.msg || '预约失败')
      }
    }
  }
}
</script>