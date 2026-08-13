import { useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronRight, Folder } from 'lucide-react';
import { Link } from 'react-router-dom';

/**
 * Renders the nested category tree the backend builds from the flat
 * cat_id / subcat_id codes. Recursive, so it handles any depth.
 */
function TreeNode({ node, depth = 0 }) {
  const [open, setOpen] = useState(depth === 0);
  const hasChildren = node.children?.length > 0;

  return (
    <li>
      <div
        className="flex items-center gap-1 rounded-lg px-2 py-1.5 transition-colors hover:bg-brand-50"
        style={{ paddingLeft: `${depth * 14 + 8}px` }}
      >
        {hasChildren ? (
          <button
            onClick={() => setOpen((v) => !v)}
            aria-label={open ? 'Collapse' : 'Expand'}
            className="rounded p-0.5 text-slate-400 hover:text-brand-600"
          >
            <motion.span animate={{ rotate: open ? 90 : 0 }} className="block">
              <ChevronRight className="h-3.5 w-3.5" />
            </motion.span>
          </button>
        ) : (
          <span className="w-[18px]" />
        )}

        <Link
          to={`/categories/${node.catmasterId}`}
          className="flex flex-1 items-center gap-2 text-sm text-slate-700 hover:text-brand-600"
        >
          <Folder className="h-3.5 w-3.5 text-brand-400" />
          {node.catName}
        </Link>
      </div>

      <AnimatePresence initial={false}>
        {hasChildren && open && (
          <motion.ul
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            {node.children.map((child) => (
              <TreeNode key={child.catmasterId} node={child} depth={depth + 1} />
            ))}
          </motion.ul>
        )}
      </AnimatePresence>
    </li>
  );
}

export default function CategoryTree({ categories = [] }) {
  if (!categories.length) return null;
  return (
    <ul className="space-y-0.5">
      {categories.map((c) => <TreeNode key={c.catmasterId} node={c} />)}
    </ul>
  );
}
