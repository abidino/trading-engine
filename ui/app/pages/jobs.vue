<script setup lang="ts">
const { schedulerStatus, recentExecutions, jobHistory, loading, error, fetchSchedulerStatus, fetchRecentExecutions, fetchJobHistory, clearJobHistory, triggerJob } = useJobs()

const selectedJob = ref<string | null>(null)
const triggeringJob = ref<string | null>(null)

onMounted(async () => {
  await Promise.all([fetchSchedulerStatus(), fetchRecentExecutions()])
})

async function selectJob(jobName: string) {
  selectedJob.value = jobName
  await fetchJobHistory(jobName)
}

function clearSelection() {
  selectedJob.value = null
  clearJobHistory()
}

async function handleTrigger(jobName: string) {
  triggeringJob.value = jobName
  await triggerJob(jobName)
  triggeringJob.value = null
  // Refresh history if we're looking at that job
  if (selectedJob.value === jobName) {
    setTimeout(() => fetchJobHistory(jobName), 2500)
  }
}

// Get schedule label for a job
function getSchedule(jobName: string): string {
  if (!schedulerStatus.value) return ''
  const job = schedulerStatus.value.jobs.find(j => j.name === jobName)
  return job?.schedule || ''
}

function getScheduleLabel(jobName: string): string {
  const schedule = getSchedule(jobName)
  if (!schedule) return ''
  if (schedule.startsWith('every')) return schedule
  // cron: show raw
  return `cron: ${schedule}`
}

const formatTime = (iso?: string) =>
  iso ? new Date(iso).toLocaleString() : '—'

