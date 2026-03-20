<template>
  <div class="mobile-page admin-mobile-page">
    <div class="mobile-topbar mobile-topbar-sticky">
      <div>
        <div class="mobile-topbar-title">投诉处理</div>
        <div class="mobile-topbar-subtitle">在手机上快速筛选待处理反馈，及时回复投诉、建议与异常评价。</div>
      </div>
      <button type="button" class="mobile-ghost-btn" @click="refreshAll">刷新</button>
    </div>

    <section class="mobile-section">
      <div class="mobile-grid-3 mobile-stat-grid-compact">
        <div class="mobile-stat mobile-stat-compact">
          <div class="mobile-stat-label">待处理</div>
          <div class="mobile-stat-value">{{ stats.pending ?? 0 }}</div>
        </div>
        <div class="mobile-stat mobile-stat-compact">
          <div class="mobile-stat-label">处理中</div>
          <div class="mobile-stat-value">{{ stats.processing ?? 0 }}</div>
        </div>
        <div class="mobile-stat mobile-stat-compact">
          <div class="mobile-stat-label">已解决</div>
          <div class="mobile-stat-value">{{ stats.resolved ?? 0 }}</div>
        </div>
      </div>
    </section>

    <div class="mobile-form-card mobile-filter-card">
      <div class="mobile-field">
        <label>关键词</label>
        <input v-model="query.keyword" type="text" placeholder="用户 / 学号 / 预约信息" @keyup.enter="loadFeedback(true)" />
      </div>
      <div class="mobile-grid-2">
        <div class="mobile-field">
          <label>状态</label>
          <select v-model="query.status">
            <option value="">全部</option>
            <option value="pending">待处理</option>
            <option value="processing">处理中</option>
            <option value="resolved">已解决</option>
          </select>
        </div>
        <div class="mobile-field">
          <label>类型</label>
          <select v-model="query.category">
            <option value="">全部</option>
            <option value="complaint">投诉</option>
            <option value="suggestion">建议</option>
            <option value="praise">表扬</option>
            <option value="other">其他</option>
          </select>
        </div>
      </div>
      <div class="mobile-switch-row">
        <label class="mobile-check">
          <input v-model="query.onlyPending" type="checkbox" />
          <span>仅看待处理</span>
        </label>
      </div>
      <div class="mobile-actions mobile-actions-between">
        <button type="button" class="mobile-outline-btn" @click="resetQuery">重置</button>
        <button type="button" class="mobile-action-btn" @click="loadFeedback(true)">查询</button>
      </div>
    </div>

    <section class="mobile-section">
      <div class="mobile-list">
        <article v-for="item in feedbackList" :key="item.id" class="mobile-list-item mobile-admin-card">
          <div class="mobile-split mobile-split-start">
            <div>
              <div class="mobile-item-title">#{{ item.id }} · {{ categoryText(item.category) }}</div>
              <div class="mobile-item-meta">{{ item.userName || '匿名用户' }} · {{ item.studentNo || '未提供学号' }}</div>
            </div>
            <span class="mobile-badge" :class="statusBadgeClass(item.status)">{{ statusText(item.status) }}</span>
          </div>

          <div class="mobile-chip-row mobile-chip-row-spacious">
            <span class="mobile-chip">{{ item.roomLabel || '未关联自习室' }}</span>
            <span class="mobile-chip mobile-chip-muted">{{ item.rating ? `评分 ${item.rating}` : '无评分' }}</span>
          </div>

          <div class="mobile-detail-list">
            <div class="mobile-detail-item">
              <span class="label">关联预约</span>
              <span class="value">{{ item.reservationLabel || (item.reservationId ? `预约ID ${item.reservationId}` : '未关联预约') }}</span>
            </div>
            <div class="mobile-detail-item">
              <span class="label">提交时间</span>
              <span class="value">{{ formatTime(item.createTime) }}</span>
            </div>
            <div class="mobile-detail-item" v-if="item.updateTime">
              <span class="label">更新时间</span>
              <span class="value">{{ formatTime(item.updateTime) }}</span>
            </div>
          </div>

          <div class="mobile-content-block">
            <div class="mobile-content-title">反馈内容</div>
            <div class="mobile-content-body">{{ item.content || '（空）' }}</div>
          </div>

          <div class="mobile-field mobile-field-inline">
            <label>处理回复</label>
            <textarea v-model="draftReply[item.id]" placeholder="填写处理意见或结果，回复会同步给用户。" />
          </div>

          <div class="mobile-actions">
            <button type="button" class="mobile-outline-btn" @click="submitHandle(item, 'processing')">标记处理中</button>
            <button type="button" class="mobile-action-btn" @click="submitHandle(item, 'resolved')">处理完成</button>
          </div>
        </article>

        <div v-if="!feedbackList.length && !loading" class="mobile-card mobile-empty">暂无投诉/反馈数据。</div>
        <div v-if="loading" class="mobile-card mobile-empty">正在加载反馈列表…</div>
      </div>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import '../../components/mobile.css'

