// ─── Portfolio ───────────────────────────────────────────────────────────────

export interface SoldPosition {
  ticker: string
  totalQuantity: number
  avgBuyPrice: number
  avgSellPrice: number
  totalBuyCommission: number
  totalSellCommission: number
  totalCommission: number
  realizedPnl: number
  soldAt?: string
}

export interface Position {
  id: string
  ticker: string
  quantity: number
  averageCost: number
  currentPrice: number
  marketValue: number
  unrealizedPnl: number
  unrealizedPnlPercent: number
  sector?: string
  assetType: string
}

export interface Transaction {
  id: string
  ticker: string
  transactionType: 'BUY' | 'SELL'
  quantity: number
  price: number
  commission: number
  notes?: string
  executedAt: string
}

export interface PortfolioSummaryStats {
  totalMarketValue: number
  totalGains: number
  totalLosses: number
  totalPnl: number
  netPnl: number
  totalCommissions: number
  positionCount: number
  gainPositionCount: number
  lossPositionCount: number
}

// ─── Dashboard ──────────────────────────────────────────────────────────────

export interface PortfolioSummary {
  totalMarketValue: number
  totalCostBasis: number
  totalUnrealizedPnl: number
  totalUnrealizedPnlPercent: number
  positionCount: number
  sectorAllocation: Record<string, number>
  topGainers: TickerPnl[]
  topLosers: TickerPnl[]
  watchlistCount: number
  decisionAccuracy: number
}

export interface TickerPnl {
  ticker: string
  pnl: number
  pnlPercent: number
}

export interface SectorBreakdown {
  sector: string
  totalValue: number
  percentage: number
  tickers: string[]
}

export interface PerformanceEntry {
  ticker: string
  marketValue: number
  costBasis: number
  unrealizedPnl: number
  unrealizedPnlPercent: number
  decisionCount: number
  winRate: number
}

// ─── Watchlist ──────────────────────────────────────────────────────────────

export interface WatchlistItem {
  id: string
  ticker: string
  addedAt: string
  targetPrice?: string | null
  stopLoss?: string | null
  notes?: string | null
  approved: boolean
}

// ─── Analysis ───────────────────────────────────────────────────────────────

export interface AnalysisRun {
  runId: string
  ticker: string
  status: 'running' | 'completed' | 'failed'
  startedAt: string
  completedAt?: string
  result?: AnalysisResult
  error?: string
}

export interface TradingLevels {
  entryLow?: number | null
  entryHigh?: number | null
  aggressiveEntry?: number | null
  idealEntry?: number | null
  safeEntry?: number | null
  stopLoss?: number | null
  takeProfit?: number | null
  nearestSupport?: number | null
  nearestResistance?: number | null
}

export type AnalysisRequestType = 'PORTFOLIO_REVIEW' | 'WATCHLIST_REVIEW' | 'DISCOVERY'

export interface AnalysisResult {
  action: 'BUY' | 'SELL' | 'HOLD' | string
  confidence: number
  reasoning: string
  technicalSummary?: string
  fundamentalSummary?: string
  newsSummary?: string
  socialSummary?: string
  counterThesis?: string
  keyRisks?: string[]
  decidedAt?: string
  levels?: TradingLevels | null
  analystReports: Record<string, string>
}

// ─── Discovery ──────────────────────────────────────────────────────────────

// A saved screener filter. Mirrors the Finviz free screener's single-select model:
// `selections` maps each filter key to ONE exact Finviz option token.
export interface DiscoveryFilter {
  id: string
  name: string
  description?: string
  active: boolean
  selections: Record<string, string>
  rawFinvizFilters?: string
  createdAt: string
}

// ─── Finviz filter catalog (drives the UI dropdowns; scraped from Finviz) ─────

export interface FilterOption {
  token: string
  label: string
}

export interface FilterDef {
  key: string
  label: string
  group: string
  prefix: string
  options: FilterOption[]
}

export interface FilterCatalogGroup {
  group: string
  filters: FilterDef[]
}

export interface FilterCatalog {
  groups: FilterCatalogGroup[]
}

export interface DiscoveredStock {
  ticker: string
  companyName: string
  sector: string
  marketCap: number
  peRatio?: number
  score: number
  status: string
  recommended: boolean
  confidence?: number
  reasoning?: string
  trendDirection?: string
  signals: string[]
  discoveredAt: string
  evaluatedAt?: string
}

// ─── Decisions ──────────────────────────────────────────────────────────────

export interface Decision {
  id: string
  ticker: string
  action: 'BUY' | 'SELL' | 'HOLD'
  confidence: number
  reasoning: string
  outcome?: 'WIN' | 'LOSS' | 'PENDING'
  createdAt: string
  evaluatedAt?: string
}

// ─── Events ─────────────────────────────────────────────────────────────────

export type EventType =
  | 'price_updated'
  | 'alert_triggered'
  | 'decision_created'
  | 'analysis_started'
  | 'analysis_completed'
  | 'analysis_failed'
  | 'discovery_found'
  | 'stock_promoted'
  | 'job_completed'
  | 'job_failed'

export interface ServerEvent {
  type: EventType
  data: Record<string, unknown>
  timestamp: string
}

// ─── Jobs ───────────────────────────────────────────────────────────────────

export interface ScheduledJob {
  id: string
  name: string
  nextRunTime?: string
  lastRunTime?: string
  status: 'active' | 'paused'
  intervalSeconds?: number
}

// ─── Market Data ────────────────────────────────────────────────────────────

export interface MarketDataResponse {
  ticker: string
  date: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export interface PriceData {
  ticker: string
  price: number
  change: number
  changePercent: number
  volume: number
  timestamp: string
}

// ─── API Response ───────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  data: T
  message?: string
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}
