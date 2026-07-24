<script setup lang="ts">
import type { DecisionResponse, TradingLevels } from '~/types/decisions'

const props = defineProps<{
  decision: DecisionResponse | null
}>()

const emit = defineEmits<{ close: [] }>()

const actionColor = (action?: string) => {
  switch (action) {
    case 'BUY': return 'badge-green'
    case 'SELL': return 'badge-red'
    case 'HOLD': return 'badge-yellow'
    default: return 'badge-gray'
  }
}

const outcomeColor = (outcome?: string) => {
  switch (outcome) {
    case 'CORRECT': return 'text-green-400'
    case 'INCORRECT': return 'text-red-400'
    default: return 'text-gray-500'
  }
}

const formatTime = (iso?: string | null) =>
  iso ? new Date(iso).toLocaleString() : '—'

const formatConfidence = (c?: number) =>
  c == null ? '—' : `${(Number(c) * 100).toFixed(0)}%`

const fmtLevel = (v?: number | null) =>
  v == null ? '—' : `$${Number(v).toFixed(2)}`

const hasLevels = (l?: TradingLevels | null) =>
  !!l && [l.entryLow, l.entryHigh, l.aggressiveEntry, l.idealEntry, l.safeEntry,
          l.stopLoss, l.takeProfit, l.nearestSupport, l.nearestResistance]
    .some(v => v != null)

const summaries = computed(() => {
  const d = props.decision
  if (!d) return []
  return [
    { key: 'news', label: 'News', icon: 'heroicons:newspaper', text: d.newsSummary },
    { key: 'social', label: 'Social', icon: 'heroicons:chat-bubble-left-right', text: d.socialSummary },
    { key: 'technical', label: 'Technical', icon: 'heroicons:chart-bar', text: d.technicalSummary },
    { key: 'fundamental', label: 'Fundamental', icon: 'heroicons:building-library', text: d.fundamentalSummary },
  ].filter(s => s.text && s.text.trim().length > 0)
})

