import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/utils/formatters';

/** Works directly with the backend's PageResponse shape. */
export default function Pagination({ page = 0, totalPages = 1, onChange }) {
  if (totalPages <= 1) return null;

  // Window of at most 5 page numbers around the current one.
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => start + i);

  return (
    <nav className="mt-8 flex items-center justify-center gap-1" aria-label="Pagination">
      <button
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
        className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-40"
        aria-label="Previous page"
      >
        <ChevronLeft className="h-4 w-4" />
      </button>

      {pages.map((p) => (
        <button
          key={p}
          onClick={() => onChange(p)}
          aria-current={p === page ? 'page' : undefined}
          className={cn(
            'h-9 min-w-9 rounded-lg px-3 text-sm font-medium transition-colors',
<<<<<<< HEAD
            p === page ? 'bg-brand-600 text-white' : 'text-slate-600 hover:bg-slate-100'
=======
            p === page ? 'bg-brand-gradient text-white shadow-sm shadow-brand-600/25' : 'text-slate-600 hover:bg-slate-100'
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
          )}
        >
          {p + 1}
        </button>
      ))}

      <button
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-40"
        aria-label="Next page"
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </nav>
  );
}
