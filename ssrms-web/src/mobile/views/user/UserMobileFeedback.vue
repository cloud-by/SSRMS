<template>
  <div class="mobile-page">
    <div class="mobile-topbar">
      <div>
        <div class="mobile-topbar-title">评价与投诉</div>
        <div class="mobile-topbar-subtitle">支持提交投诉、建议、表扬并查看自己的处理进度。</div>
      </div>
      <button type="button" class="mobile-ghost-btn" @click="loadFeedback">刷新</button>
    </div>

    <section class="mobile-section">
      <div class="mobile-form-card">
        <div class="mobile-grid-2">
          <div class="mobile-field">
            <label>反馈类型</label>
            <select v-model="form.category">
              <option value="complaint">投诉</option>
              <option value="suggestion">建议</option>
              <option value="praise">表扬</option>
              <option value="other">其他</option>
            </select>
          </div>
          <div class="mobile-field">
            <label>评分</label>
            <select v-model="form.rating">
              <option :value="null">无需评分</option>
              <option v-for="score in [5,4,3,2,1]" :key="score" :value="score">{{ score }} 分</option>
            </select>
          </div>
        </div>

        <div class="mobile-field">
          <label>关联预约</label>
          <select v-model="form.reservationId">
            <option value="">不关联预约</option>
            <option v-for="item in reservationOptions" :key="item.id" :value="item.id">
              {{ reservationOptionLabel(item) }}
            </option>
          </select>
        </div>

        <div class="mobile-field">
          <label>反馈内容</label>
          <textarea v-model="form.content" placeholder="请描述你的意见、问题或表扬内容，管理员会尽快处理。" />
        </div>

        <div class="mobile-actions">
          <button type="button" class="mobile-outline-btn" @click="resetForm">清空</button>
          <button type="button" class="mobile-action-btn" @click="submitFeedback">提交反馈</button>
        </div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">我的反馈记录</div>
      <div class="mobile-list">
        <article v-for="item in feedbackList" :key="item.id" class="mobile-list-item">
          <div class="mobile-split">
            <div class="mobile-item-title">{{ categoryText(item.category) }}</div>
            <span class="mobile-badge" :class="statusBadgeClass(item.status)">{{ statusText(item.status) }}</span>
          </div>
          <div class="mobile-item-meta">{{ formatTime(item.createTime) }} · {{ item.roomLabel || '未关联自习室' }}</div>
          <div class="mobile-item-meta">{{ item.content || '（空）' }}</div>
          <div v-if="item.reply" class="mobile-inline-tip success">管理员回复：{{ item.reply }}</div>
        </article>
        <div v-if="!feedbackList.length" class="mobile-card mobile-empty">还没有反馈记录，欢迎提交你的建议。</div>
      </div>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import '../../components/mobile.css'
import { getLoginUser } from '../../components/mobileAuth'

function defaultForm () {
  return {
    category: 'suggestion',
    reservationId: '',
    rating: null,
    content: ''
  }
}

export default {
  name: 'UserMobileFeedback',
  data () {
    return {
      form: defaultForm(),
      reservationOptions: [],
      feedbackList: []
    }
  },
  mounted () {
    this.loadReservationOptions()
    this.loadFeedback()
  },
  methods: {
    async loadReservationOptions () {
      const user = getLoginUser()
      if (!user?.id) return
      const res = await request.get('/reservation/my', { params: { userId: user.id } })
      this.reservationOptions = res.code === 200 ? (res.data || []) : []
    },
    async loadFeedback () {
      const user = getLoginUser()
      if (!user?.id) return
      const res = await request.get('/feedback/my-page', {
        params: { userId: user.id, pageNum: 1, pageSize: 20 }
      })
      const data = res.data || {}
      this.feedbackList = Array.isArray(data.records) ? data.records : (Array.isArray(data) ? data : [])
    },
    resetForm () {
      this.form = defaultForm()
    },
    async submitFeedback () {
      const user = getLoginUser()
      if (!user?.id) return ElMessage.error('请先登录')
      if (!String(this.form.content || '').trim()) return ElMessage.warning('请填写反馈内容')
      const payload = {
        userId: user.id,
        category: this.form.category,
        rating: this.form.rating ? Number(this.form.rating) : null,
        content: String(this.form.content || '').trim(),
        reservationId: this.form.reservationId ? Number(this.form.reservationId) : null
      }
      const res = await request.post('/feedback/submit', payload)
      if (res.code === 200) {
        ElMessage.success('反馈已提交')
        this.resetForm()
        this.loadFeedback()
      } else {
        ElMessage.error(res.msg || '提交失败')
      }
    },
    statusText (status) {
      if (status === 'resolved') return '已解决'
      if (status === 'processing') return '处理中'
      return '待处理'
    },
    statusBadgeClass (status) {
      if (status === 'resolved') return 'success'
      if (status === 'processing') return 'warning'
      return 'danger'
    },
    categoryText (category) {
      return { complaint: '投诉', suggestion: '建议', praise: '表扬', other: '其他' }[category] || '其他'
    },
    timeText (value) {
      return value ? String(value).slice(0, 5) : '--:--'
    },
    formatTime (value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').slice(0, 16)
    },
    reservationOptionLabel (item) {
      const room = item.roomName || item.roomLabel || '自习室'
      return `${item.date} ${this.timeText(item.startTime)}-${this.timeText(item.endTime)} · ${room} · ${item.seatNo || '-'}号`
    }
  }
}
</script>