import { AnimatePresence, motion } from 'framer-motion';
import { CheckCircle2, Info, XCircle, X } from 'lucide-react';
import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { dismissToast } from '@/store/slices/uiSlice';

const ICONS = { success: CheckCircle2, error: XCircle, info: Info };
const TONES = {
  success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  error: 'border-rose-200 bg-rose-50 text-rose-800',
  info: 'border-brand-200 bg-brand-50 text-brand-800',
};

function ToastItem({ toast }) {
  const dispatch = useDispatch();
  const Icon = ICONS[toast.type] || Info;

  useEffect(() => {
    const timer = setTimeout(() => dispatch(dismissToast(toast.id)), 4000);
    return () => clearTimeout(timer);
  }, [dispatch, toast.id]);

  return (
    <motion.div
      layout
      initial={{ opacity: 0, x: 40, scale: 0.95 }}
      animate={{ opacity: 1, x: 0, scale: 1 }}
      exit={{ opacity: 0, x: 40, scale: 0.95 }}
      transition={{ type: 'spring', stiffness: 350, damping: 28 }}
      className={`pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 shadow-lg ${TONES[toast.type] || TONES.info}`}
      role="status"
    >
      <Icon className="mt-0.5 h-4 w-4 shrink-0" />
      <p className="flex-1 text-sm">{toast.message}</p>
      <button onClick={() => dispatch(dismissToast(toast.id))} aria-label="Dismiss">
        <X className="h-4 w-4 opacity-60 hover:opacity-100" />
      </button>
    </motion.div>
  );
}

/** Mounted once in App; any slice can raise a toast via pushToast(). */
export default function ToastContainer() {
  const toasts = useSelector((state) => state.ui.toasts);

  return (
    <div className="pointer-events-none fixed right-4 top-20 z-[60] flex w-full max-w-sm flex-col gap-2">
      <AnimatePresence mode="popLayout">
        {toasts.map((t) => <ToastItem key={t.id} toast={t} />)}
      </AnimatePresence>
    </div>
  );
}
