const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
    transpileDependencies: true,
    devServer: {
        client: {
            overlay: {
                errors: true,
                warnings: true,
                runtimeErrors: (error) => {
                    const message = String((error && error.message) || '')
                    if (message.includes('ResizeObserver loop completed with undelivered notifications')) {
                        return false
                    }
                    if (message.includes('ResizeObserver loop limit exceeded')) {
                        return false
                    }
                    return true
                }
            }
        },
        proxy: {
            '/api': {
                target: 'http://localhost:8090',
                changeOrigin: true,
                pathRewrite: { '^/api': '' }
            }
        }
    }
})