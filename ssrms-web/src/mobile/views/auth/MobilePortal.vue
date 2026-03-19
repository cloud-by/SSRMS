<template>
  <div class="mobile-portal">
    <div class="portal-card">
      <div class="portal-brand">SSRMS Mobile</div>
      <h1 class="portal-title">自习室预约移动端</h1>
      <p class="portal-desc">用户端提供完整预约链路，管理员端提供轻量工作台。登录成功后会按角色自动进入对应移动页面。</p>

      <div class="portal-actions">
        <button type="button" class="portal-primary" @click="goLogin">进入登录</button>
        <button type="button" class="portal-secondary" @click="goDesktop">返回桌面版</button>
      </div>

      <div class="portal-note">建议在手机浏览器或开发者工具移动设备模式下访问。</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MobilePortal',
  methods: {
    goLogin () {
      this.$router.push('/login?mobile=1')
    },
    goDesktop () {
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.mobile-portal {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(160deg, #eef4ff 0%, #f7f8fc 55%, #eef4ff 100%);
}
.portal-card {
  max-width: 420px;
  width: 100%;
  background: #fff;
  border-radius: 28px;
  padding: 28px 22px;
  box-shadow: 0 18px 42px rgba(27, 39, 94, 0.09);
}
.portal-brand {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: #edf3ff;
  color: #2f6bff;
  font-size: 12px;
  font-weight: 700;
}
.portal-title {
  margin-top: 14px;
  font-size: 28px;
  color: #22324d;
}
.portal-desc {
  margin-top: 10px;
  color: #63748c;
  line-height: 1.7;
}
.portal-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 26px;
}
.portal-primary,
.portal-secondary {
  border-radius: 14px;
  border: none;
  padding: 14px 16px;
  font-size: 15px;
}
.portal-primary { background: #2f6bff; color: #fff; }
.portal-secondary { background: #eef4ff; color: #2f6bff; }
.portal-note {
  margin-top: 18px;
  font-size: 12px;
  color: #8a98ad;
}
</style>