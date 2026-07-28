import { FaCoins } from 'react-icons/fa';
import './LoyaltyPointsButton.css';

/**
 * LoyaltyPointsButton
 * Reusable across Navbar (compact) and Home/Profile (large promo variant).
 *
 * @param {number} [points=0] - current point balance. Will come from
 *   the logged-in user's profile (e.g. user.loyaltyPoints from AuthContext)
 *   once the backend supplies it — for now pass a mock number.
 * @param {'compact'|'large'} [variant='compact']
 */
export default function LoyaltyPointsButton({ points = 0, variant = 'compact' }) {
  return (
    <a
      href="/loyalty-points"
      className={`emart-loyalty-btn emart-loyalty-btn--${variant}`}
      aria-label={`View loyalty points, balance ${points}`}
    >
      <span className="emart-loyalty-btn__icon">
        <FaCoins />
      </span>
      <span className="emart-loyalty-btn__text">
        <span className="emart-loyalty-btn__points font-mono">{points.toLocaleString('en-IN')}</span>
        <span className="emart-loyalty-btn__label">Loyalty Points</span>
      </span>
    </a>
  );
}
