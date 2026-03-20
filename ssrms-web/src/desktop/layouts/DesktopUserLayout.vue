<template>
  <div class="desktop-shell">
    <AppAside
        :current-page="currentPage"
        @change-page="changePage"
    />

    <div class="desktop-main">
      <AppHeader
          :current-page="currentPage"
          @logout="handleLogout"
      />

      <main class="desktop-content">
        <router-view v-slot="{ Component, route }">
          <component
              :is="Component"
              :current-page="route.meta.pageKey || currentPage"
              @change-page="changePage"
          />
        </router-view>
      </main>
    </div>
  </div>
</template>

<script>
import { ElMessageBox, ElMessage } from 'element-plus'
import UserHeader from '../components/UserHeader.vue'
import UserAside from '../components/UserAside.vue'
import { userDesktopNavItems } from '../config/userNavigation'

export default {
  name: 'DesktopUserLayout',
  components: {
    AppHeader: UserHeader,
    AppAside: UserAside
  },
  computed: {
    currentPage () {
      return this.$route.meta.pageKey || 'home'
    }
  },
  methods: {
    changePage (page) {
      const matchedItem = userDesktopNavItems.find(item => item.page === page)
      if (matchedItem && matchedItem.path !== this.$route.path) {
        this.$router.push(matchedItem.path)
      }
    },

    async handleLogout () {
      try {
        await ElMessageBox.confirm(
            '确定要退出登录吗？',
            '提示',
            {
              confirmButtonText: '退出',
              cancelButtonText: '取消',
              type: 'warning'
            }
        )

        localStorage.removeItem('ssrmsUser')
        localStorage.removeItem('ssrmsToken')
        localStorage.removeItem('token')
        sessionStorage.removeItem('ssrmsUser')
        sessionStorage.removeItem('ssrmsToken')
        sessionStorage.removeItem('token')

        ElMessage.success('已退出登录')
        this.$router.replace('/login')
      } catch (e) {
        // 用户取消退出时不做处理
      }
    }
  }
}
</script>

<style scoped>
.desktop-shell {
  min-height: 100vh;
  display: flex;
  background-color: #f5f7fb;
}

.desktop-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.desktop-content {
  flex: 1;
  display: flex;
  padding: 8px 24px 20px;
  box-sizing: border-box;
}

@media (max-width: 900px) {
  .desktop-shell {
    flex-direction: column;
  }

  .desktop-content {
    padding: 8px 12px 16px;
  }
}
</style>