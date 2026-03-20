export const userDesktopNavItems = [
    { label: '首页', icon: '🏠', page: 'home', path: '/user/home', title: '首页' },
    { label: '我要预约', icon: '🗓', page: 'user-reserve', path: '/user/reserve', title: '我要预约' },
    { label: '我的预约', icon: '📋', page: 'user-reservations', path: '/user/reservations', title: '我的预约' },
    { label: '评价与投诉', icon: '💬', page: 'user-feedback', path: '/user/feedback', title: '评价与投诉' },
    { label: '个人中心', icon: '👤', page: 'user-profile', path: '/user/profile', title: '个人中心' }
]

export const userDesktopPageTitleMap = userDesktopNavItems.reduce((map, item) => {
    map[item.page] = item.title
    return map
}, {})