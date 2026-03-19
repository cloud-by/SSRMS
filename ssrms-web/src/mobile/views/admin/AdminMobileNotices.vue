<template>
  <div class="mobile-page">
    <div class="mobile-topbar">
      <div>
        <div class="mobile-topbar-title">公告管理</div>
        <div class="mobile-topbar-subtitle">移动端可查看最新公告，快速把握学生侧通知情况。</div>
      </div>
    </div>

    <section class="mobile-section">
      <div class="mobile-list">
        <div v-for="item in notices" :key="item.id" class="mobile-list-item">
          <div class="mobile-split">
            <div class="mobile-item-title">{{ item.title }}</div>
            <span class="mobile-badge" :class="badgeClass(item.level)">{{ item.level || '普通' }}</span>
          </div>
          <div class="mobile-item-meta">{{ item.targetText || '全部用户' }} · {{ formatTime(item.publishTime) }}</div>
          <div class="mobile-item-meta">{{ item.summary || item.content }}</div>
        </div>
        <div v-if="!notices.length" class="mobile-card mobile-empty">暂无公告。</div>
      </div>
    </section>
  </div>
</template>

<script>
import request from '@/utils/request'
import '../../components/mobile.css'

export default {
  name: 'AdminMobileNotices',
  data () {
    return { notices: [] }
  },
  mounted () {
    this.loadNotices()
  },
  methods: {
    async loadNotices () {
      const res = await request.get('/announcement/admin/page', { params: { pageNum: 1, pageSize: 20 } })
      this.notices = res.code === 200 ? (res.data || []) : []
    },
    badgeClass (level) {
      if (level === 'HIGH' || level === '重要') return 'danger'
      if (level === 'MEDIUM' || level === '提醒') return 'warning'
      return 'info'
    },
    formatTime (value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>