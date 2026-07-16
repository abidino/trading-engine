<script setup lang="ts">
const {
  positions,
  soldPositions,
  transactions,
  summary,
  loading,
  fetchPositions,
  refreshQuotes,
  refreshTickerQuote,
  fetchSoldPositions,
  fetchTransactions,
  fetchSummary,
  addTransaction,
} = usePortfolio()

onMounted(async () => {
  // Pull the latest live quotes for held tickers before showing valuations.
  await refreshQuotes()
  await Promise.all([fetchPositions(), fetchSoldPositions(), fetchTransactions(), fetchSummary()])
})

const activeTab = ref<'open' | 'sold'>('open')
const showAddForm = ref(false)
const refreshingTicker = ref<string | null>(null)
const form = reactive({
  ticker: '',
  transactionType: 'BUY' as 'BUY' | 'SELL',
  quantity: 0,
  price: 0,
  commission: 1.5,
})

const formatCurrency = (value: number | string) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value))

const formatPercent = (value: number | string) => {
  const n = Number(value)
  const sign = n >= 0 ? '+' : ''
  return sign + n.toFixed(2) + '%'
}

async function handleSubmit() {
  await addTransaction({
    ticker: form.ticker.toUpperCase(),
    transactionType: form.transactionType,
    quantity: form.quantity,
    price: form.price,
    commission: form.commission,
  })
  showAddForm.value = false
  form.ticker = ''
  form.quantity = 0
  form.price = 0
  form.commission = 1.5
}

