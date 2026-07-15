import type { ApiResponse, PaginatedResponse } from '~/types'

export function useApi() {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiBaseUrl || ''

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
      headers: { 'Content-Type': 'application/json' },
    })

    return response
  }

  async function post<T>(path: string, body?: unknown): Promise<T> {
    const response = await $fetch<T>(buildUrl(path), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    })

    return response
  }

  async function put<T>(path: string, body?: unknown): Promise<T> {
    const response = await $fetch<T>(buildUrl(path), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    })

    return response
  }

  async function del<T>(path: string): Promise<T> {
    const response = await $fetch<T>(buildUrl(path), {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
    })

    return response
  }

  return { get, post, put, del }
}
