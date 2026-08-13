import { useEffect, useMemo, useState } from 'react';
import { Filter, Search, X } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { Button, EmptyState, Input, Pagination, ProductCardSkeleton } from '@/components/ui';
import ProductCard from '@/components/common/ProductCard';
import CategoryTree from '@/components/common/CategoryTree';
import { useFetch } from '@/hooks/useApi';
import useDebounce from '@/hooks/useDebounce';
import endpoints from '@/api/endpoints';

/** Module 5 — product search with filters and pagination. */
export default function ProductListPage() {
  const [params, setParams] = useSearchParams();

  const [search, setSearch] = useState(params.get('search') || '');
  const [minPrice, setMinPrice] = useState(params.get('minPrice') || '');
  const [maxPrice, setMaxPrice] = useState(params.get('maxPrice') || '');
  const [page, setPage] = useState(Number(params.get('page') || 0));
  const [filtersOpen, setFiltersOpen] = useState(false);

  // Debounced so typing does not fire a request per keystroke.
  const debouncedSearch = useDebounce(search, 450);

  // Building the URL in a memo means useFetch re-runs exactly when a filter
  // actually changes, and never on an unrelated re-render.
  const url = useMemo(() => {
    const qs = new URLSearchParams();
    if (debouncedSearch) qs.set('search', debouncedSearch);
    if (minPrice) qs.set('minPrice', minPrice);
    if (maxPrice) qs.set('maxPrice', maxPrice);
    qs.set('page', page);
    qs.set('size', 12);
    return endpoints.products.search(qs.toString());
  }, [debouncedSearch, minPrice, maxPrice, page]);

  const { data, loading, error } = useFetch(url);
  const { data: categories } = useFetch(endpoints.categories.tree);

  // Keep the address bar in sync so the search is shareable/bookmarkable.
  useEffect(() => {
    const next = {};
    if (debouncedSearch) next.search = debouncedSearch;
    if (minPrice) next.minPrice = minPrice;
    if (maxPrice) next.maxPrice = maxPrice;
    if (page) next.page = page;
    setParams(next, { replace: true });
  }, [debouncedSearch, minPrice, maxPrice, page, setParams]);

  // A new filter should always land the user on page 1.
  useEffect(() => { setPage(0); }, [debouncedSearch, minPrice, maxPrice]);

  const clearFilters = () => { setSearch(''); setMinPrice(''); setMaxPrice(''); };
  const hasFilters = search || minPrice || maxPrice;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900">Shop</h1>
        <p className="mt-1 text-sm text-slate-500">
          {data ? `${data.totalElements} product${data.totalElements === 1 ? '' : 's'}` : 'Loading…'}
        </p>
      </div>

      <div className="grid gap-8 lg:grid-cols-[260px_1fr]">
        {/* ---- sidebar ---- */}
        <aside className={`${filtersOpen ? 'block' : 'hidden'} lg:block`}>
          <div className="sticky top-24 space-y-6">
            <div className="rounded-2xl border border-slate-200 bg-white p-4">
              <h3 className="mb-3 text-sm font-semibold text-slate-900">Search</h3>
              <Input icon={Search} value={search} placeholder="Product name…"
                onChange={(e) => setSearch(e.target.value)} />

              <h3 className="mb-2 mt-5 text-sm font-semibold text-slate-900">Price range</h3>
              <div className="flex items-center gap-2">
                <Input type="number" min="0" placeholder="Min" value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)} />
                <span className="text-slate-400">–</span>
                <Input type="number" min="0" placeholder="Max" value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)} />
              </div>

              {hasFilters && (
                <Button variant="ghost" size="sm" fullWidth className="mt-3" onClick={clearFilters}>
                  <X className="h-3.5 w-3.5" /> Clear filters
                </Button>
              )}

              {/* product_master has no brand column, so no brand filter exists. */}
            </div>

            {categories?.length > 0 && (
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <h3 className="mb-3 text-sm font-semibold text-slate-900">Categories</h3>
                <CategoryTree categories={categories} />
              </div>
            )}
          </div>
        </aside>

        {/* ---- results ---- */}
        <div>
          <Button variant="outline" size="sm" className="mb-4 lg:hidden"
            onClick={() => setFiltersOpen((v) => !v)}>
            <Filter className="h-3.5 w-3.5" /> {filtersOpen ? 'Hide' : 'Show'} filters
          </Button>

          {loading ? (
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
              {Array.from({ length: 9 }).map((_, i) => <ProductCardSkeleton key={i} />)}
            </div>
          ) : error ? (
            <EmptyState title="Could not load products" message={error.message} />
          ) : data?.content?.length ? (
            <>
              <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
                {data.content.map((p, i) => <ProductCard key={p.prodId} product={p} index={i} />)}
              </div>
              <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
            </>
          ) : (
            <EmptyState title="No products match" message="Try widening your filters."
              action={hasFilters && <Button onClick={clearFilters}>Clear filters</Button>} />
          )}
        </div>
      </div>
    </div>
  );
}
