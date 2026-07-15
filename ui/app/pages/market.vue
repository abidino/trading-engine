<script setup lang="ts">
const {
  ticker, indicators, trend, trendHistory, supportResistance, quote,
  loading, analyzing, error, loadAll, analyzeTrend, refreshQuote,
} = useMarketData()

const tickerInput = ref('')

async function handleLoad() {
  if (!tickerInput.value.trim()) return
  await Promise.all([
    loadAll(tickerInput.value.trim()),
    refreshQuote(tickerInput.value.trim()),
  ])
}

async function handleAnalyze() {
  if (!tickerInput.value.trim()) return
  await analyzeTrend(tickerInput.value.trim())
  await refreshQuote(tickerInput.value.trim())
}

const trendColor = (t?: string) => {
  switch (t) {
    case 'STRONG_UPTREND':
    case 'UPTREND': return 'badge-green'
    case 'STRONG_DOWNTREND':
    case 'DOWNTREND': return 'badge-red'
    default: return 'badge-yellow'
  }
}

const fmt = (v: number | null | undefined) =>
  v === null || v === undefined ? '—' : Number(v).toFixed(2)

const formatTime = (iso?: string) => (iso ? new Date(iso).toLocaleString() : '—')
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-white">Market Data</h1>
      <p class="text-sm text-gray-400">Technical indicators, daily trend &amp; support/resistance</p>
    </div>

    <!-- Search + actions -->
    <div class="card">
      <form class="flex flex-wrap gap-3" @submit.prevent="handleLoad">
        <input
          v-model="tickerInput"
          type="text"
          placeholder="Enter ticker (e.g. AAPL, NVDA)"
          class="flex-1 min-w-[12rem] rounded-lg border border-gray-700 bg-gray-800 px-4 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none"
          required
        >
        <button type="submit" class="btn-primary" :disabled="loading || analyzing">
          <Icon name="heroicons:chart-bar" class="mr-1.5 h-4 w-4" />
          Load
        </button>
        <button type="button" class="btn-primary" :disabled="analyzing || !tickerInput.trim()" @click="handleAnalyze">
          <Icon name="heroicons:cpu-chip" class="mr-1.5 h-4 w-4" :class="analyzing ? 'animate-spin' : ''" />
          Analyze Trend
        </button>
      </form>
    </div>

    <!-- Error -->
    <div v-if="error" class="rounded-lg bg-red-900/20 p-4">
      <p class="text-sm text-red-400">{{ error }}</p>
    </div>

    <!-- Loading -->
    <div v-if="loading || analyzing" class="flex items-center justify-center py-12">
      <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
    </div>

    <template v-else-if="ticker && indicators">
      <!-- Live quote + trend verdict -->
      <div class="grid gap-4 md:grid-cols-2">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">{{ ticker }} — Quote</h3>
            <span v-if="quote" class="badge badge-blue text-xs">{{ quote.session }}</span>
          </div>
          <div v-if="quote" class="space-y-1">
            <div class="flex items-baseline gap-3">
              <span class="text-3xl font-bold text-white">${{ fmt(quote.price) }}</span>
              <span
                class="text-sm font-medium"
                :class="(quote.change ?? 0) >= 0 ? 'text-green-400' : 'text-red-400'"
              >
                {{ (quote.change ?? 0) >= 0 ? '+' : '' }}{{ fmt(quote.change) }}
                ({{ fmt(quote.changePercent) }}%)
              </span>
            </div>
            <p class="text-xs text-gray-500">Prev close ${{ fmt(quote.previousClose) }} · vol {{ quote.volume?.toLocaleString() }}</p>
            <p class="text-xs text-gray-500">Captured {{ formatTime(quote.capturedAt) }}</p>
          </div>
          <p v-else class="text-sm text-gray-500">No quote — press Load.</p>
        </div>

        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Daily Trend</h3>
            <span v-if="trend" :class="trendColor(trend.trend)" class="badge">{{ trend.trend }}</span>
          </div>
          <div v-if="trend" class="space-y-1">
            <p class="text-sm text-gray-400">
              Confidence {{ (trend.confidence * 100).toFixed(0) }}% · {{ trend.analysisDate }}
            </p>
            <p class="text-sm text-gray-300">{{ trend.reasoning }}</p>
            <p class="text-xs text-gray-600">model: {{ trend.llmModel }}</p>
          </div>
          <p v-else class="text-sm text-gray-500">No trend yet — press <span class="text-blue-400">Analyze Trend</span>.</p>
        </div>
      </div>

      <!-- Indicators -->
      <div class="card">
        <h3 class="card-title mb-4">Technical Indicators <span class="text-xs text-gray-500">({{ indicators.dataPoints }} candles)</span></h3>
        <div class="grid grid-cols-2 gap-x-6 gap-y-2 sm:grid-cols-3 lg:grid-cols-4">
          <div class="flex justify-between border-b border-gray-800 py-1"><span class="text-gray-500">Close</span><span class="text-white">{{ fmt(indicators.close) }}</span></div>
          <div class="flex justify-between border-b border-gray-800 py-1"><span class="text-gray-500">RSI(14)</span><span class="text-white">{{ fmt(indicators.rsi14) }}</span></div>
          <div class="flex justify-between border-b border-gray-800 py-1"><span class="text-gray-500">MACD</span><span class="text-white">{{ fmt(indicators.macd) }}</span></div>
          <div class="flex justify-between border-b border-gray-800 py-1"><span class="text-gray-500">MACD sig</span><span class="text-white">{{ fmt(indicators.macdSignal) }}</span></div>
          <div class="flex justify-between border-b border-gray-800 py-1"><span class="text-gray-500">EMA 9</span><span class="text-white">{{ fmt(indicators.ema9) }}</span></div>
          <div class="flex justify-between border-b border-gray-800 py-1"><span class="text-gray-500">EMA 20</span><span class="text-white">{{ fmt(indicators.ema20) }}</span></div>
          <div class="flex justify-between border-b border-gray-800 py-1"><span class="text-gray-500">EMA 50</span><span class="text-white">{{ fmt(indicators.ema50) }}</span></div>
          <div class="flex justify-between border-b border-gray-800 py-1"><span class="text-gray-500">EMA 100</span><span class="text-white">{{ fmt(indicators.ema100) }}</span></div>
          <div class="flex justify-between border-b border-gray-800 py-1"><span class="text-gray-500">EMA 200</span><span class="text-white">{{ fmt(indicators.ema200) }}</span></div>
        </div>
      </div>

      <!-- Support / Resistance -->
      <div v-if="supportResistance" class="card">
        <h3 class="card-title mb-4">Support &amp; Resistance</h3>
        <div class="grid gap-6 md:grid-cols-2">
          <!-- nearest + swing zones -->
          <div class="space-y-3">
            <div class="flex gap-3">
              <div class="flex-1 rounded-lg bg-red-900/20 p-3 text-center">
                <p class="text-xs text-gray-400">Nearest Resistance</p>
                <p class="text-lg font-bold text-red-400">{{ fmt(supportResistance.nearestResistance) }}</p>
              </div>
              <div class="flex-1 rounded-lg bg-gray-800 p-3 text-center">
                <p class="text-xs text-gray-400">Close</p>
                <p class="text-lg font-bold text-white">{{ fmt(supportResistance.close) }}</p>
              </div>
              <div class="flex-1 rounded-lg bg-green-900/20 p-3 text-center">
                <p class="text-xs text-gray-400">Nearest Support</p>
                <p class="text-lg font-bold text-green-400">{{ fmt(supportResistance.nearestSupport) }}</p>
              </div>
            </div>
            <div>
              <p class="mb-1 text-xs font-medium text-red-400">Resistance zones</p>
              <div class="flex flex-wrap gap-1.5">
                <span v-for="r in supportResistance.resistances" :key="'r' + r" class="badge badge-red text-xs">{{ fmt(r) }}</span>
                <span v-if="!supportResistance.resistances.length" class="text-xs text-gray-500">none above current price</span>
              </div>
            </div>
            <div>
              <p class="mb-1 text-xs font-medium text-green-400">Support zones</p>
              <div class="flex flex-wrap gap-1.5">
                <span v-for="s in supportResistance.supports" :key="'s' + s" class="badge badge-green text-xs">{{ fmt(s) }}</span>
                <span v-if="!supportResistance.supports.length" class="text-xs text-gray-500">none below current price</span>
              </div>
            </div>
          </div>
          <!-- classic pivots -->
          <div>
            <p class="mb-2 text-xs font-medium text-gray-400">Floor-trader pivots</p>
            <div class="grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
              <div class="flex justify-between"><span class="text-red-400">R3</span><span class="text-white">{{ fmt(supportResistance.r3) }}</span></div>
              <div class="flex justify-between"><span class="text-green-400">S3</span><span class="text-white">{{ fmt(supportResistance.s3) }}</span></div>
              <div class="flex justify-between"><span class="text-red-400">R2</span><span class="text-white">{{ fmt(supportResistance.r2) }}</span></div>
              <div class="flex justify-between"><span class="text-green-400">S2</span><span class="text-white">{{ fmt(supportResistance.s2) }}</span></div>
              <div class="flex justify-between"><span class="text-red-400">R1</span><span class="text-white">{{ fmt(supportResistance.r1) }}</span></div>
              <div class="flex justify-between"><span class="text-green-400">S1</span><span class="text-white">{{ fmt(supportResistance.s1) }}</span></div>
              <div class="col-span-2 mt-1 flex justify-between border-t border-gray-800 pt-1"><span class="text-gray-400">Pivot</span><span class="text-white">{{ fmt(supportResistance.pivot) }}</span></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Trend history -->
      <div v-if="trendHistory.length" class="card">
        <h3 class="card-title mb-4">Trend History</h3>
        <div class="space-y-2">
          <div
            v-for="h in trendHistory"
            :key="h.analysisDate"
            class="flex items-center justify-between rounded-lg bg-gray-800/50 px-4 py-2"
          >
            <div class="flex items-center gap-3">
              <span class="text-sm text-gray-400">{{ h.analysisDate }}</span>
              <span :class="trendColor(h.trend)" class="badge text-xs">{{ h.trend }}</span>
            </div>
            <span class="text-xs text-gray-500">{{ (h.confidence * 100).toFixed(0) }}%</span>
          </div>
        </div>
      </div>
    </template>

    <!-- Empty prompt -->
    <div v-else-if="!loading && !analyzing" class="card flex flex-col items-center py-16 text-center">
      <Icon name="heroicons:chart-bar-square" class="h-12 w-12 text-gray-600" />
      <p class="mt-3 text-sm text-gray-500">Enter a ticker and press Load to see technicals</p>
    </div>
  </div>
</template>
