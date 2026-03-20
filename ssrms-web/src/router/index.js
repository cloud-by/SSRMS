// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import DesktopPortal from '@/desktop/view/auth/DesktopPortal.vue'
import DesktopUserLayout from '@/desktop/layouts/DesktopUserLayout.vue'
import DesktopAdminLayout from '@/desktop/layouts/DesktopAdminLayout.vue'
import MobilePortal from '@/mobile/views/auth/MobilePortal.vue'
import MobileUserLayout from '@/mobile/layouts/MobileUserLayout.vue'
import MobileAdminLayout from '@/mobile/layouts/MobileAdminLayout.vue'
import UserMobileHome from '@/mobile/views/user/UserMobileHome.vue'
import UserMobileReserve from '@/mobile/views/user/UserMobileReserve.vue'
import UserMobileOrders from '@/mobile/views/user/UserMobileOrders.vue'
import UserMobileNotices from '@/mobile/views/user/UserMobileNotices.vue'
import UserMobileFeedback from '@/mobile/views/user/UserMobileFeedback.vue'
import UserMobileMe from '@/mobile/views/user/UserMobileMe.vue'
import AdminMobileHome from '@/mobile/views/admin/AdminMobileHome.vue'
import AdminMobileReservations from '@/mobile/views/admin/AdminMobileReservations.vue'
import AdminMobileUsers from '@/mobile/views/admin/AdminMobileUsers.vue'
import AdminMobileNotices from '@/mobile/views/admin/AdminMobileNotices.vue'
import AdminMobileComplaints from '@/mobile/views/admin/AdminMobileComplaints.vue'
import AdminMobileMe from '@/mobile/views/admin/AdminMobileMe.vue'

