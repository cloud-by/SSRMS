<template>
  <div class="mobile-page admin-mobile-page">
    <div class="mobile-topbar mobile-topbar-sticky">
      <div>
        <div class="mobile-topbar-title">用户处理</div>
        <div class="mobile-topbar-subtitle">支持搜索异常用户、查看信用分与黑名单状态，并执行快捷处置。</div>
      </div>
    </div>

    <div class="mobile-form-card mobile-filter-card">
      <div class="mobile-field"><label>关键词</label><input v-model="query.keyword" type="text" placeholder="姓名 / 学号 / 账号" /></div>
      <div class="mobile-grid-2">
        <div class="mobile-field"><label>角色</label><select v-model="query.roleId"><option value="">全部</option><option value="1">学生</option><option value="0">管理员</option></select></div>
        <div class="mobile-field"><label>状态</label><select v-model="query.status"><option value="">全部</option><option value="1">预警</option><option value="2">黑名单</option></select></div>
      </div>
      <div class="mobile-actions mobile-actions-between"><button type="button" class="mobile-outline-btn" @click="resetQuery">重置</button><button type="button" class="mobile-action-btn" @click="loadUsers">查询</button></div>
    </div>

    <section class="mobile-section">
      <div class="mobile-chip-row mobile-chip-row-spacious">
        <span class="mobile-chip">筛选结果 {{ users.length }} 人</span>
        <span class="mobile-chip mobile-chip-muted">支持快速黑名单处置</span>
      </div>
      <div class="mobile-list">
        <div v-for="item in users" :key="item.id" class="mobile-list-item mobile-admin-card">
          <div class="mobile-split">
            <div class="mobile-item-title">{{ item.name || item.account }}</div>
            <span class="mobile-badge" :class="statusBadgeClass(item.status)">{{ statusText(item.status) }}</span>
          </div>
          <div class="mobile-chip-row mobile-chip-row-spacious">
            <span class="mobile-chip">{{ Number(item.roleId) === 0 ? '管理员' : '学生' }}</span>
            <span class="mobile-chip mobile-chip-muted">{{ item.college || '未设置院系' }}</span>
          </div>
          <div class="mobile-detail-list">
            <div class="mobile-detail-item"><span class="label">账号</span><span class="value">{{ item.account || '-' }}</span></div>
            <div class="mobile-detail-item"><span class="label">学号</span><span class="value">{{ item.studentNo || '-' }}</span></div>
            <div class="mobile-detail-item"><span class="label">信用分</span><span class="value">{{ item.creditScore ?? '--' }}</span></div>
            <div class="mobile-detail-item"><span class="label">违约次数</span><span class="value">{{ item.violationCount ?? 0 }} 次</span></div>
          </div>
          <div class="mobile-actions" style="margin-top:12px;">
            <button v-if="Number(item.status) !== 2 && Number(item.roleId) !== 0" type="button" class="mobile-action-btn" @click="toggleBlacklist(item, true)">加入黑名单</button>
            <button v-if="Number(item.status) === 2 && Number(item.roleId) !== 0" type="button" class="mobile-outline-btn" @click="toggleBlacklist(item, false)">解除黑名单</button>
          </div>
        </div>
        <div v-if="!users.length" class="mobile-card mobile-empty">暂无用户数据。</div>
      </div>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import '../../components/mobile.css'

export default {
  name: 'AdminMobileUsers',
  data () {
    return {
      query: { keyword: '', roleId: '', status: '' },
      users: []
    }
  },
  mounted () {
    this.loadUsers()
  },
  methods: {
    resetQuery () {
      this.query = { keyword: '', roleId: '', status: '' }
      this.loadUsers()
    },
    async loadUsers () {
      const res = await request.get('/user/admin/page', {
        params: {
          page: 1,
          size: 20,
          keyword: this.query.keyword || undefined,
          roleId: this.query.roleId === '' ? undefined : Number(this.query.roleId),
          status: this.query.status === '' ? undefined : Number(this.query.status)
        }
      })
      const data = res.data || {}
      this.users = Array.isArray(data.records) ? data.records : []
    },
    statusText (status) {
      if (Number(status) === 2) return '黑名单'
      if (Number(status) === 1) return '预警'
      return '正常'
    },
    statusBadgeClass (status) {
      if (Number(status) === 2) return 'danger'
      if (Number(status) === 1) return 'warning'
      return 'success'
    },
    async toggleBlacklist (item, black) {
      const url = black ? `/user/admin/blacklist/${item.id}` : `/user/admin/unblacklist/${item.id}`
      const res = await request.post(url)
      if (res.code === 200) {
        ElMessage.success(black ? '已加入黑名单' : '已解除黑名单')
        this.loadUsers()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    }
  }
}
</script>