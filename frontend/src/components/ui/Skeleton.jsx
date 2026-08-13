import { cn } from '@/utils/formatters';

/**
 * Shimmering placeholder shown while data loads.
 *
 * Preferred over a spinner for lists: it keeps the page height stable, so
 * content does not jump when the data arrives.
 */
export default function Skeleton({ className }) {
  return <div className={cn('shimmer rounded-lg', className)} aria-hidden />;
}

export function ProductCardSkeleton() {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <Skeleton className="mb-4 aspect-square w-full" />
      <Skeleton className="mb-2 h-4 w-3/4" />
      <Skeleton className="mb-3 h-3 w-1/2" />
      <Skeleton className="h-6 w-1/3" />
    </div>
  );
}

export function ListSkeleton({ rows = 3 }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} className="h-20 w-full" />
      ))}
    </div>
  );
}
