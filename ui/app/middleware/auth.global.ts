export default defineNuxtRouteMiddleware((to) => {
  // Public routes that don't require authentication
  const publicRoutes = ['/login']
  
  if (publicRoutes.includes(to.path)) {
    return
  }

  // Check authentication token
  let hasToken = false
  
  if (import.meta.client) {
    // Client-side: check localStorage
    hasToken = !!localStorage.getItem('auth_token')
  } else {
    // Server-side: check cookie
    const cookie = useCookie('auth_token')
    hasToken = !!cookie.value
  }

  if (!hasToken) {
    return navigateTo('/login')
  }
})
