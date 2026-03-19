<template>
  <div class="mobile-page">
    <div class="mobile-hero">
      <div>{{ profile.name || profile.account || '未登录用户' }}</div>
      <div class="mobile-hero-subtitle">学号 {{ profile.studentNo || '-' }} · 常用校区 {{ profile.commonCampus || '未设置' }}</div>
    </div>

    <section class="mobile-section">
      <div class="mobile-grid-2">
        <div class="mobile-stat">
          <div class="mobile-stat-label">信用分</div>
          <div class="mobile-stat-value">{{ profile.creditScore ?? '--' }}</div>
          <div class="mobile-stat-desc">状态 {{ blacklistText }}</div>
        </div>
        <div class="mobile-stat">
          <div class="mobile-stat-label">最近登录</div>
          <div class="mobile-stat-value" style="font-size:18px;">{{ shortTime(profile.lastLoginTime) }}</div>
          <div class="mobile-stat-desc">IP {{ profile.lastLoginIp || '-' }}</div>
        </div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-title">个人资料</div>
      <div class="mobile-form-card">
        <div class="mobile-field"><label>昵称</label><input v-model="form.name" type="text" /></div>
        <div class="mobile-field"><label>学号</label><input v-model="form.studentNo" type="text" /></div>
        <div class="mobile-field"><label>手机号</label><input v-model="form.phone" type="text" /></div>
        <div class="mobile-field"><label>邮箱</label><input v-model="form.email" type="email" /></div>
        <div class="mobile-field"><label>学院</label><input v-model="form.college" type="text" /></div>
        <div class="mobile-field"><label>班级</label><input v-model="form.gradeClass" type="text" /></div>
        <div class="mobile-field"><label>常用校区</label><input v-model="form.commonCampus" type="text" /></div>
        <div class="mobile-field"><label>备注</label><textarea v-model="form.profileRemark"></textarea></div>
        <div class="mobile-actions">
          <button type="button" class="mobile-action-btn" @click="saveProfile">保存资料</button>
          <button type="button" class="mobile-outline-btn" @click="logout">退出登录</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import '../../components/mobile.css'
import { clearLoginUser, getLoginUser, saveLoginUser } from '../../components/mobileAuth'

export default {
  name: 'UserMobileMe',
  data () {
    return {
      profile: {},
      form: {
        id: null,
        name: '',
        studentNo: '',
        phone: '',
        email: '',
        college: '',
        gradeClass: '',
        commonCampus: '',
        profileRemark: ''
      }
    }
  },
  computed: {
    blacklistText () {
      const flag = Number(this.profile.blacklistFlag || 0)
      if (flag === 2) return '黑名单'
      if (flag === 1) return '预警'
      return '正常'
    }
  },
  mounted () {
    this.loadProfile()
  },
  methods: {
    async loadProfile () {
      const user = getLoginUser()
      if (!user || !user.id) return
      const res = await request.get('/user/profile', { params: { userId: user.id } })
      if (res.code !== 200) return
      this.profile = res.data || {}
      this.form = {
        id: this.profile.id,
        name: this.profile.name || '',
        studentNo: this.profile.studentNo || '',
        phone: this.profile.phone || '',
        email: this.profile.email || '',
        college: this.profile.college || '',
        gradeClass: this.profile.gradeClass || '',
        commonCampus: this.profile.commonCampus || '',
        profileRemark: this.profile.profileRemark || ''
      }
    },
    shortTime (value) {
      if (!value) return '暂无'
      return String(value).replace('T', ' ').slice(0, 16)
    },
    async saveProfile () {
      const res = await request.post('/user/profile', this.form)
      if (res.code === 200) {
        ElMessage.success('保存成功')
        const loginUser = getLoginUser() || {}
        saveLoginUser({ ...loginUser, ...res.data })
        this.loadProfile()
      } else {
        ElMessage.error(res.msg || '保存失败')
      }
    },
    logout () {
      clearLoginUser()
      this.$router.replace('/login')
    }
  }
}
</script>