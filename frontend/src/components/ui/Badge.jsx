import { cn } from '@/utils/formatters';

export default function Badge({ children, className, tone = 'slate' }) {
  const tones = {
    slate: 'bg-slate-100 text-slate-700',
    brand: 'bg-brand-50 text-brand-800',
    accent: 'bg-accent-500/15 text-accent-700',
    success: 'bg-brand-100 text-brand-800',
    danger: 'bg-rose-100 text-rose-700',
  };

  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        tones[tone],
        className
      )}
    >
      {children}
    </span>
  );
}
