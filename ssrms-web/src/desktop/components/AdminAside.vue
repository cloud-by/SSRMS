<template>
  <aside class="side">
    <div class="sidebar-header">
      <div class="logo-icon admin-logo">
        <span>🛡</span>
      </div>
      <div class="logo-text">
        <div class="system-title">自习室预约系统</div>
        <div class="system-subtitle">管理员后台</div>
      </div>
    </div>

    <nav class="menu">
      <button
          v-for="item in navItems"
          :key="item.page"
          type="button"
          class="menu-item"
          :class="{ active: currentPage === item.page }"
          @click="emitChange(item.page)"
      >
        <span class="menu-icon">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </button>

    </nav>
  </aside>
</template>

<script>
import { adminDesktopNavItems } from '../config/adminNavigation'

export default {
  name: 'AdminAside',
  props: {
    currentPage: {
      type: String,
      required: true
    }
  },
  emits: ['change-page'],
  computed: {
    navItems () {
      return adminDesktopNavItems
    }
  },
  methods: {
    emitChange (page) {
      this.$emit('change-page', page)
    }
  }
}
</script>

<style scoped>
.side {
  width: 260px;
  background-color: #ffffff;
  box-shadow: 2px 0 8px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  padding: 24px 20px;
  box-sizing: border-box;
  z-index: 10;
}

.sidebar-header {
  display: flex;
  align-items: center;
  margin-bottom: 32px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 20px;
}

.admin-logo {
  background-color: #eff6ff;
  color: #1d4ed8;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.system-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.system-subtitle {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  border: none;
  background: transparent;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #4b5563;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, transform 0.1s ease;
  text-align: left;
}

.menu-item .menu-icon {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.menu-item.active {
  background-color: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}

.menu-item:hover {
  background-color: #f3f4f6;
  transform: translateX(1px);
}

@media (max-width: 900px) {
  .side {
    width: 100%;
    flex-direction: row;
    align-items: center;
    padding: 16px;
  }

  .sidebar-header {
    margin-bottom: 0;
    margin-right: 24px;
  }

  .menu {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .menu-item {
    flex: 1 0 auto;
  }
}
</style>