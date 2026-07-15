import type { FilterCatalog } from '~/types'

/**
 * Loads the Finviz filter catalog (`GET /discovery/filter-options`) once and caches it
 * for the session. Drives every filter dropdown in the discovery UI.
 */
const cache = ref<FilterCatalog | null>(null)

export function useFilterCatalog() {
  const api = useApi()
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function loadCatalog() {
    if (cache.value) return cache.value
    loading.value = true
    error.value = null
    try {
      cache.value = await api.get<FilterCatalog>('/discovery/filter-options')
    } catch (e: any) {
      error.value = e?.message || 'Failed to load filter catalog'
    } finally {
      loading.value = false
    }
    return cache.value
  }

  return { catalog: cache, loading, error, loadCatalog }
}
