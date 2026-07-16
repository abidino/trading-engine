import type { Position, Transaction, PortfolioSummaryStats, SoldPosition } from '~/types'

export function usePortfolio() {
  const api = useApi()

  const positions = ref<Position[]>([])
  const soldPositions = ref<SoldPosition[]>([])
  const transactions = ref<Transaction[]>([])
  const summary = ref<PortfolioSummaryStats | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchPositions() {
    loading.value = true
    error.value = null
    try {
      positions.value = await api.get<Position[]>('/portfolio/positions')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch positions'
    } finally {
      loading.value = false
    }
  }

  // Best-effort: refresh live quotes for held tickers (called on page load).
  async function refreshQuotes() {
    try {
      await api.post('/portfolio/refresh')
    } catch {
      // Non-fatal — fall back to last known prices.
    }
  }

  // Refresh live quote for a specific ticker and update positions.
  async function refreshTickerQuote(ticker: string) {
    try {
      const updatedPosition = await api.post<Position>(`/portfolio/refresh/${ticker}`)
      // Update the position in the local state
      const index = positions.value.findIndex(p => p.ticker === ticker)
      if (index !== -1) {
        positions.value[index] = updatedPosition
      }
    } catch (e: any) {
      error.value = e.message || 'Failed to refresh quote'
      throw e
    }
  }

  async function fetchSoldPositions() {
    try {
      soldPositions.value = await api.get<SoldPosition[]>('/portfolio/positions/closed')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch sold positions'
    }
  }

  async function fetchTransactions() {
    try {
      transactions.value = await api.get<Transaction[]>('/portfolio/transactions')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch transactions'
    }
  }

  async function fetchSummary() {
    try {
      summary.value = await api.get<PortfolioSummaryStats>('/portfolio/summary')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch portfolio summary'
    }
  }

  async function addTransaction(payload: { ticker: string; transactionType: string; quantity: number; price: number; commission?: number; notes?: string }) {
    try {
      await api.post('/portfolio/transactions', {
        ticker: payload.ticker,
        transactionType: payload.transactionType,
        quantity: payload.quantity,
        price: payload.price,
        commission: payload.commission ?? 0,
        notes: payload.notes,
      })
      await Promise.all([fetchPositions(), fetchSoldPositions(), fetchTransactions(), fetchSummary()])
    } catch (e: any) {
      error.value = e.message || 'Failed to add transaction'
      throw e
    }
  }

  return {
    positions: readonly(positions),
    soldPositions: readonly(soldPositions),
    transactions: readonly(transactions),
    summary: readonly(summary),
    loading: readonly(loading),
    error: readonly(error),
    fetchPositions,
    refreshQuotes,
    refreshTickerQuote,
    fetchSoldPositions,
    fetchTransactions,
    fetchSummary,
    addTransaction,
  }
}