async function handleRefreshTicker(ticker: string) {
  refreshingTicker.value = ticker
  try {
    await refreshTickerQuote(ticker)
  } finally {
    refreshingTicker.value = null
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Portfolio</h1>
        <p class="text-sm text-gray-400">Manage positions and transactions</p>
      </div>
      <button class="btn-primary" @click="showAddForm = !showAddForm">
        <Icon name="heroicons:plus" class="mr-1.5 h-4 w-4" />
        Add Transaction
      </button>
    </div>

    <!-- Add Transaction Form -->
    <div v-if="showAddForm" class="card">
      <h3 class="card-title mb-4">New Transaction</h3>
      <p class="mb-4 text-xs text-gray-500">Commission is per transaction (total fee paid for this order, not per share).</p>
      <form class="grid grid-cols-1 gap-4 sm:grid-cols-6" @submit.prevent="handleSubmit">
        <div>
          <label class="mb-1 block text-xs text-gray-400">Ticker</label>
          <input
            v-model="form.ticker"
            type="text"
            placeholder="AAPL"
            class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none"
            required
          />
        </div>
        <div>
          <label class="mb-1 block text-xs text-gray-400">Type</label>
          <select
            v-model="form.transactionType"
            class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white focus:border-blue-500 focus:outline-none"
          >
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-xs text-gray-400">Quantity</label>
          <input
            v-model.number="form.quantity"
            type="number"
            min="0.0001"
            step="0.0001"
            placeholder="0.7"
            class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none"
            required
          />
        </div>
        <div>
          <label class="mb-1 block text-xs text-gray-400">Price per Share</label>
          <input
            v-model.number="form.price"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="150.00"
            class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none"
            required
          />
        </div>
        <div>
          <label class="mb-1 block text-xs text-gray-400">Commission (total)</label>
          <input
            v-model.number="form.commission"
            type="number"
            step="0.01"
            min="0"
            placeholder="1.50"
            class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none"
          />
        </div>
        <div class="flex items-end">
          <button type="submit" class="btn-primary w-full">Submit</button>
        </div>
      </form>
    </div>

    <!-- Summary Dashboard -->
    <div v-if="summary" class="grid grid-cols-2 gap-4 sm:grid-cols-5">
      <div class="card sm:col-span-1">
        <p class="stat-label">Market Value</p>
        <p class="stat-value text-white">{{ formatCurrency(summary.totalMarketValue) }}</p>
        <p class="mt-1 text-xs text-gray-500">{{ summary.positionCount }} positions</p>
      </div>
      <div class="card sm:col-span-1">
        <p class="stat-label">Portfolio Gains</p>
        <p class="stat-value text-green-400">{{ formatCurrency(summary.totalGains) }}</p>
        <p class="mt-1 text-xs text-gray-500">{{ summary.gainPositionCount }} positions up</p>
      </div>
      <div class="card sm:col-span-1">
        <p class="stat-label">Portfolio Losses</p>
        <p class="stat-value text-red-400">{{ formatCurrency(summary.totalLosses) }}</p>
        <p class="mt-1 text-xs text-gray-500">{{ summary.lossPositionCount }} positions down</p>
      </div>
      <div class="card sm:col-span-1">
        <p class="stat-label">Commissions Paid</p>
        <p class="stat-value text-yellow-400">{{ formatCurrency(summary.totalCommissions) }}</p>
        <p class="mt-1 text-xs text-gray-500">All transactions</p>
      </div>
      <div class="card sm:col-span-1">
        <p class="stat-label">Net P&amp;L</p>
        <p class="stat-value" :class="summary.netPnl >= 0 ? 'text-green-400' : 'text-red-400'">
          {{ formatCurrency(summary.netPnl) }}
        </p>
        <p class="mt-1 text-xs text-gray-500">Unrealized P&amp;L − alım komisyonları</p>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-10">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
    </div>

    <!-- Tabs -->
    <div v-else class="card">
      <div class="mb-4 flex gap-1 border-b border-gray-800">
        <button
          class="px-4 py-2 text-sm font-medium transition-colors"
          :class="activeTab === 'open' ? 'border-b-2 border-blue-500 text-blue-400' : 'text-gray-400 hover:text-gray-200'"
          @click="activeTab = 'open'"
        >
          Open Positions
          <span class="ml-1.5 rounded-full bg-gray-700 px-1.5 py-0.5 text-xs text-gray-300">
            {{ positions.length }}
          </span>
        </button>
        <button
          class="px-4 py-2 text-sm font-medium transition-colors"
          :class="activeTab === 'sold' ? 'border-b-2 border-blue-500 text-blue-400' : 'text-gray-400 hover:text-gray-200'"
          @click="activeTab = 'sold'"
        >
          Sold Positions
          <span class="ml-1.5 rounded-full bg-gray-700 px-1.5 py-0.5 text-xs text-gray-300">
            {{ soldPositions.length }}
          </span>
        </button>
      </div>

      <!-- Open Positions Table -->
      <div v-if="activeTab === 'open'" class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-800 text-left text-gray-400">
              <th class="pb-3 font-medium">Ticker</th>
              <th class="pb-3 font-medium">Qty</th>
              <th class="pb-3 font-medium">Avg Cost</th>
              <th class="pb-3 font-medium">Current</th>
              <th class="pb-3 font-medium">Market Value</th>
              <th class="pb-3 font-medium">P&amp;L</th>
              <th class="pb-3 font-medium">%</th>
              <th class="pb-3 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-800">
            <tr v-for="pos in positions" :key="pos.id">
              <td class="py-3 font-medium text-white">{{ pos.ticker }}</td>
              <td class="py-3 text-gray-300">{{ pos.quantity }}</td>
              <td class="py-3 text-gray-300">{{ formatCurrency(pos.averageCost) }}</td>
              <td class="py-3 text-gray-300">{{ formatCurrency(pos.currentPrice) }}</td>
              <td class="py-3 text-gray-300">{{ formatCurrency(pos.marketValue) }}</td>
              <td class="py-3" :class="Number(pos.unrealizedPnl) >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ formatCurrency(pos.unrealizedPnl) }}
              </td>
              <td class="py-3" :class="Number(pos.unrealizedPnlPercent) >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ formatPercent(pos.unrealizedPnlPercent) }}
              </td>
              <td class="py-3">
                <button
                  class="rounded bg-blue-600 px-2 py-1 text-xs text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="refreshingTicker === pos.ticker"
                  @click="handleRefreshTicker(pos.ticker)"
                >
                  <span v-if="refreshingTicker === pos.ticker" class="flex items-center gap-1">
                    <Icon name="svg-spinners:ring-resize" class="h-3 w-3" />
                    <span>Refreshing...</span>
                  </span>
                  <span v-else class="flex items-center gap-1">
                    <Icon name="heroicons:arrow-path" class="h-3 w-3" />
                    <span>Refresh Price</span>
                  </span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!positions.length" class="py-6 text-center text-sm text-gray-500">
          No open positions. Add a BUY transaction to get started.
        </p>
      </div>

      <!-- Sold Positions Table -->
      <div v-if="activeTab === 'sold'" class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-800 text-left text-gray-400">
              <th class="pb-3 font-medium">Ticker</th>
              <th class="pb-3 font-medium">Qty</th>
              <th class="pb-3 font-medium">Avg Buy</th>
              <th class="pb-3 font-medium">Avg Sell</th>
              <th class="pb-3 font-medium">Buy Commission</th>
              <th class="pb-3 font-medium">Sell Commission</th>
              <th class="pb-3 font-medium">Total Commission</th>
              <th class="pb-3 font-medium">Realized P&amp;L</th>
              <th class="pb-3 font-medium">Sold At</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-800">
            <tr v-for="sold in soldPositions" :key="sold.ticker">
              <td class="py-3 font-medium text-white">{{ sold.ticker }}</td>
              <td class="py-3 text-gray-300">{{ Number(sold.totalQuantity).toFixed(4) }}</td>
              <td class="py-3 text-gray-300">{{ formatCurrency(sold.avgBuyPrice) }}</td>
              <td class="py-3 text-gray-300">{{ formatCurrency(sold.avgSellPrice) }}</td>
              <td class="py-3 text-yellow-400">{{ formatCurrency(sold.totalBuyCommission) }}</td>
              <td class="py-3 text-yellow-400">{{ formatCurrency(sold.totalSellCommission) }}</td>
              <td class="py-3 text-yellow-400">{{ formatCurrency(sold.totalCommission) }}</td>
              <td class="py-3 font-semibold" :class="Number(sold.realizedPnl) >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ formatCurrency(sold.realizedPnl) }}
              </td>
              <td class="py-3 text-xs text-gray-500">
                {{ sold.soldAt ? new Date(sold.soldAt).toLocaleDateString() : '-' }}
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!soldPositions.length" class="py-6 text-center text-sm text-gray-500">
          No sold positions yet.
        </p>
      </div>
    </div>

    <!-- Recent Transactions -->
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">Recent Transactions</h3>
      </div>
      <div class="space-y-2">
        <div
          v-for="tx in transactions.slice(0, 10)"
          :key="tx.id"
          class="flex items-center justify-between rounded-lg bg-gray-800/50 px-4 py-3"
        >
          <div class="flex items-center gap-3">
            <span class="badge" :class="tx.transactionType === 'BUY' ? 'badge-green' : 'badge-red'">
              {{ tx.transactionType }}
            </span>
            <span class="font-medium text-white">{{ tx.ticker }}</span>
            <span class="text-gray-400">{{ tx.quantity }} shares @ {{ formatCurrency(tx.price) }}</span>
            <span v-if="Number(tx.commission) > 0" class="text-xs text-yellow-500">
              fee: {{ formatCurrency(tx.commission) }}
            </span>
          </div>
          <span class="text-xs text-gray-500">
            {{ new Date(tx.executedAt).toLocaleDateString() }}
          </span>
        </div>
        <p v-if="!transactions.length" class="py-4 text-center text-sm text-gray-500">
          No transactions yet.
        </p>
      </div>
    </div>
  </div>
</template>
