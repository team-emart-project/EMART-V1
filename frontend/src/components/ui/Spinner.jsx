import { Loader2 } from 'lucide-react';
import { cn } from '@/utils/formatters';

export default function Spinner({ className, label = 'Loading' }) {
  return (
    <span role="status" aria-label={label}>
      <Loader2 className={cn('h-5 w-5 animate-spin text-brand-600', className)} />
    </span>
  );
}
