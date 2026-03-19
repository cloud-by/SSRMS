<template>
  <!-- initial-page 告诉 UserIndex：默认打开 admin-main -->
  <MainIndex initial-page="admin-main" />
</template>

<script>
import DesktopAdminLayout from '@/desktop/layouts/DesktopAdminLayout.vue'

export default {
  name: 'AdminHomeView',
  components: { MainIndex: DesktopAdminLayout }
}
</script>