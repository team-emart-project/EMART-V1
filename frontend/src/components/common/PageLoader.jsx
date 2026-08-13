import { motion } from 'framer-motion';

/** Suspense fallback while a lazily-loaded route chunk downloads. */
export default function PageLoader() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4">
      <motion.div
        animate={{ rotate: 360 }}
        transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
        className="h-10 w-10 rounded-full border-[3px] border-brand-100 border-t-brand-600"
      />
      <p className="text-sm text-slate-400">Loading…</p>
    </div>
  );
}
