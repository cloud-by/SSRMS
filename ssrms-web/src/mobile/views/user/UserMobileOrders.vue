<template>
  <div class="mobile-page">
    <div class="mobile-topbar">
      <div>
        <div class="mobile-topbar-title">我的预约</div>
        <div class="mobile-topbar-subtitle">查看待签到、进行中与历史预约，支持手机上一键处理。</div>
      </div>
      <button type="button" class="mobile-outline-btn" @click="loadOrders">刷新</button>
    </div>

    <div class="mobile-chip-row">
      <button v-for="item in tabs" :key="item.value" type="button" class="mobile-chip" :style="tab === item.value ? activeChipStyle : ''" @click="tab = item.value">{{ item.label }}（{{ countByTab(item.value) }}）</button>
    </div>

    <section class="mobile-section">
      <div class="mobile-list">
        <div v-for="item in filteredOrders" :key="item.id" class="mobile-list-item">
          <div class="mobile-split">
            <div class="mobile-item-title">{{ item.roomLabel || item.roomName }}</div>
            <span class="mobile-badge" :class="statusBadgeClass(item.status)">{{ statusText(item.status) }}</span>
          </div>
          <div class="mobile-item-meta">{{ item.date }} {{ timeText(item.startTime) }}-{{ timeText(item.endTime) }}</div>
          <div class="mobile-item-meta">座位 {{ item.seatNo || '未指定' }} · 预约号 {{ item.reservationNo }}</div>
          <div class="mobile-actions" style="margin-top:12px;">
            <button v-if="item.status === 'reserved'" type="button" class="mobile-action-btn" @click="checkin(item.id)">签到</button>
            <button v-if="item.status === 'reserved'" type="button" class="mobile-outline-btn" @click="cancel(item.id)">取消</button>
          </div>
        </div>
        <div v-if="!filteredOrders.length" class="mobile-card mobile-empty">当前筛选下暂无预约记录。</div>
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
  name: 'UserMobileOrders',
  data () {
    return {
      tab: 'pending',
      orders: [],
      tabs: [
        { value: 'pending', label: '待签到' },
        { value: 'active', label: '进行中' },
        { value: 'history', label: '历史记录' }
      ],
      activeChipStyle: 'background:#2f6bff;color:#fff;'
    }
  },
  computed: {
    filteredOrders () {
      return this.orders.filter(item => {
        if (this.tab === 'pending') return item.status === 'reserved'
        if (this.tab === 'active') return item.status === 'checked_in' || item.status === 'late'
        return ['cancelled', 'cancel_overdue', 'no_show'].includes(item.status)
      })
    }
  },
  mounted () {
    this.loadOrders()
  },
  methods: {
    async loadOrders () {
      const user = getLoginUser()
      if (!user || !user.id) return
      await request.post('/reservation/refreshNoShow', null, { params: { userId: user.id } })
      const res = await request.get('/reservation/my', { params: { userId: user.id } })
      this.orders = res.code === 200 ? (res.data || []) : []
    },
    countByTab (tab) {
      if (tab === 'pending') return this.orders.filter(item => item.status === 'reserved').length
      if (tab === 'active') return this.orders.filter(item => item.status === 'checked_in' || item.status === 'late').length
      return this.orders.filter(item => ['cancelled', 'cancel_overdue', 'no_show'].includes(item.status)).length
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
    },
    async checkin (id) {
      const res = await request.post(`/reservation/checkin/${id}`)
      if (res.code === 200) {
        ElMessage.success('签到成功')
        this.loadOrders()
      } else {
        ElMessage.error(res.msg || '签到失败')
      }
    },
    async cancel (id) {
      try {
        await ElMessageBox.confirm('确认取消这条预约吗？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      const res = await request.post(`/reservation/cancel/${id}`)
      if (res.code === 200) {
        ElMessage.success('已取消预约')
        this.loadOrders()
      } else {
        ElMessage.error(res.msg || '取消失败')
      }
    }
  }
}
</script>