const onKey = (e: KeyboardEvent) => {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div
        v-if="decision"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="emit('close')" />

        <!-- Panel -->
        <div class="relative z-10 flex max-h-[85vh] w-full max-w-2xl flex-col overflow-hidden rounded-xl border border-gray-700 bg-gray-900 shadow-2xl">
          <!-- Header -->
          <div class="flex items-start justify-between gap-4 border-b border-gray-800 px-6 py-4">
            <div class="flex flex-wrap items-center gap-3">
              <span class="text-xl font-bold text-white">{{ decision.ticker }}</span>
              <span :class="actionColor(decision.action)" class="badge">{{ decision.action }}</span>
              <span class="text-xs text-gray-500">{{ decision.decisionType }}</span>
            </div>
            <button
              class="rounded-lg p-1 text-gray-400 transition hover:bg-gray-800 hover:text-white"
              aria-label="Close"
              @click="emit('close')"
            >
              <Icon name="heroicons:x-mark" class="h-5 w-5" />
            </button>
          </div>

          <!-- Body (scrollable) -->
          <div class="space-y-5 overflow-y-auto px-6 py-5">
            <!-- Meta -->
            <div class="grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
              <div>
                <p class="text-xs text-gray-500">Confidence</p>
                <p class="font-semibold text-white">{{ formatConfidence(decision.confidence) }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500">Outcome</p>
                <p class="font-semibold" :class="outcomeColor(decision.outcome)">{{ decision.outcome || '—' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500">Decided</p>
                <p class="font-medium text-gray-300">{{ formatTime(decision.decidedAt) }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500">Evaluated</p>
                <p class="font-medium text-gray-300">{{ formatTime(decision.evaluatedAt) }}</p>
              </div>
            </div>

            <!-- Trading levels -->
            <div v-if="hasLevels(decision.levels)">
              <h3 class="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Trading Levels</h3>
              <div class="grid grid-cols-2 gap-3 rounded-lg bg-gray-800/50 p-3 text-sm sm:grid-cols-3">
                <div v-if="decision.levels?.entryLow != null || decision.levels?.entryHigh != null">
                  <p class="text-xs text-gray-500">Buy Zone</p>
                  <p class="font-semibold text-green-400">
                    {{ fmtLevel(decision.levels?.entryLow) }} – {{ fmtLevel(decision.levels?.entryHigh) }}
                  </p>
                </div>
                <div v-if="decision.levels?.aggressiveEntry != null">
                  <p class="text-xs text-gray-500">Aggressive Entry</p>
                  <p class="font-semibold text-amber-400">{{ fmtLevel(decision.levels?.aggressiveEntry) }}</p>
                </div>
                <div v-if="decision.levels?.idealEntry != null">
                  <p class="text-xs text-gray-500">Ideal Entry</p>
                  <p class="font-semibold text-emerald-400">{{ fmtLevel(decision.levels?.idealEntry) }}</p>
                </div>
                <div v-if="decision.levels?.safeEntry != null">
                  <p class="text-xs text-gray-500">Safe Entry</p>
                  <p class="font-semibold text-sky-400">{{ fmtLevel(decision.levels?.safeEntry) }}</p>
                </div>
                <div v-if="decision.levels?.stopLoss != null">
                  <p class="text-xs text-gray-500">Stop Loss</p>
                  <p class="font-semibold text-red-400">{{ fmtLevel(decision.levels?.stopLoss) }}</p>
                </div>
                <div v-if="decision.levels?.takeProfit != null">
                  <p class="text-xs text-gray-500">Take Profit</p>
                  <p class="font-semibold text-blue-400">{{ fmtLevel(decision.levels?.takeProfit) }}</p>
                </div>
                <div v-if="decision.levels?.nearestSupport != null">
                  <p class="text-xs text-gray-500">Support</p>
                  <p class="font-medium text-gray-300">{{ fmtLevel(decision.levels?.nearestSupport) }}</p>
                </div>
                <div v-if="decision.levels?.nearestResistance != null">
                  <p class="text-xs text-gray-500">Resistance</p>
                  <p class="font-medium text-gray-300">{{ fmtLevel(decision.levels?.nearestResistance) }}</p>
                </div>
              </div>
            </div>

            <!-- Reasoning -->
            <div>
              <h3 class="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Reasoning</h3>
              <p class="whitespace-pre-wrap text-sm leading-relaxed text-gray-200">
                {{ decision.reasoning || '—' }}
              </p>
            </div>

            <!-- Counter-thesis & Risks (mandatory opposing view) -->
            <div
              v-if="(decision.counterThesis && decision.counterThesis.trim()) || (decision.keyRisks && decision.keyRisks.length)"
              class="rounded-lg border border-amber-800/60 bg-amber-900/15 p-3"
            >
              <h3 class="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-amber-400">
                <Icon name="heroicons:exclamation-triangle" class="h-4 w-4" />
                Karşıt Görüş & Riskler
              </h3>
              <p
                v-if="decision.counterThesis && decision.counterThesis.trim()"
                class="mb-2 whitespace-pre-wrap text-sm leading-relaxed text-amber-100/90"
              >
                {{ decision.counterThesis }}
              </p>
              <ul v-if="decision.keyRisks && decision.keyRisks.length" class="list-disc space-y-1 pl-5 text-sm text-amber-100/80">
                <li v-for="(risk, i) in decision.keyRisks" :key="i">{{ risk }}</li>
              </ul>
            </div>

            <!-- Summaries -->
            <div v-if="summaries.length" class="space-y-3">
              <div
                v-for="s in summaries"
                :key="s.key"
                class="rounded-lg border border-gray-800 bg-gray-800/30 p-3"
              >
                <h4 class="mb-1.5 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-gray-400">
                  <Icon :name="s.icon" class="h-4 w-4" />
                  {{ s.label }}
                </h4>
                <p class="whitespace-pre-wrap text-sm leading-relaxed text-gray-300">{{ s.text }}</p>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="border-t border-gray-800 px-6 py-3 text-right">
            <button class="btn-secondary text-xs" @click="emit('close')">Close</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
