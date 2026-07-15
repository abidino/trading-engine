import type { FilterCatalog, FilterDef } from '~/types'

/**
 * Finviz filter helpers — the single source of truth is the backend catalog
 * (`GET /discovery/filter-options`), scraped verbatim from Finviz's own dropdowns.
 *
 * The Finviz free screener honours exactly ONE token per filter family (no ranges,
 * no multi-select), so the UI state is simply `filterKey -> token`. These pure
 * helpers derive the active-criteria pills and default preset from that same map,
 * so pills, payload and query can never drift apart.
 */

export type Selections = Record<string, string>

/** Growth / financially-healthy preset — mirrors backend DiscoveryFilter.defaults(). */
export function defaultSelections(): Selections {
  return {
    country: 'usa',
    marketCap: 'smallover',
    price: 'o5',
    pe: 'profitable',
    peg: 'u2',
    relVol: 'o1.5',
    roe: 'o10',
    debtEq: 'u1',
    currentRatio: 'o1.5',
    targetPrice: 'a5',
  }
}

/** Flat index of all filter defs by key, built from the catalog. */
export function indexFilters(catalog: FilterCatalog | null): Map<string, FilterDef> {
  const map = new Map<string, FilterDef>()
  if (!catalog) return map
  for (const g of catalog.groups) {
    for (const f of g.filters) map.set(f.key, f)
  }
  return map
}

/** Human-readable label for a selected option (e.g. "P/E: Under 20"). */
export function optionLabel(def: FilterDef | undefined, token: string): string {
  if (!def) return token
  const opt = def.options.find(o => o.token === token)
  return opt ? opt.label : token
}

export interface ActivePill {
  key: string
  label: string
}

/** Active-criteria pills derived from the selections + catalog (single source of truth). */
export function buildActivePills(selections: Selections, catalog: FilterCatalog | null): ActivePill[] {
  const idx = indexFilters(catalog)
  const pills: ActivePill[] = []
  for (const [key, token] of Object.entries(selections)) {
    if (!token) continue
    const def = idx.get(key)
    pills.push({ key, label: `${def?.label ?? key}: ${optionLabel(def, token)}` })
  }
  return pills
}

/** API payload for ad-hoc run / save. */
export function toPayload(selections: Selections, rawFinvizFilters?: string) {
  const clean: Selections = {}
  for (const [k, v] of Object.entries(selections)) {
    if (v != null && v !== '') clean[k] = v
  }
  return { selections: clean, rawFinvizFilters: rawFinvizFilters?.trim() || undefined }
}
