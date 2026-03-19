<template>
  <!-- initial-page 告诉 UserIndex：默认打开 user-main -->
  <MainIndex initial-page="user-main" />
</template>

<script>
import DesktopUserLayout from '@/desktop/layouts/DesktopUserLayout.vue'

export default {
  name: 'UserHomeView',
  components: { MainIndex: DesktopUserLayout }
}
</script>