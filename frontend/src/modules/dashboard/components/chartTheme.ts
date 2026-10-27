// filepath: /frontend/src/modules/dashboard/components/chartTheme.ts
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js'

// Registrasi sekali (tree-shaking: hanya elemen yang dipakai)
Chart.register(ArcElement, BarElement, CategoryScale, Filler, Legend, LineElement, LinearScale, PointElement, Tooltip)

Chart.defaults.font.family = "'Plus Jakarta Sans', ui-sans-serif, system-ui, sans-serif"
Chart.defaults.font.size = 12
Chart.defaults.color = '#6b6860' // teks sekunder (arang-600), bukan warna seri

/**
 * Warna seri tren: 3 slot pertama palet kategorikal tervalidasi (lolos uji CVD semua pasangan).
 * Teks/label tetap memakai warna teks, bukan warna seri.
 */
export const SERIES = {
  income: '#1BAF7A',
  spending: '#EB6834',
  saving: '#2A78D6',
} as const

export const SURFACE = '#ffffff'
export const GRID = '#ece3d0' // krem-200: grid samar
export const OTHER_COLOR = '#9A968C'

export const tooltipStyle = {
  backgroundColor: '#2b2a26',
  titleColor: '#ffffff',
  bodyColor: '#ffffff',
  padding: 10,
  cornerRadius: 10,
  boxPadding: 4,
  usePointStyle: true,
} as const
