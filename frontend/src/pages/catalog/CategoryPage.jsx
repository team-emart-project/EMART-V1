import { useMemo, useState } from 'react';
import { ChevronRight } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { EmptyState, Pagination, ProductCardSkeleton } from '@/components/ui';
import ProductCard from '@/components/common/ProductCard';
import { useFetch } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';

/**
 * Products inside a category.
 *
 * Note these three calls all use the SAME useFetch hook — a category by id, its
 * sub-categories, and its products. No separate "getById" hook exists.
 */
export default function CategoryPage() {
  const { catmasterId } = useParams();
  const [page, setPage] = useState(0);

  const { data: category } = useFetch(endpoints.categories.byId(catmasterId));
  const { data: subCategories } = useFetch(endpoints.categories.subCategories(catmasterId));

  const productsUrl = useMemo(() => {
    const qs = new URLSearchParams({ includeSubCategories: 'true', page, size: 12 });
    return endpoints.categories.products(catmasterId, qs.toString());
  }, [catmasterId, page]);

  const { data, loading } = useFetch(productsUrl);

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <nav className="mb-4 flex items-center gap-1 text-sm text-slate-500">
        <Link to="/" className="hover:text-brand-600">Home</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <Link to="/products" className="hover:text-brand-600">Shop</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="font-medium text-slate-700">{category?.catName || '…'}</span>
      </nav>

      <h1 className="text-3xl font-bold text-slate-900">{category?.catName || 'Category'}</h1>
      <p className="mt-1 text-sm text-slate-500">
        {data ? `${data.totalElements} product${data.totalElements === 1 ? '' : 's'}` : 'Loading…'}
        {' · includes sub-categories'}
      </p>

      {subCategories?.length > 0 && (
        <div className="mt-6 flex flex-wrap gap-2">
          {subCategories.map((sc) => (
            <Link key={sc.catmasterId} to={`/categories/${sc.catmasterId}`}
              className="rounded-full border border-slate-200 bg-white px-4 py-1.5 text-sm text-slate-700 transition-colors hover:border-brand-300 hover:bg-brand-50 hover:text-brand-700">
              {sc.catName}
            </Link>
          ))}
        </div>
      )}

      <div className="mt-8">
        {loading ? (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
            {Array.from({ length: 8 }).map((_, i) => <ProductCardSkeleton key={i} />)}
          </div>
        ) : data?.content?.length ? (
          <>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
              {data.content.map((p, i) => <ProductCard key={p.prodId} product={p} index={i} />)}
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </>
        ) : (
          <EmptyState title="Nothing in this category yet"
            message="Products filed under this category or its sub-categories will appear here." />
        )}
      </div>
    </div>
  );
}
