import { FaStar, FaStarHalfAlt, FaRegStar } from 'react-icons/fa';
import './Rating.css';

/**
 * Rating
 * Renders a 5-star visual rating plus optional review count.
 *
 * @param {number} value - rating out of 5, e.g. 4.3
 * @param {number} [count] - number of reviews, e.g. 1284
 * @param {'sm'|'md'} [size='sm']
 */
export default function Rating({ value = 0, count, size = 'sm' }) {
  const full = Math.floor(value);
  const hasHalf = value - full >= 0.5;
  const empty = 5 - full - (hasHalf ? 1 : 0);

  return (
    <div className={`emart-rating emart-rating--${size}`} aria-label={`Rated ${value} out of 5`}>
      <span className="emart-rating__badge">
        {value.toFixed(1)}
        <span className="emart-rating__star-icon">
          <FaStar />
        </span>
      </span>
      {typeof count === 'number' && (
        <span className="emart-rating__count">({count.toLocaleString('en-IN')})</span>
      )}
    </div>
  );
}
