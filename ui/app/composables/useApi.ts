import type { ApiResponse, PaginatedResponse } from '~/types'

export function useApi() {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiBaseUrl

  async function get<T>(path: string, params?: Record<string, string>): Promise<T> {
    const url = new URL(`/api/v1${path}`, baseUrl)
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        url.searchParams.set(key, value)
      })
    }

    const response = await $fetch<T>(url.toString(), {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    })

    return response
  }

  async function post<T>(path: string, body?: unknown): Promise<T> {
    const url = `${baseUrl}/api/v1${path}`

    const response = await $fetch<T>(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    })

    return response
  }

  async function put<T>(path: string, body?: unknown): Promise<T> {
    const url = `${baseUrl}/api/v1${path}`

    const response = await $fetch<T>(url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    })

    return response
  }

  async function del<T>(path: string): Promise<T> {
    const url = `${baseUrl}/api/v1${path}`

    const response = await $fetch<T>(url, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
    })

    return response
  }

  return { get, post, put, del }
}
