export function getLoginUser () {
    const raw = localStorage.getItem('ssrmsUser')
    if (!raw) return null
    try {
        return JSON.parse(raw)
    } catch (e) {
        localStorage.removeItem('ssrmsUser')
        return null
    }
}

export function clearLoginUser () {
    localStorage.removeItem('ssrmsUser')
    localStorage.removeItem('ssrmsToken')
    localStorage.removeItem('token')
    sessionStorage.removeItem('ssrmsUser')
    sessionStorage.removeItem('ssrmsToken')
    sessionStorage.removeItem('token')
}