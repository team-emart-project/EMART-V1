import './Loader.css';

/**
 * Loader
 * variant="spinner" -> centered spinner for full-page / section loads
 * variant="product-grid" -> skeleton cards matching ProductCard's shape,
 *   so content doesn't "pop" or reflow once real data arrives.
 *
 * @param {'spinner'|'product-grid'} [variant='spinner']
 * @param {number} [count=6] - number of skeleton cards for product-grid
 * @param {string} [label='Loading...'] - accessible label for spinner
 */
export default function Loader({ variant = 'spinner', count = 6, label = 'Loading…' }) {
  if (variant === 'product-grid') {
    return (
      <div className="row g-4" role="status" aria-label={label}>
        {Array.from({ length: count }).map((_, i) => (
          <div className="col-6 col-md-4 col-lg-2" key={i}>
            <div className="emart-skeleton-card">
              <div className="skeleton emart-skeleton-card__image" />
              <div className="skeleton emart-skeleton-card__line" style={{ width: '90%' }} />
              <div className="skeleton emart-skeleton-card__line" style={{ width: '60%' }} />
              <div className="skeleton emart-skeleton-card__line" style={{ width: '40%' }} />
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="emart-loader" role="status" aria-label={label}>
      <div className="emart-loader__spinner" />
      <span className="emart-loader__label">{label}</span>
    </div>
  );
}
