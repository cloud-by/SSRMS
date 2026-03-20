<template>
  <div class="mobile-page">
    <div class="mobile-hero">
      <div>{{ user.name || user.account || '管理员' }}</div>
      <div class="mobile-hero-subtitle">移动端用于值班处理与巡检记录。</div>
    </div>

    <section class="mobile-section">
      <div class="mobile-card">
        <div class="mobile-item-title">当前账号信息</div>
        <div class="mobile-item-meta">账号：{{ user.account || '-' }}</div>
        <div class="mobile-item-meta">角色：管理员</div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-card">
        <div class="mobile-item-title">操作建议</div>
        <div class="mobile-item-meta">• 使用“预约处理”快速补录签到或取消预约。</div>
        <div class="mobile-item-meta">• 使用“用户处理”快速处置异常账号。</div>
        <div class="mobile-item-meta">• 常用处理入口已保留在底部导航，适合手机端快速操作。</div>
        <div class="mobile-actions mobile-actions-between" style="margin-top:12px;">
          <button type="button" class="mobile-action-btn" @click="logout">退出登录</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import '../../components/mobile.css'
import { clearLoginUser, getLoginUser } from '../../components/mobileAuth'

export default {
  name: 'AdminMobileMe',
  data () {
    return { user: getLoginUser() || {} }
  },
  methods: {
    logout () {
      clearLoginUser()
      this.$router.replace('/m')
    }
  }
}
</script>