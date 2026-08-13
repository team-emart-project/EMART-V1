import { forwardRef, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { cn } from '@/utils/formatters';

/**
 * Reusable text input with label, error and optional password reveal.
 *
 * forwardRef so react-hook-form or a manual .focus() still works.
 */
const Input = forwardRef(function Input(
  { label, error, hint, type = 'text', icon: Icon, className, required, ...props },
  ref
) {
  const [reveal, setReveal] = useState(false);
  const isPassword = type === 'password';
  const inputType = isPassword && reveal ? 'text' : type;

  return (
    <div className="w-full">
      {label && (
        <label className="mb-1.5 block text-sm font-medium text-slate-700">
          {label}
          {required && <span className="ml-0.5 text-rose-500">*</span>}
        </label>
      )}

      <div className="relative">
        {Icon && (
          <Icon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        )}

        <input
          ref={ref}
          type={inputType}
          className={cn(
            'w-full rounded-xl border bg-white px-3.5 py-2.5 text-sm text-slate-800',
            'placeholder:text-slate-400 transition-all duration-200',
            'focus:outline-none focus:ring-2 focus:ring-brand-500/30',
            Icon && 'pl-9',
            isPassword && 'pr-10',
            error
              ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-500/20'
              : 'border-slate-300 focus:border-brand-500',
            className
          )}
          aria-invalid={Boolean(error)}
          {...props}
        />

        {isPassword && (
          <button
            type="button"
            onClick={() => setReveal((v) => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            aria-label={reveal ? 'Hide password' : 'Show password'}
            tabIndex={-1}
          >
            {reveal ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        )}
      </div>

      {error && <p className="mt-1 text-xs text-rose-600 animate-fade-in">{error}</p>}
      {!error && hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </div>
  );
});

export default Input;
