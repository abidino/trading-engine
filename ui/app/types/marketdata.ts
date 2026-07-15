export interface TechnicalIndicators {
  ticker: string
  asOfDate: string
  close: number
  ema9: number | null
  ema20: number | null
  ema50: number | null
  ema100: number | null
  ema200: number | null
  rsi14: number | null
  macd: number | null
  macdSignal: number | null
  macdHistogram: number | null
  dataPoints: number
}

export interface TrendAnalysis {
  ticker: string
  analysisDate: string
  trend: string
  confidence: number
  reasoning: string
  indicators: TechnicalIndicators
  llmModel: string
  createdAt: string
}

export interface SupportResistance {
  ticker: string
  asOfDate: string
  close: number
  pivot: number
  r1: number
  r2: number
  r3: number
  s1: number
  s2: number
  s3: number
  supports: number[]
  resistances: number[]
  nearestSupport: number | null
  nearestResistance: number | null
  dataPoints: number
}

export interface IntradayQuote {
  ticker: string
  session: string
  price: number
  previousClose: number
  change: number
  changePercent: number | null
  volume: number
  quoteTime: string
  capturedAt: string
}