const formatDuration = (ms?: number) => {
  if (!ms) return '—'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

const statusColor = (status: string) => {
  switch (status) {
    case 'SUCCESS': return 'badge-green'
    case 'FAILED': return 'badge-red'
    case 'RUNNING': return 'badge-blue'
    default: return 'badge-yellow'
  }
}

// Group executions by job name for summary
const jobSummary = computed(() => {
  const map = new Map<string, { lastRun?: string; lastStatus?: string; totalRuns: number; failCount: number }>()
  
  // Initialize from scheduler status (all registered jobs)
  if (schedulerStatus.value) {
    for (const job of schedulerStatus.value.jobs) {
      map.set(job.name, { totalRuns: 0, failCount: 0 })
    }
  }

  // Aggregate from executions
  for (const exec of recentExecutions.value) {
    const existing = map.get(exec.jobName) || { totalRuns: 0, failCount: 0 }
    existing.totalRuns++
    if (exec.status === 'FAILED') existing.failCount++
    if (!existing.lastRun || exec.startedAt > existing.lastRun) {
      existing.lastRun = exec.startedAt
      existing.lastStatus = exec.status
    }
    map.set(exec.jobName, existing)
  }

  return Array.from(map.entries()).map(([name, data]) => ({ name, ...data }))
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Scheduler Jobs</h1>
        <p class="text-sm text-gray-400">Monitor scheduled background tasks</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="flex items-center gap-2">
          <span
            class="h-2.5 w-2.5 rounded-full"
            :class="schedulerStatus?.running ? 'bg-green-500 animate-pulse' : 'bg-red-500'"
          />
          <span class="text-sm text-gray-300">
            {{ schedulerStatus?.running ? 'Running' : 'Stopped' }}
          </span>
        </div>
        <button class="btn-secondary" @click="fetchRecentExecutions()">
          <Icon name="heroicons:arrow-path" class="mr-1.5 h-4 w-4" />
          Refresh
        </button>
      </div>
    </div>

    <!-- Job Summary Grid -->
    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
      <div
        v-for="job in jobSummary"
        :key="job.name"
        class="card cursor-pointer transition-colors hover:border-blue-700"
        :class="selectedJob === job.name ? 'border-blue-600' : ''"
        @click="selectJob(job.name)"
      >
        <div class="flex items-start justify-between">
          <div>
            <h4 class="text-sm font-semibold text-white">{{ job.name }}</h4>
            <p class="mt-0.5 text-xs text-blue-400">{{ getScheduleLabel(job.name) }}</p>
            <p class="mt-1 text-xs text-gray-400">
              Last: {{ job.lastRun ? formatTime(job.lastRun) : 'Never' }}
            </p>
          </div>
          <span v-if="job.lastStatus" :class="statusColor(job.lastStatus)">
            {{ job.lastStatus }}
          </span>
          <span v-else class="badge-yellow">PENDING</span>
        </div>
        <div class="mt-3 flex items-center justify-between">
          <div class="flex items-center gap-4 text-xs text-gray-500">
            <span>Runs: {{ job.totalRuns }}</span>
            <span v-if="job.failCount > 0" class="text-red-400">Fails: {{ job.failCount }}</span>
          </div>
          <button
            class="btn-secondary !px-2 !py-1 text-xs"
            :disabled="triggeringJob === job.name"
            @click.stop="handleTrigger(job.name)"
          >
            <Icon v-if="triggeringJob === job.name" name="heroicons:arrow-path" class="mr-1 h-3 w-3 animate-spin" />
            <Icon v-else name="heroicons:play" class="mr-1 h-3 w-3" />
            Run
          </button>
        </div>
      </div>
    </div>

    <!-- Job Detail / History -->
    <div v-if="selectedJob" class="card">
      <div class="card-header">
        <h3 class="card-title">
          <span class="text-blue-400">{{ selectedJob }}</span> — Execution History
        </h3>
        <button class="btn-secondary text-xs" @click="clearSelection">
          <Icon name="heroicons:x-mark" class="mr-1 h-3 w-3" />
          Close
        </button>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-800 text-left text-gray-400">
              <th class="pb-3 font-medium">Status</th>
              <th class="pb-3 font-medium">Started</th>
              <th class="pb-3 font-medium">Duration</th>
              <th class="pb-3 font-medium">Items</th>
              <th class="pb-3 font-medium">Error</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-800">
            <tr v-for="exec in jobHistory" :key="exec.id">
              <td class="py-3">
                <span :class="statusColor(exec.status)">{{ exec.status }}</span>
              </td>
              <td class="py-3 text-gray-300">{{ formatTime(exec.startedAt) }}</td>
              <td class="py-3 text-gray-300">{{ formatDuration(exec.durationMs) }}</td>
              <td class="py-3 text-gray-300">{{ exec.itemsProcessed }}</td>
              <td class="py-3 max-w-xs truncate text-red-400">{{ exec.errorMessage || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!jobHistory.length" class="py-6 text-center text-sm text-gray-500">
          No execution history for this job yet.
        </p>
      </div>
    </div>

    <!-- Recent Executions (all jobs) -->
    <div v-if="!selectedJob" class="card">
      <div class="card-header">
        <h3 class="card-title">Recent Executions (All Jobs)</h3>
      </div>

      <div v-if="loading" class="flex items-center justify-center py-10">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
      </div>

      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-800 text-left text-gray-400">
              <th class="pb-3 font-medium">Job</th>
              <th class="pb-3 font-medium">Status</th>
              <th class="pb-3 font-medium">Started</th>
              <th class="pb-3 font-medium">Duration</th>
              <th class="pb-3 font-medium">Items</th>
              <th class="pb-3 font-medium">Error</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-800">
            <tr v-for="exec in recentExecutions" :key="exec.id">
              <td class="py-3">
                <button
                  class="font-medium text-blue-400 hover:underline"
                  @click="selectJob(exec.jobName)"
                >
                  {{ exec.jobName }}
                </button>
              </td>
              <td class="py-3">
                <span :class="statusColor(exec.status)">{{ exec.status }}</span>
              </td>
              <td class="py-3 text-gray-300">{{ formatTime(exec.startedAt) }}</td>
              <td class="py-3 text-gray-300">{{ formatDuration(exec.durationMs) }}</td>
              <td class="py-3 text-gray-300">{{ exec.itemsProcessed }}</td>
              <td class="py-3 max-w-xs truncate text-red-400">{{ exec.errorMessage || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!recentExecutions.length" class="py-6 text-center text-sm text-gray-500">
          No job executions yet. Jobs will appear here after the scheduler runs them.
        </p>
      </div>
    </div>
  </div>
</template>
