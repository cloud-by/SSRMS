<template>
  <div class="mobile-page">
    <div class="mobile-topbar">
      <div>
        <div class="mobile-topbar-title">公告中心</div>
        <div class="mobile-topbar-subtitle">支持类型筛选，适合移动端快速浏览学校通知和规则公告。</div>
      </div>
    </div>

    <div class="mobile-chip-row">
      <button type="button" class="mobile-chip" :style="type === '' ? activeChipStyle : ''" @click="changeType('')">全部</button>
      <button v-for="item in typeOptions" :key="item.value" type="button" class="mobile-chip" :style="type === item.value ? activeChipStyle : ''" @click="changeType(item.value)">{{ item.label }}</button>
    </div>

    <section class="mobile-section">
      <div class="mobile-list">
        <div v-for="item in notices" :key="item.id" class="mobile-list-item">
          <div class="mobile-split">
            <div class="mobile-item-title">{{ item.title }}</div>
            <span class="mobile-badge" :class="badgeClass(item.level)">{{ item.level || '普通' }}</span>
          </div>
          <div class="mobile-item-meta">{{ item.summary || item.content }}</div>
          <div class="mobile-item-meta">{{ item.targetText || '全体学生' }} · {{ formatTime(item.publishTime) }}</div>
        </div>
        <div v-if="!notices.length" class="mobile-card mobile-empty">暂无符合条件的公告。</div>
      </div>
    </section>
  </div>
</template>

<script>
import request from '@/utils/request'
import '../../components/mobile.css'

export default {
  name: 'UserMobileNotices',
  data () {
    return {
      type: '',
      notices: [],
      activeChipStyle: 'background:#2f6bff;color:#fff;',
      typeOptions: [
        { label: '规则', value: 'RULE' },
        { label: '通知', value: 'NOTICE' },
        { label: '活动', value: 'ACTIVITY' }
      ]
    }
  },
  mounted () {
    this.loadNotices()
  },
  methods: {
    async loadNotices () {
      const res = await request.get('/announcement/page', {
        params: { roleId: 1, pageNum: 1, pageSize: 20, type: this.type || undefined }
      })
      this.notices = res.code === 200 ? (res.data || []) : []
    },
    changeType (value) {
      this.type = value
      this.loadNotices()
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