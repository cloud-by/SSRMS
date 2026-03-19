export function getLoginUser () {
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

export function saveLoginUser (user) {
    const raw = JSON.stringify(user)
    if (localStorage.getItem('ssrmsUser')) {
        localStorage.setItem('ssrmsUser', raw)
        sessionStorage.removeItem('ssrmsUser')
        return
    }
    if (sessionStorage.getItem('ssrmsUser')) {
        sessionStorage.setItem('ssrmsUser', raw)
        localStorage.removeItem('ssrmsUser')
        return
    }
    localStorage.setItem('ssrmsUser', raw)
}

export function clearLoginUser () {
    localStorage.removeItem('ssrmsUser')
    localStorage.removeItem('ssrmsToken')
    localStorage.removeItem('token')
    sessionStorage.removeItem('ssrmsUser')
    sessionStorage.removeItem('ssrmsToken')
    sessionStorage.removeItem('token')
}