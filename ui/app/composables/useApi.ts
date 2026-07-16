import type { ApiResponse, PaginatedResponse } from '~/types'

export function useApi() {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiBaseUrl || ''

  function getAuthHeaders(): Record<string, string> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    
    let token: string | null = null
    
    if (import.meta.client) {
      // Client-side: try localStorage first, then cookie
      token = localStorage.getItem('auth_token')
      if (!token) {
        const cookie = useCookie('auth_token')
        token = cookie.value || null
      }
    } else {
      // Server-side: use cookie
      const cookie = useCookie('auth_token')
      token = cookie.value || null
    }
    
    if (token) {
      headers['Authorization'] = `Basic ${token}`
    }
    
    return headers
  }

  function buildUrl(path: string, params?: Record<string, string>): string {
    let url = `${baseUrl}/api/v1${path}`
    if (params && Object.keys(params).length) {
      url += `?${new URLSearchParams(params).toString()}`
    }
    return url
  }

  async function get<T>(path: string, params?: Record<string, string>): Promise<T> {
    const response = await $fetch<T>(buildUrl(path, params), {
      method: 'GET',
      headers: getAuthHeaders(),
    })

    return response
  }

  async function post<T>(path: string, body?: unknown): Promise<T> {
    const response = await $fetch<T>(buildUrl(path), {
      method: 'POST',
      headers: getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
    })

    return response
  }

  async function put<T>(path: string, body?: unknown): Promise<T> {
    const response = await $fetch<T>(buildUrl(path), {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
    })

    return response
  }

  async function del<T>(path: string): Promise<T> {
    const response = await $fetch<T>(buildUrl(path), {
      method: 'DELETE',
      headers: getAuthHeaders(),
    })

    return response
  }

  return { get, post, put, del }
}
