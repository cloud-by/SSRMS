<template>
  <div class="mobile-shell">
    <main class="mobile-main">
      <router-view />
    </main>
    <MobileTabBar :items="tabItems" />
  </div>
</template>

<script>
import MobileTabBar from '../components/MobileTabBar.vue'

export default {
  name: 'MobileUserLayout',
  components: { MobileTabBar },
  data () {
    return {
      tabItems: [
        { label: '首页', icon: '🏠', path: '/m/user/home' },
        { label: '预约', icon: '🪑', path: '/m/user/reserve' },
        { label: '订单', icon: '🧾', path: '/m/user/orders' },
        { label: '反馈', icon: '💬', path: '/m/user/feedback' },
        { label: '我的', icon: '👤', path: '/m/user/me' }
      ]
    }
  }
}
</script>

<style scoped>
.mobile-shell {
  min-height: 100vh;
  background: linear-gradient(180deg, #f3f7ff 0%, #f7f8fc 36%, #f5f7fb 100%);
}

.mobile-main {
  min-height: 100vh;
  padding-bottom: calc(104px + env(safe-area-inset-bottom));
}
</style>