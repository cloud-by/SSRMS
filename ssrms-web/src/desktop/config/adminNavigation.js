export const adminDesktopNavItems = [
    { label: '后台首页', icon: '📊', page: 'admin-home', path: '/admin/home', title: '后台首页' },
    { label: '预约管理', icon: '📅', page: 'admin-reservations', path: '/admin/reservations', title: '预约管理' },
    { label: '用户管理', icon: '👥', page: 'admin-users', path: '/admin/users', title: '用户管理' },
    { label: '投诉处理', icon: '📢', page: 'admin-complaints', path: '/admin/complaints', title: '投诉处理' }
]

export const adminDesktopPageTitleMap = adminDesktopNavItems.reduce((map, item) => {
    map[item.page] = item.title
    return map
}, {})