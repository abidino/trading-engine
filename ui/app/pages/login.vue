<script setup lang="ts">
definePageMeta({
  layout: false,
})

const config = useRuntimeConfig()
const router = useRouter()

const form = reactive({
  username: '',
  password: '',
})

const loading = ref(false)
const error = ref('')
const recaptchaLoaded = ref(false)
const recaptchaToken = ref('')

const recaptchaSiteKey = config.public.recaptchaSiteKey
const recaptchaEnabled = config.public.recaptchaEnabled

// Load reCAPTCHA script
onMounted(() => {
  if (recaptchaEnabled && recaptchaSiteKey) {
    const script = document.createElement('script')
    script.src = `https://www.google.com/recaptcha/api.js?render=${recaptchaSiteKey}`
    script.async = true
    script.defer = true
    script.onload = () => {
      recaptchaLoaded.value = true
    }
    document.head.appendChild(script)
  }

  // Check if already logged in
  const token = localStorage.getItem('auth_token') || useCookie('auth_token').value
  if (token) {
    router.push('/')
  }
})

async function handleSubmit() {
  error.value = ''
  loading.value = true

  try {
    // Get reCAPTCHA token if enabled
    if (recaptchaEnabled && recaptchaSiteKey && recaptchaLoaded.value) {
      try {
        recaptchaToken.value = await (window as any).grecaptcha.execute(recaptchaSiteKey, {
          action: 'login',
        })
      } catch (e) {
        console.error('reCAPTCHA error:', e)
        error.value = 'reCAPTCHA verification failed'
        loading.value = false
        return
      }
    }

    const api = useApi()
    const response = await api.post<{ message: string; username: string }>('/auth/login', {
      username: form.username,
      password: form.password,
      recaptchaToken: recaptchaToken.value,
    })

    if (response && response.username) {
      // Create Basic Auth token
      const token = btoa(`${form.username}:${form.password}`)
      
      // Save to localStorage (client-side)
      localStorage.setItem('auth_token', token)
      localStorage.setItem('username', response.username)
      
      // Save to cookie (for SSR)
      const authCookie = useCookie('auth_token', {
        maxAge: 60 * 60 * 24 * 7, // 7 days
        path: '/',
        sameSite: 'lax',
        secure: false, // Set to true in production with HTTPS
      })
      authCookie.value = token
      
      const usernameCookie = useCookie('username', {
        maxAge: 60 * 60 * 24 * 7,
        path: '/',
        sameSite: 'lax',
        secure: false,
      })
      usernameCookie.value = response.username

      // Redirect to home
      router.push('/')
    }
  } catch (e: any) {
    error.value = e.message || 'Login failed. Please check your credentials.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900">
    <div class="w-full max-w-md px-6">
      <!-- Logo/Title -->
      <div class="mb-8 text-center">
        <h1 class="mb-2 text-4xl font-bold text-white">Trading Engine</h1>
        <p class="text-gray-400">Sign in to continue</p>
      </div>

      <!-- Login Card -->
      <div class="card">
        <form class="space-y-6" @submit.prevent="handleSubmit">
          <!-- Username -->
          <div>
            <label for="username" class="mb-2 block text-sm font-medium text-gray-300">
              Username
            </label>
            <input
              id="username"
              v-model="form.username"
              type="text"
              required
              autocomplete="username"
              class="w-full rounded-lg border border-gray-700 bg-gray-800 px-4 py-3 text-white placeholder-gray-500 transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              placeholder="Enter your username"
            />
          </div>

          <!-- Password -->
          <div>
            <label for="password" class="mb-2 block text-sm font-medium text-gray-300">
              Password
            </label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              required
              autocomplete="current-password"
              class="w-full rounded-lg border border-gray-700 bg-gray-800 px-4 py-3 text-white placeholder-gray-500 transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              placeholder="Enter your password"
            />
          </div>

          <!-- Error Message -->
          <div v-if="error" class="rounded-lg bg-red-500/10 border border-red-500/20 px-4 py-3 text-sm text-red-400">
            {{ error }}
          </div>

          <!-- reCAPTCHA Badge Info -->
          <div v-if="recaptchaEnabled && recaptchaSiteKey" class="text-xs text-gray-500">
            This site is protected by reCAPTCHA and the Google
            <a href="https://policies.google.com/privacy" target="_blank" class="text-blue-400 hover:underline">Privacy Policy</a> and
            <a href="https://policies.google.com/terms" target="_blank" class="text-blue-400 hover:underline">Terms of Service</a> apply.
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            :disabled="loading"
            class="w-full rounded-lg bg-blue-600 px-4 py-3 font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <span v-if="loading" class="flex items-center justify-center gap-2">
              <div class="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
              Signing in...
            </span>
            <span v-else>Sign In</span>
          </button>
        </form>
      </div>

      <!-- Footer -->
      <div class="mt-6 text-center text-sm text-gray-500">
        <p>Environment: {{ config.public.apiBaseUrl }}</p>
      </div>
    </div>
  </div>
</template>
