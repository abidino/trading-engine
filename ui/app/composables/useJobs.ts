export interface JobExecution {
  id: string
  jobName: string
  status: string
  startedAt: string
  finishedAt?: string
  durationMs?: number
  itemsProcessed: number
  errorMessage?: string
}

export interface JobInfo {
  name: string
  triggerType: string
  schedule: string
}

export interface SchedulerStatus {
  running: boolean
  jobs: JobInfo[]
}

export function useJobs() {
  const api = useApi()

  const schedulerStatus = ref<SchedulerStatus | null>(null)
  const recentExecutions = ref<JobExecution[]>([])
  const jobHistory = ref<JobExecution[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchSchedulerStatus() {
    try {
      schedulerStatus.value = await api.get<SchedulerStatus>('/scheduler/status')
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch scheduler status'
    }
  }

  async function fetchRecentExecutions(limit: number = 50) {
    loading.value = true
    error.value = null
    try {
      recentExecutions.value = await api.get<JobExecution[]>('/jobs/recent', { limit: String(limit) })
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch recent jobs'
    } finally {
      loading.value = false
    }
  }

  async function fetchJobHistory(jobName: string, limit: number = 20) {
    try {
      jobHistory.value = await api.get<JobExecution[]>(`/jobs/${jobName}`, { limit: String(limit) })
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch job history'
    }
  }

  function clearJobHistory() {
    jobHistory.value = []
  }

  async function triggerJob(jobName: string) {
    try {
      await api.post(`/jobs/${jobName}/trigger`)
      // Wait a moment for the job to complete, then refresh
      setTimeout(async () => {
        await fetchRecentExecutions()
      }, 2000)
      return true
    } catch (e: any) {
      error.value = e.message || `Failed to trigger job: ${jobName}`
      return false
    }
  }

  return {
    schedulerStatus: readonly(schedulerStatus),
    recentExecutions: readonly(recentExecutions),
    jobHistory: readonly(jobHistory),
    loading: readonly(loading),
    error: readonly(error),
    fetchSchedulerStatus,
    fetchRecentExecutions,
    fetchJobHistory,
    clearJobHistory,
    triggerJob,
  }
}
