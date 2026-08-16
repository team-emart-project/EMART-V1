import { motion } from 'framer-motion';
import { Loader2 } from 'lucide-react';
import { cn } from '@/utils/formatters';

/**
 * Reusable button.
 *
 * Every variant and size lives here, so a design change is one edit rather than
 * a search across the whole app.
 *
 * `as` lets the same styling render a <button>, a react-router <Link>, or an
 * <a>. Framer's animation props are only passed to a real motion component —
 * forwarding whileTap to a <Link> would spray unknown attributes onto the DOM
 * and log React warnings.
 */
const VARIANTS = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 shadow-sm shadow-brand-600/20',
  accent: 'bg-accent-500 text-white hover:bg-accent-600 shadow-sm shadow-accent-500/25',
  outline: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 hover:border-slate-400',
  ghost: 'text-slate-600 hover:bg-slate-100',
  danger: 'bg-rose-600 text-white hover:bg-rose-700',
  subtle: 'bg-brand-50 text-brand-700 hover:bg-brand-100',
};

const SIZES = {
  sm: 'px-3 py-1.5 text-sm gap-1.5',
  md: 'px-4 py-2.5 text-sm gap-2',
  lg: 'px-6 py-3 text-base gap-2',
  icon: 'p-2',
};

export default function Button({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  fullWidth = false,
  className,
  as: Component,
  ...props
}) {
  const isDisabled = disabled || loading;
  const isNativeButton = !Component;
  const Tag = Component || motion.button;

  const classes = cn(
    'inline-flex items-center justify-center rounded-xl font-medium',
    'transition-all duration-200 select-none',
    isDisabled && 'opacity-50 pointer-events-none',
    !isNativeButton && 'hover:-translate-y-px active:translate-y-0',
    VARIANTS[variant],
    SIZES[size],
    fullWidth && 'w-full',
    className
  );

  // Motion props only make sense on a motion component.
  const motionProps = isNativeButton
    ? {
        whileTap: isDisabled ? undefined : { scale: 0.97 },
        whileHover: isDisabled ? undefined : { y: -1 },
        transition: { type: 'spring', stiffness: 400, damping: 25 },
        disabled: isDisabled,
      }
    : { 'aria-disabled': isDisabled || undefined };

  return (
    <Tag className={classes} {...motionProps} {...props}>
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
      {children}
    </Tag>
  );
}
