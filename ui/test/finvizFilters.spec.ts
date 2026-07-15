import { describe, it, expect } from 'vitest'
import {
  defaultSelections, buildActivePills, indexFilters, optionLabel, toPayload,
  type Selections,
} from '~/composables/useFinvizFilters'
import type { FilterCatalog } from '~/types'

// Minimal catalog mirroring the backend shape.
const catalog: FilterCatalog = {
  groups: [
    {
      group: 'Valuation',
      filters: [
        { key: 'pe', label: 'P/E', group: 'Valuation', prefix: 'fa_pe_', options: [
          { token: 'u20', label: 'Under 20' }, { token: 'profitable', label: 'Profitable (>0)' },
        ] },
        { key: 'peg', label: 'PEG', group: 'Valuation', prefix: 'fa_peg_', options: [
          { token: 'u2', label: 'Under 2' },
        ] },
      ],
    },
    {
      group: 'Universe',
      filters: [
        { key: 'sector', label: 'Sector', group: 'Universe', prefix: 'sec_', options: [
          { token: 'technology', label: 'Technology' },
        ] },
      ],
    },
  ],
}

describe('useFinvizFilters — faithful single-select mirror', () => {
  it('defaults use only real Finviz tokens', () => {
    const d = defaultSelections()
    expect(d.country).toBe('usa')
    expect(d.peg).toBe('u2')
    expect(d.targetPrice).toBe('a5')
  })

  it('indexFilters flattens all groups by key', () => {
    const idx = indexFilters(catalog)
    expect(idx.get('pe')?.prefix).toBe('fa_pe_')
    expect(idx.get('sector')?.label).toBe('Sector')
    expect(idx.size).toBe(3)
  })

  it('optionLabel resolves the human label', () => {
    const idx = indexFilters(catalog)
    expect(optionLabel(idx.get('pe'), 'u20')).toBe('Under 20')
    expect(optionLabel(idx.get('pe'), 'unknown')).toBe('unknown')
  })

  it('active pills derive from the SAME selections as the payload (no drift)', () => {
    const sel: Selections = { pe: 'u20', sector: 'technology' }
    const pills = buildActivePills(sel, catalog)
    expect(pills.map(p => p.label)).toEqual(['P/E: Under 20', 'Sector: Technology'])

    const payload = toPayload(sel)
    // Every pill corresponds to a key in the payload selections (single source of truth).
    for (const p of pills) expect(payload.selections[p.key]).toBeTruthy()
  })

  it('toPayload strips empty values and trims raw filters', () => {
    const payload = toPayload({ pe: 'u20', peg: '' }, '  sh_avgvol_o500 ')
    expect(payload.selections).toEqual({ pe: 'u20' })
    expect(payload.rawFinvizFilters).toBe('sh_avgvol_o500')
  })
})
