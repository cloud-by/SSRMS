<template>
  <div class="mobile-shell admin-shell">
    <main class="mobile-main">
      <router-view />
    </main>
    <MobileTabBar :items="tabItems" />
  </div>
</template>

<script>
import MobileTabBar from '../components/MobileTabBar.vue'

export default {
  name: 'MobileAdminLayout',
  components: { MobileTabBar },
  data () {
    return {
      tabItems: [
        { label: '工作台', icon: '📊', path: '/m/admin/home' },
        { label: '预约', icon: '✅', path: '/m/admin/reservations' },
        { label: '用户', icon: '👥', path: '/m/admin/users' },
        { label: '投诉', icon: '💬', path: '/m/admin/complaints' },
        { label: '我的', icon: '🛠️', path: '/m/admin/me' }
      ]
    }
  }
}
</script>

<style scoped>
.admin-shell {
  background: linear-gradient(180deg, #f6f8ff 0%, #f7f8fc 44%, #f5f7fb 100%);
}

.mobile-main {
  min-height: 100vh;
  padding-bottom: calc(104px + env(safe-area-inset-bottom));
}
</style>