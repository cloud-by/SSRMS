<template>
  <nav class="mobile-tabbar">
    <button
        v-for="item in items"
        :key="item.path"
        type="button"
        class="tab-item"
        :class="{ active: isActive(item) }"
        @click="go(item.path)"
    >
      <span class="tab-icon">{{ item.icon }}</span>
      <span class="tab-label">{{ item.label }}</span>
    </button>
  </nav>
</template>

<script>
export default {
  name: 'MobileTabBar',
  props: {
    items: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    isActive (item) {
      return this.$route.path === item.path
    },
    go (path) {
      if (this.$route.path !== path) this.$router.push(path)
    }
  }
}
</script>

<style scoped>
.mobile-tabbar {
  position: sticky;
  bottom: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 4px;
  padding: 8px 8px calc(8px + env(safe-area-inset-bottom));
  border-top: 1px solid #e8edf5;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(16px);
}

.tab-item {
  border: none;
  background: transparent;
  padding: 6px 4px;
  border-radius: 12px;
  color: #70809a;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.tab-item.active {
  background: #eef4ff;
  color: #2f6bff;
  font-weight: 600;
}

.tab-icon {
  font-size: 18px;
  line-height: 1;
}

.tab-label {
  line-height: 1.1;
}
</style>