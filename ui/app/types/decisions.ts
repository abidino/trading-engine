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

export interface AccuracyBucket {
  key: string
  validated: number
  invalidated: number
  hitRate: number
}

export interface AccuracyReport {
  total: number
  validated: number
  invalidated: number
  pending: number
  hitRate: number
  byAction: AccuracyBucket[]
  byType: AccuracyBucket[]
}

export interface DecisionResponse {
  id: string
  ticker: string
  decisionType: string
  action: string
  confidence: number
  reasoning: string
  counterThesis?: string | null
  keyRisks?: string[] | null
  newsSummary?: string | null
  socialSummary?: string | null
  technicalSummary?: string | null
  fundamentalSummary?: string | null
  levels?: TradingLevels | null
  decidedAt: string
  outcome: string
  evaluatedAt?: string | null
}
