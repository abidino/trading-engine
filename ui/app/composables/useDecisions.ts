import type { DecisionResponse, AccuracyReport } from '~/types/decisions'

export function useDecisions() {
  const { get } = useApi()

  const all = ref<DecisionResponse[]>([])
  const accuracy = ref<AccuracyReport | null>(null)
  const page = ref(1)
  const pageSize = ref(20)
  const tickerFilter = ref('')
  const actionFilter = ref('')
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchDecisions() {
    loading.value = true
    error.value = null
    try {
      all.value = await get<DecisionResponse[]>('/decisions')
    } catch (e: any) {
      error.value = e?.message || 'Failed to fetch decisions'
    } finally {
      loading.value = false
    }
  }

  async function fetchAccuracy() {
    try {
      accuracy.value = await get<AccuracyReport>('/decisions/accuracy')
    } catch {
      accuracy.value = null
    }
  }

  const filtered = computed(() => {
    const ticker = tickerFilter.value.trim().toUpperCase()
    const action = actionFilter.value.trim().toUpperCase()
    return all.value.filter(d =>
      (!ticker || d.ticker.toUpperCase().includes(ticker))
      && (!action || d.action.toUpperCase() === action),
    )
  })

  const total = computed(() => filtered.value.length)
  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

  const decisions = computed(() => {
    const start = (page.value - 1) * pageSize.value
    return filtered.value.slice(start, start + pageSize.value)
  })

  function nextPage() {
    if (page.value < totalPages.value) page.value++
  }

  function prevPage() {
    if (page.value > 1) page.value--
  }

  function applyFilters() {
    page.value = 1
  }

  return {
    decisions,
    accuracy,
    total,
    page,
    pageSize,
    tickerFilter,
    actionFilter,
    loading,
    error,
    totalPages,
    fetchDecisions,
    fetchAccuracy,
    nextPage,
    prevPage,
    applyFilters,
  }
}
