<script setup lang="ts">
import type { DiscoveryFilter } from '~/types'
import {
  type Selections,
  defaultSelections, buildActivePills, indexFilters, optionLabel, toPayload,
} from '~/composables/useFinvizFilters'

const {
  stocks, filters, loading, error,
  fetchDiscoveredStocks, fetchFilters, createFilter, toggleFilter, deleteFilter,
  runDiscovery, runAdHoc, promoteStock, dismissStock,
} = useDiscovery()

const { catalog, loadCatalog, error: catalogError } = useFilterCatalog()

// Builder state = one Finviz token per filter key (single-select, faithful mirror).
const builder = reactive<Selections>({})
const rawFinvizFilters = ref('')
const builderOpen = ref(true)
const running = ref(false)
const runMode = ref<'saved' | 'adhoc' | null>(null)
const actionTicker = ref<string | null>(null)
const lastRunLabel = ref<string | null>(null)

const filterIndex = computed(() => indexFilters(catalog.value))
const activeCriteria = computed(() => buildActivePills(builder, catalog.value))

// Save-as-filter inline panel
const showSave = ref(false)
const saveName = ref('')
const saveDesc = ref('')
const saving = ref(false)

onMounted(async () => {
  await Promise.all([loadCatalog(), fetchDiscoveredStocks(), fetchFilters()])
})

function setSelection(key: string, value: string) {
  if (!value) delete builder[key]
  else builder[key] = value
}

function clearBuilder() {
  for (const k of Object.keys(builder)) delete builder[k]
  rawFinvizFilters.value = ''
  lastRunLabel.value = null
}

function loadDefaults() {
  clearBuilder()
  Object.assign(builder, defaultSelections())
}

// ── Actions ─────────────────────────────────────────────────────────────────
async function handleRunNow() {
  running.value = true
  runMode.value = 'adhoc'
  const n = await runAdHoc(toPayload(builder, rawFinvizFilters.value))
  lastRunLabel.value = `Anlık tarama — ${n} sonuç`
  running.value = false
  runMode.value = null
}

async function handleRunSaved() {
  running.value = true
  runMode.value = 'saved'
  const n = await runDiscovery()
  lastRunLabel.value = `Kayıtlı filtreler — ${n} öneri`
  running.value = false
  runMode.value = null
}

async function handleSaveFilter() {
  if (!saveName.value.trim()) return
  saving.value = true
  try {
    await createFilter({
      name: saveName.value.trim(),
      description: saveDesc.value.trim() || undefined,
      ...toPayload(builder, rawFinvizFilters.value),
    })
    showSave.value = false
    saveName.value = ''
    saveDesc.value = ''
  } finally {
    saving.value = false
  }
}

