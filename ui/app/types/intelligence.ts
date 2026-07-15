export interface NewsTag {
  ticker: string
  sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL'
  interpretation: string
}

export interface NewsRecord {
  url?: string
  headline: string
  source: string
  category: string
  summary?: string
  publishedAt: string
  tags: NewsTag[]
}

export interface SocialSignal {
  ticker: string
  source: string
  content: string
  engagementScore: number
  sentimentScore: number
}