const routes = [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'Login', component: DesktopPortal },
    { path: '/m', name: 'MobilePortal', component: MobilePortal },
    { path: '/m/login', name: 'MobileLogin', component: DesktopPortal },
    {
        path: '/user',
        component: DesktopUserLayout,
        meta: {
            requiresAuth: true,
            role: 'user'
        },
        children: [
            { path: '', redirect: '/user/home' },
            { path: 'home', name: 'UserHome', component: () => import('@/desktop/view/user/UserDesktopHome.vue'), meta: { requiresAuth: true, role: 'user', pageKey: 'home' } },
            { path: 'reserve', name: 'UserReserve', component: () => import('@/desktop/view/user/UserDesktopHome.vue'), meta: { requiresAuth: true, role: 'user', pageKey: 'user-reserve' } },
            { path: 'reservations', name: 'UserReservations', component: () => import('@/desktop/view/user/UserDesktopHome.vue'), meta: { requiresAuth: true, role: 'user', pageKey: 'user-reservations' } },
            { path: 'violations', name: 'UserViolations', component: () => import('@/desktop/view/user/UserDesktopHome.vue'), meta: { requiresAuth: true, role: 'user', pageKey: 'user-violations' } },
            { path: 'feedback', name: 'UserFeedback', component: () => import('@/desktop/view/user/UserDesktopHome.vue'), meta: { requiresAuth: true, role: 'user', pageKey: 'user-feedback' } },
            { path: 'profile', name: 'UserProfile', component: () => import('@/desktop/view/user/UserDesktopHome.vue'), meta: { requiresAuth: true, role: 'user', pageKey: 'user-profile' } }
        ]
    },
    {
        path: '/admin',
        component: DesktopAdminLayout,
        meta: {
            requiresAuth: true,
            role: 'admin'
        },
        children: [
            { path: '', redirect: '/admin/home' },
            { path: 'home', name: 'AdminHome', component: () => import('@/desktop/view/admin/AdminDesktopHome.vue'), meta: { requiresAuth: true, role: 'admin', pageKey: 'admin-home' } },
            { path: 'reservations', name: 'AdminReservations', component: () => import('@/desktop/view/admin/AdminDesktopHome.vue'), meta: { requiresAuth: true, role: 'admin', pageKey: 'admin-reservations' } },
            { path: 'users', name: 'AdminUsers', component: () => import('@/desktop/view/admin/AdminDesktopHome.vue'), meta: { requiresAuth: true, role: 'admin', pageKey: 'admin-users' } },
            { path: 'complaints', name: 'AdminComplaints', component: () => import('@/desktop/view/admin/AdminDesktopHome.vue'), meta: { requiresAuth: true, role: 'admin', pageKey: 'admin-complaints' } }
        ]
    },
    {
        path: '/m/user',
        component: MobileUserLayout,
        meta: { requiresAuth: true, role: 'user', mobile: true },
        children: [
            { path: '', redirect: '/m/user/home' },
            { path: 'home', name: 'MobileUserHome', component: UserMobileHome, meta: { requiresAuth: true, role: 'user', mobile: true } },
            { path: 'reserve', name: 'MobileUserReserve', component: UserMobileReserve, meta: { requiresAuth: true, role: 'user', mobile: true } },
            { path: 'orders', name: 'MobileUserOrders', component: UserMobileOrders, meta: { requiresAuth: true, role: 'user', mobile: true } },
            { path: 'feedback', name: 'MobileUserFeedback', component: UserMobileFeedback, meta: { requiresAuth: true, role: 'user', mobile: true } },
            { path: 'notices', name: 'MobileUserNotices', component: UserMobileNotices, meta: { requiresAuth: true, role: 'user', mobile: true } },
            { path: 'me', name: 'MobileUserMe', component: UserMobileMe, meta: { requiresAuth: true, role: 'user', mobile: true } }
        ]
    },
    {
        path: '/m/admin',
        component: MobileAdminLayout,
        meta: { requiresAuth: true, role: 'admin', mobile: true },
        children: [
            { path: '', redirect: '/m/admin/home' },
            { path: 'home', name: 'MobileAdminHome', component: AdminMobileHome, meta: { requiresAuth: true, role: 'admin', mobile: true } },
            { path: 'reservations', name: 'MobileAdminReservations', component: AdminMobileReservations, meta: { requiresAuth: true, role: 'admin', mobile: true } },
            { path: 'users', name: 'MobileAdminUsers', component: AdminMobileUsers, meta: { requiresAuth: true, role: 'admin', mobile: true } },
            { path: 'complaints', name: 'MobileAdminComplaints', component: AdminMobileComplaints, meta: { requiresAuth: true, role: 'admin', mobile: true } },
            { path: 'notices', name: 'MobileAdminNotices', component: AdminMobileNotices, meta: { requiresAuth: true, role: 'admin', mobile: true } },
            { path: 'me', name: 'MobileAdminMe', component: AdminMobileMe, meta: { requiresAuth: true, role: 'admin', mobile: true } }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

function getLoginUser () {
    const raw = localStorage.getItem('ssrmsUser') || sessionStorage.getItem('ssrmsUser')
    if (!raw) return null
    try {
        return JSON.parse(raw)
    } catch (e) {
        localStorage.removeItem('ssrmsUser')
        sessionStorage.removeItem('ssrmsUser')
        return null
    }
}

function homePathByRole (user, mobile) {
    const isAdmin = user && Number(user.roleId) === 0
    if (mobile) return isAdmin ? '/m/admin/home' : '/m/user/home'
    return isAdmin ? '/admin/home' : '/user/home'
}

router.beforeEach((to, from, next) => {
    const user = getLoginUser()

    if (to.meta && to.meta.requiresAuth) {
        if (!user) {
            return next({
                path: '/login',
                query: {
                    redirect: to.fullPath,
                    ...(to.meta.mobile ? { mobile: '1' } : {})
                }
            })
        }

        if (to.meta.role === 'admin' && Number(user.roleId) !== 0) {
            return next(homePathByRole(user, Boolean(to.meta.mobile)))
        }
        if (to.meta.role === 'user' && Number(user.roleId) !== 1) {
            return next(homePathByRole(user, Boolean(to.meta.mobile)))
        }
    }

    if (to.path === '/login' && user) {
        return next(homePathByRole(user, to.query.mobile === '1'))
    }

    next()
})

export default router