export default {
  name: 'AdminMobileComplaints',
  data () {
    return {
      loading: false,
      stats: { pending: 0, processing: 0, resolved: 0 },
      query: {
        keyword: '',
        status: '',
        category: '',
        onlyPending: true
      },
      feedbackList: [],
      draftReply: {}
    }
  },
  mounted () {
    this.refreshAll()
  },
  methods: {
    async refreshAll () {
      await Promise.all([this.loadStats(), this.loadFeedback(true)])
    },
    resetQuery () {
      this.query = { keyword: '', status: '', category: '', onlyPending: true }
      this.loadFeedback(true)
    },
    async loadStats () {
      try {
        const res = await request.get('/feedback/admin/stats')
        if (res.code === 200 && res.data) {
          this.stats = {
            pending: Number(res.data.pending || 0),
            processing: Number(res.data.processing || 0),
            resolved: Number(res.data.resolved || 0)
          }
        }
      } catch (e) {
        this.stats = { pending: 0, processing: 0, resolved: 0 }
      }
    },
    async loadFeedback () {
      this.loading = true
      try {
        const params = {
          pageNum: 1,
          pageSize: 20
        }
        if (this.query.onlyPending) params.onlyPending = true
        if (this.query.status) params.status = this.query.status
        if (this.query.category) params.category = this.query.category
        if (this.query.keyword) params.keyword = this.query.keyword
        const res = await request.get('/feedback/admin/page', { params })
        if (res.code === 200) {
          const list = Array.isArray(res.data) ? res.data : []
          this.feedbackList = list
          const nextDraft = {}
          list.forEach(item => {
            nextDraft[item.id] = this.draftReply[item.id] ?? item.reply ?? ''
          })
          this.draftReply = nextDraft
        } else {
          this.feedbackList = []
          ElMessage.error(res.msg || '加载反馈失败')
        }
      } catch (e) {
        this.feedbackList = []
        ElMessage.error('加载反馈失败')
      } finally {
        this.loading = false
      }
    },
    async submitHandle (item, status) {
      const reply = String(this.draftReply[item.id] || '').trim()
      if (status === 'resolved' && !reply) {
        ElMessage.warning('请先填写处理回复，再标记为已解决')
        return
      }
      try {
        const res = await request.post('/feedback/admin/handle', {
          id: Number(item.id),
          status,
          reply,
          cancelReservation: false
        })
        if (res.code === 200) {
          ElMessage.success(status === 'resolved' ? '已完成处理' : '已标记处理中')
          this.refreshAll()
        } else {
          ElMessage.error(res.msg || '提交处理失败')
        }
      } catch (e) {
        ElMessage.error('提交处理失败')
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
      const value = String(category || '').toLowerCase()
      if (value === 'complaint') return '投诉'
      if (value === 'suggestion') return '建议'
      if (value === 'praise') return '表扬'
      return '其他'
    },
    formatTime (value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>