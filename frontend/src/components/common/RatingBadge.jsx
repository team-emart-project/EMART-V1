import { Star } from 'lucide-react';

/**
 * Flipkart-style rating chip: "4.3 ★" in a green pill, with the review count
 * beside it. Green above 3.5, amber below — a low score should not look like
 * an endorsement.
 */
export default function RatingBadge({ rating, count, size = 'sm' }) {
  const value = Number(rating ?? 0);
  if (!value) return null;

  const good = value >= 3.5;
  const pad = size === 'sm' ? 'px-1.5 py-0.5 text-[11px]' : 'px-2 py-1 text-xs';

  return (
    <span className="inline-flex items-center gap-1.5">
      <span className={`inline-flex items-center gap-0.5 rounded font-semibold text-white ${pad} ${
        good ? 'bg-brand-600' : 'bg-amber-500'}`}>
        {value.toFixed(1)}
        <Star className="h-2.5 w-2.5 fill-white" />
      </span>
      {count > 0 && (
        <span className="text-[11px] font-medium text-slate-400">
          ({count.toLocaleString('en-IN')})
        </span>
      )}
    </span>
  );
}
