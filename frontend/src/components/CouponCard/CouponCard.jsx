import { useState } from 'react';
import { FaRegCopy, FaCheck } from 'react-icons/fa';
import './CouponCard.css';

/**
 * CouponCard
 * @param {object} coupon - { id, code, description, discountLabel, expiresOn }
 */
export default function CouponCard({ coupon }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(coupon.code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard API can fail (e.g. insecure context) — fail silently,
      // the code is still visible for the user to copy manually.
    }
  };

  return (
    <div className="emart-coupon-card">
      <div className="emart-coupon-card__left">
        <p className="emart-coupon-card__discount">{coupon.discountLabel}</p>
        <p className="emart-coupon-card__desc">{coupon.description}</p>
        <p className="emart-coupon-card__expiry">Valid till {coupon.expiresOn}</p>
      </div>
      <div className="emart-coupon-card__notch" aria-hidden="true" />
      <div className="emart-coupon-card__right">
        <span className="emart-coupon-card__code font-mono">{coupon.code}</span>
        <button type="button" className="emart-coupon-card__copy-btn" onClick={handleCopy}>
          {copied ? <FaCheck /> : <FaRegCopy />}
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
    </div>
  );
}