function loadFilterIntoBuilder(f: DiscoveryFilter) {
  clearBuilder()
  Object.assign(builder, f.selections || {})
  rawFinvizFilters.value = f.rawFinvizFilters || ''
  builderOpen.value = true
  if (import.meta.client) window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function runSavedFilterAdHoc(f: DiscoveryFilter) {
  loadFilterIntoBuilder(f)
  await nextTick()
  await handleRunNow()
}

async function handlePromote(ticker: string) {
  actionTicker.value = ticker
  await promoteStock(ticker)
  actionTicker.value = null
}
async function handleDismiss(ticker: string) {
  actionTicker.value = ticker
  await dismissStock(ticker)
  actionTicker.value = null
}

// Summary of a saved filter's selections (labels).
function filterSummary(f: DiscoveryFilter): string[] {
  const idx = filterIndex.value
  return Object.entries(f.selections || {}).map(([k, tok]) => {
    const def = idx.get(k)
    return `${def?.label ?? k}: ${optionLabel(def, tok)}`
  })
}

// ── Formatting ──────────────────────────────────────────────────────────────
const formatCurrency = (value: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', notation: 'compact' }).format(value)

const scoreColor = (score: number) => {
  if (score >= 0.8) return 'text-green-400'
  if (score >= 0.6) return 'text-yellow-400'
  return 'text-gray-400'
}
const trendColor = (t?: string) => {
  if (!t) return 'text-gray-400'
  if (t.includes('UP')) return 'text-green-400'
  if (t.includes('DOWN')) return 'text-red-400'
  return 'text-yellow-400'
}

const groupIcon: Record<string, string> = {
  'Universe': 'heroicons:globe-alt',
  'Price & Size': 'heroicons:banknotes',
  'Valuation': 'heroicons:chart-bar',
  'Momentum & Technical': 'heroicons:bolt',
  'Profitability': 'heroicons:shield-check',
  'Balance Sheet': 'heroicons:building-library',
  'Ownership': 'heroicons:command-line',
  'Analyst': 'heroicons:sparkles',
}

const activeSavedCount = computed(() => filters.value.filter(f => f.active).length)
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-white">Discovery</h1>
        <p class="text-sm text-gray-400">
          Finviz free screener'ı birebir yansıtan filtreler — anlık tara ya da günlük job için kaydet.
        </p>
      </div>
      <div class="flex items-center gap-2">
        <span class="badge-blue">{{ activeSavedCount }} aktif kayıtlı filtre</span>
      </div>
    </div>

    <!-- Error -->
    <div v-if="error || catalogError" class="card border-red-800">
      <p class="text-red-400">{{ error || catalogError }}</p>
    </div>

    <!-- ═══════════════ FILTER BUILDER (ad-hoc) ═══════════════ -->
    <section class="card !p-0 overflow-hidden">
      <button
        class="flex w-full items-center justify-between px-6 py-4 text-left transition-colors hover:bg-gray-800/40"
        @click="builderOpen = !builderOpen"
      >
        <div class="flex items-center gap-2">
          <Icon name="heroicons:adjustments-horizontal" class="h-5 w-5 text-blue-400" />
          <span class="text-base font-semibold text-white">Filtre Oluşturucu</span>
          <span class="text-xs text-gray-500">— her filtre Finviz'in kendi seçenekleriyle, tek seçim</span>
        </div>
        <Icon :name="builderOpen ? 'heroicons:chevron-up' : 'heroicons:chevron-down'" class="h-5 w-5 text-gray-400" />
      </button>

      <div v-show="builderOpen" class="space-y-4 border-t border-gray-800 px-6 py-5">
        <!-- Quick presets -->
        <div class="flex flex-wrap items-center gap-2">
          <span class="text-xs text-gray-500">Hızlı başlangıç:</span>
          <button class="chip" @click="loadDefaults">Growth / Healthy (varsayılan)</button>
          <button class="chip" @click="clearBuilder">Temizle</button>
        </div>

        <!-- Catalog-driven groups -->
        <div v-if="catalog" class="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div
            v-for="g in catalog.groups"
            :key="g.group"
            class="rounded-lg border border-gray-800 bg-gray-900/40 p-4"
          >
            <div class="mb-3 flex items-center gap-1.5 text-sm font-semibold text-gray-300">
              <Icon :name="groupIcon[g.group] || 'heroicons:funnel'" class="h-4 w-4 text-blue-400" />
              {{ g.group }}
            </div>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div v-for="f in g.filters" :key="f.key">
                <label class="mb-1 block text-xs font-medium text-gray-400">{{ f.label }}</label>
                <select
                  :value="builder[f.key] || ''"
                  class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white focus:border-blue-500 focus:outline-none"
                  @change="setSelection(f.key, ($event.target as HTMLSelectElement).value)"
                >
                  <option value="">Any</option>
                  <option v-for="o in f.options" :key="o.token" :value="o.token">{{ o.label }}</option>
                </select>
              </div>
            </div>
          </div>

          <!-- Advanced passthrough -->
          <div class="rounded-lg border border-gray-800 bg-gray-900/40 p-4 lg:col-span-2">
            <div class="mb-2 flex items-center gap-1.5 text-sm font-semibold text-gray-300">
              <Icon name="heroicons:command-line" class="h-4 w-4 text-blue-400" /> Gelişmiş (opsiyonel)
            </div>
            <label class="mb-1 block text-xs font-medium text-gray-400">Ham Finviz filtre token'ları</label>
            <input
              v-model="rawFinvizFilters"
              class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 font-mono text-sm text-white focus:border-blue-500 focus:outline-none"
              placeholder="ör. sh_avgvol_o500,ta_sma50_pa"
            />
            <span class="mt-1 block text-xs text-gray-500">Virgülle ayrılmış Finviz token'ları olduğu gibi eklenir.</span>
          </div>
        </div>
        <div v-else class="py-6 text-center text-sm text-gray-500">Filtre kataloğu yükleniyor…</div>

        <!-- Active criteria summary -->
        <div v-if="activeCriteria.length" class="rounded-lg bg-gray-800/40 px-4 py-3">
          <p class="mb-1.5 text-[11px] font-semibold uppercase tracking-wide text-gray-500">Aktif kriterler</p>
          <div class="flex flex-wrap gap-1.5">
            <span v-for="c in activeCriteria" :key="c.key" class="badge bg-gray-800 text-gray-300 ring-1 ring-gray-700">{{ c.label }}</span>
          </div>
        </div>

        <!-- Builder actions -->
        <div class="flex flex-wrap items-center gap-2 border-t border-gray-800 pt-4">
          <button class="btn-primary" :disabled="running" @click="handleRunNow">
            <Icon v-if="running && runMode === 'adhoc'" name="heroicons:arrow-path" class="mr-1.5 h-4 w-4 animate-spin" />
            <Icon v-else name="heroicons:bolt" class="mr-1.5 h-4 w-4" />
            {{ running && runMode === 'adhoc' ? 'Taranıyor...' : 'Şimdi Tara' }}
          </button>
          <button class="btn-secondary" :disabled="running" @click="showSave = !showSave">
            <Icon name="heroicons:bookmark" class="mr-1.5 h-4 w-4" /> Filtre Olarak Kaydet
          </button>
          <button class="btn-secondary" :disabled="running" @click="clearBuilder">Temizle</button>
          <span v-if="lastRunLabel" class="ml-auto text-xs text-gray-500">{{ lastRunLabel }}</span>
        </div>

        <!-- Save inline panel -->
        <div v-if="showSave" class="rounded-lg border border-blue-800 bg-blue-950/20 p-4">
          <p class="mb-3 text-sm font-medium text-white">Kayıtlı filtre olarak sakla</p>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-xs font-medium text-gray-400">Filtre adı *</label>
              <input v-model="saveName" class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white" placeholder="ör. Growth Mid-Cap Tech" />
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-gray-400">Açıklama</label>
              <input v-model="saveDesc" class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white" placeholder="Opsiyonel" />
            </div>
          </div>
          <div class="mt-3 flex gap-2">
            <button class="btn-primary" :disabled="!saveName.trim() || saving" @click="handleSaveFilter">
              <Icon name="heroicons:check" class="mr-1.5 h-4 w-4" /> Kaydet
            </button>
            <button class="btn-secondary" @click="showSave = false">Vazgeç</button>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════ SAVED FILTERS (scheduled job) ═══════════════ -->
    <section class="space-y-3">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-lg font-semibold text-white">Kayıtlı Filtreler</h2>
          <p class="text-xs text-gray-500">Aktif olanlar günlük discovery job'unda otomatik çalışır.</p>
        </div>
        <button class="btn-secondary" :disabled="running" @click="handleRunSaved">
          <Icon v-if="running && runMode === 'saved'" name="heroicons:arrow-path" class="mr-1.5 h-4 w-4 animate-spin" />
          <Icon v-else name="heroicons:play" class="mr-1.5 h-4 w-4" />
          Aktif Filtreleri Çalıştır
        </button>
      </div>

      <div v-if="filters.length" class="space-y-2">
        <div v-for="f in filters" :key="f.id" class="card flex flex-wrap items-center justify-between gap-3 !py-4">
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <h4 class="truncate font-medium text-white">{{ f.name }}</h4>
              <span :class="f.active ? 'badge-green' : 'badge bg-gray-800 text-gray-500'">
                {{ f.active ? 'AKTİF' : 'PASİF' }}
              </span>
            </div>
            <p v-if="f.description" class="mt-0.5 truncate text-xs text-gray-500">{{ f.description }}</p>
            <div class="mt-1 flex flex-wrap gap-1">
              <span v-for="s in filterSummary(f)" :key="s" class="rounded bg-gray-800 px-1.5 py-0.5 text-[10px] text-gray-400">{{ s }}</span>
            </div>
          </div>
          <div class="flex items-center gap-1.5">
            <button class="btn-secondary !px-2.5 !py-1 text-xs" :disabled="running" title="Bu kriterlerle hemen tara" @click="runSavedFilterAdHoc(f)">
              <Icon name="heroicons:bolt" class="mr-1 h-3.5 w-3.5" /> Tara
            </button>
            <button class="btn-secondary !px-2.5 !py-1 text-xs" title="Oluşturucuya yükle" @click="loadFilterIntoBuilder(f)">
              <Icon name="heroicons:pencil-square" class="mr-1 h-3.5 w-3.5" /> Yükle
            </button>
            <button class="btn-secondary !px-2.5 !py-1 text-xs" @click="toggleFilter(f.id, !f.active)">
              {{ f.active ? 'Pasifleştir' : 'Aktifleştir' }}
            </button>
            <button class="rounded p-1.5 text-gray-500 hover:bg-red-900/30 hover:text-red-400" title="Sil" @click="deleteFilter(f.id)">
              <Icon name="heroicons:trash" class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
      <p v-else class="rounded-lg border border-dashed border-gray-800 py-6 text-center text-sm text-gray-500">
        Henüz kayıtlı filtre yok. Yukarıdan bir filtre oluşturup "Filtre Olarak Kaydet" ile saklayabilirsin.
      </p>
    </section>

    <!-- ═══════════════ RESULTS ═══════════════ -->
    <section class="space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-semibold text-white">Sonuçlar ({{ stocks.length }})</h2>
        <button class="btn-secondary !py-1.5 text-xs" :disabled="loading" @click="fetchDiscoveredStocks">
          <Icon name="heroicons:arrow-path" class="mr-1 h-3.5 w-3.5" :class="loading && 'animate-spin'" /> Yenile
        </button>
      </div>

      <div v-if="loading" class="flex items-center justify-center py-10">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
      </div>

      <div v-else-if="stocks.length" class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        <div v-for="stock in stocks" :key="stock.ticker" class="card group transition-colors hover:border-gray-700">
          <div class="mb-3 flex items-start justify-between">
            <div>
              <h3 class="text-lg font-bold text-white">{{ stock.ticker }}</h3>
              <p class="text-sm text-gray-400">{{ stock.companyName }}</p>
            </div>
            <div class="text-right">
              <p class="text-sm font-medium" :class="scoreColor(stock.confidence ?? stock.score)">
                {{ ((stock.confidence ?? stock.score) * 100).toFixed(0) }}%
              </p>
              <p class="text-[10px]" :class="stock.recommended ? 'text-green-400' : 'text-gray-500'">
                {{ stock.recommended ? 'recommended' : stock.status?.toLowerCase() || 'confidence' }}
              </p>
            </div>
          </div>

          <div v-if="stock.reasoning" class="mb-3 rounded bg-gray-800/50 px-2 py-1.5">
            <p class="text-xs text-blue-300">{{ stock.reasoning }}</p>
          </div>

          <div class="mb-3 grid grid-cols-2 gap-2 text-sm">
            <div>
              <span class="text-gray-500">Sektör</span>
              <p class="text-gray-300">{{ stock.sector || '—' }}</p>
            </div>
            <div>
              <span class="text-gray-500">Piyasa Değeri</span>
              <p class="text-gray-300">{{ stock.marketCap ? formatCurrency(stock.marketCap) : '—' }}</p>
            </div>
            <div v-if="stock.peRatio">
              <span class="text-gray-500">P/E</span>
              <p class="text-gray-300">{{ Number(stock.peRatio).toFixed(1) }}</p>
            </div>
            <div v-if="stock.trendDirection">
              <span class="text-gray-500">Trend</span>
              <p :class="trendColor(stock.trendDirection)">{{ stock.trendDirection }}</p>
            </div>
          </div>

          <div v-if="stock.signals?.length" class="mb-4 flex flex-wrap gap-1">
            <span v-for="signal in stock.signals" :key="signal" class="rounded bg-gray-800 px-2 py-0.5 text-[10px] text-gray-400">{{ signal }}</span>
          </div>

          <div class="flex gap-2">
            <button class="btn-primary flex-1 !py-1.5 text-sm" :disabled="actionTicker === stock.ticker" @click="handlePromote(stock.ticker)">
              <Icon name="heroicons:arrow-up-circle" class="mr-1 h-4 w-4" /> Watchlist
            </button>
            <button class="btn-secondary flex-1 !py-1.5 text-sm" :disabled="actionTicker === stock.ticker" @click="handleDismiss(stock.ticker)">
              <Icon name="heroicons:x-circle" class="mr-1 h-4 w-4" /> Dismiss
            </button>
          </div>
        </div>
      </div>

      <p v-else class="rounded-lg border border-dashed border-gray-800 py-10 text-center text-sm text-gray-500">
        Henüz sonuç yok. Yukarıdan kriterleri seçip "Şimdi Tara" ile anlık bir tarama başlat.
      </p>
    </section>
  </div>
</template>
