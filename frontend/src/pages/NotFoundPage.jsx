import { motion } from 'framer-motion';
import { Home, SearchX } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center px-4 text-center">
      <motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }}>
        <span className="inline-flex rounded-3xl bg-brand-50 p-6">
          <SearchX className="h-12 w-12 text-brand-500" />
        </span>
      </motion.div>

      <h1 className="mt-6 text-6xl font-bold text-gradient">404</h1>
      <p className="mt-2 text-lg font-semibold text-slate-800">Page not found</p>
      <p className="mt-1 max-w-sm text-sm text-slate-500">
        The page you are looking for does not exist or has moved.
      </p>

      <div className="mt-6 flex gap-3">
        <Button as={Link} to="/"><Home className="h-4 w-4" /> Back home</Button>
        <Button as={Link} to="/products" variant="outline">Browse products</Button>
      </div>
    </div>
  );
}
