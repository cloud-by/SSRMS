<template>
  <div class="mobile-entry-page">
    <div class="mobile-entry-shell">
      <div class="mobile-entry-top">
        <div class="entry-brand">SSRMS Mobile</div>
        <button type="button" class="entry-desktop-btn" @click="goDesktop">桌面版</button>
      </div>

      <div class="mobile-entry-hero">
        <div class="entry-title">自习室预约移动端</div>
        <div class="entry-subtitle">
          补齐移动端用户与管理员入口，支持学生注册、双角色登录，并在登录后自动进入对应移动工作台。
        </div>

        <div class="entry-highlight-grid">
          <div class="entry-highlight-card">
            <div class="highlight-title">学生端</div>
            <div class="highlight-desc">预约、签到、查看预约、反馈投诉、个人资料维护。</div>
          </div>
          <div class="entry-highlight-card admin">
            <div class="highlight-title">管理员端</div>
            <div class="highlight-desc">预约处理、用户处置、投诉跟进与移动巡检工作台。</div>
          </div>
        </div>
      </div>

      <div class="entry-role-grid">
        <section class="entry-role-card">
          <div class="role-badge">用户入口</div>
          <h2>学生登录 / 注册</h2>
          <p>新用户可直接注册，登录后进入完整移动端预约链路。</p>
          <div class="role-actions">
            <button type="button" class="entry-primary" @click="goMobileLogin('student')">用户登录</button>
            <button type="button" class="entry-secondary" @click="goMobileRegister">用户注册</button>
          </div>
        </section>

        <section class="entry-role-card admin-card">
          <div class="role-badge admin">管理员入口</div>
          <h2>管理员登录</h2>
          <p>管理员使用现有后台账号登录，移动端聚焦值班处理与快速操作。</p>
          <div class="role-actions">
            <button type="button" class="entry-primary admin" @click="goMobileLogin('admin')">管理员登录</button>
            <button type="button" class="entry-secondary" @click="goDesktop">返回桌面后台</button>
          </div>
        </section>
      </div>

      <div class="entry-footer-note">建议在手机浏览器或开发者工具移动设备模式下访问，以获得最佳视觉效果。</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MobilePortal',
  methods: {
    goMobileLogin (role = 'student') {
      this.$router.push(`/m/login?mobile=1&mode=login&role=${role}`)
    },
    goMobileRegister () {
      this.$router.push('/m/login?mobile=1&mode=register&role=student')
    },
    goDesktop () {
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.mobile-entry-page {
  min-height: 100vh;
  padding: 24px 18px 36px;
  background:
      radial-gradient(circle at top left, rgba(80, 126, 255, 0.18), transparent 38%),
      radial-gradient(circle at bottom right, rgba(70, 208, 178, 0.16), transparent 30%),
      linear-gradient(180deg, #eef4ff 0%, #f6f8fc 50%, #eef2ff 100%);
}
.mobile-entry-shell {
  width: min(100%, 460px);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.mobile-entry-top { display:flex; justify-content:space-between; align-items:center; }
.entry-brand { padding: 7px 12px; border-radius:999px; background:#eaf1ff; color:#245dff; font-size:12px; font-weight:800; letter-spacing:.04em; }
.entry-desktop-btn { border:none; background:rgba(255,255,255,.78); color:#3056b8; border-radius:999px; padding:10px 14px; font-weight:600; box-shadow:0 10px 24px rgba(34,50,77,.08); }
.mobile-entry-hero,.entry-role-card { background:rgba(255,255,255,.88); backdrop-filter: blur(18px); border:1px solid rgba(226,233,246,.9); box-shadow:0 20px 42px rgba(45,68,123,.10); }
.mobile-entry-hero { border-radius:28px; padding:22px 20px; }
.entry-title { font-size:32px; line-height:1.15; font-weight:800; color:#20304a; }
.entry-subtitle { margin-top:12px; font-size:15px; line-height:1.8; color:#667892; }
.entry-highlight-grid { margin-top:18px; display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.entry-highlight-card { padding:14px; border-radius:20px; background:linear-gradient(180deg,#f8fbff 0%,#eef4ff 100%); }
.entry-highlight-card.admin { background:linear-gradient(180deg,#f8faff 0%,#eef0ff 100%); }
.highlight-title { font-size:14px; font-weight:700; color:#234169; }
.highlight-desc { margin-top:6px; font-size:12px; line-height:1.65; color:#6a7b94; }
.entry-role-grid { display:flex; flex-direction:column; gap:16px; }
.entry-role-card { border-radius:24px; padding:20px; }
.entry-role-card h2 { font-size:24px; color:#20304a; margin:10px 0 8px; }
.entry-role-card p { color:#6b7d96; line-height:1.75; font-size:14px; }
.role-badge { display:inline-flex; padding:6px 11px; border-radius:999px; background:#edf4ff; color:#2c68ff; font-size:12px; font-weight:700; }
.role-badge.admin { background:#eef0ff; color:#4b57d8; }
.role-actions { display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-top:18px; }
.entry-primary,.entry-secondary { min-height:50px; border:none; border-radius:16px; font-size:15px; font-weight:700; }
.entry-primary { background:linear-gradient(135deg,#2f6bff 0%,#427cff 100%); color:#fff; box-shadow:0 16px 30px rgba(47,107,255,.28); }
.entry-primary.admin { background:linear-gradient(135deg,#536dff 0%,#6a8dff 100%); }
.entry-secondary { background:#edf3ff; color:#2f6bff; }
.entry-footer-note { text-align:center; color:#8b98ac; font-size:12px; line-height:1.7; padding:0 12px; }
</style>