import type { WatchlistItem } from '~/types'

export function useWatchlist() {
  const api = useApi()

  const items = ref<WatchlistItem[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchWatchlist() {
    loading.value = true
    error.value = null
    try {
      items.value = await api.get<WatchlistItem[]>('/watchlist')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch watchlist'
    } finally {
      loading.value = false
    }
  }

  async function addToWatchlist(ticker: string) {
    try {
      await api.post('/watchlist', { ticker: ticker.toUpperCase() })
      await fetchWatchlist()
    } catch (e: any) {
      error.value = e.message || 'Failed to add to watchlist'
      throw e
    }
  }

  async function removeFromWatchlist(id: string) {
    try {
      await api.del(`/watchlist/${id}`)
      items.value = items.value.filter(i => i.id !== id)
    } catch (e: any) {
      error.value = e.message || 'Failed to remove from watchlist'
      throw e
    }
  }

  async function updateTargetPrice(id: string, price: number) {
    try {
      await api.put(`/watchlist/${id}/target-price`, { price })
      await fetchWatchlist()
    } catch (e: any) {
      error.value = e.message || 'Failed to update target price'
      throw e
    }
  }

  async function approveItem(id: string) {
    try {
      await api.post(`/watchlist/${id}/approve`)
      items.value = items.value.map(i => (i.id === id ? { ...i, approved: true } : i))
    } catch (e: any) {
      error.value = e.message || 'Failed to approve item'
      throw e
    }
  }

  async function analyzeItem(ticker: string) {
    try {
      await api.post(`/watchlist/${ticker}/analyze`)
    } catch (e: any) {
      error.value = e.message || 'Failed to request analysis'
      throw e
    }
  }

  return {
    items: readonly(items),
    loading: readonly(loading),
    error: readonly(error),
    fetchWatchlist,
    addToWatchlist,
    removeFromWatchlist,
    updateTargetPrice,
    approveItem,
    analyzeItem,
  }
}
