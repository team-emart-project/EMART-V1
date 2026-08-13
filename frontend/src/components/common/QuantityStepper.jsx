import { Minus, Plus } from 'lucide-react';

export default function QuantityStepper({ value, onChange, min = 1, max = 99, disabled }) {
  return (
    <div className="inline-flex items-center rounded-xl border border-slate-300 bg-white">
      <button
        onClick={() => onChange(Math.max(min, value - 1))}
        disabled={disabled || value <= min}
        className="p-2 text-slate-600 hover:bg-slate-50 disabled:opacity-40 rounded-l-xl"
        aria-label="Decrease quantity"
      >
        <Minus className="h-3.5 w-3.5" />
      </button>

      <span className="w-10 text-center text-sm font-medium tabular-nums">{value}</span>

      <button
        onClick={() => onChange(Math.min(max, value + 1))}
        disabled={disabled || value >= max}
        className="p-2 text-slate-600 hover:bg-slate-50 disabled:opacity-40 rounded-r-xl"
        aria-label="Increase quantity"
      >
        <Plus className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}
