import { formatPlain, formatPrice } from '@/utils/formatters';

/**
 * Describes the four price options for a product in one place.
 *
 * Returned as data rather than JSX so the cart, the product card and the
 * product page can each render the same options in their own layout without
 * three copies of the labelling rules drifting apart.
 *
 * An option is only included if the API actually sent its price. A missing
 * field means either "this product has no such offer" or "you are not a
 * cardholder, so you may not see it" — both should hide the row, so one check
 * covers both.
 */
export function buildPriceOptions(product) {
  const options = [];

  if (product.cardholderPrice != null) {
    options.push({
      value: 'MEMBER',
      label: `eMcard price ${formatPrice(product.cardholderPrice)}`,
      short: 'eMcard price',
      cash: Number(product.cardholderPrice),
      points: 0,
    });
  }

  if (product.pointsPrice != null) {
    options.push({
      value: 'POINTS',
      label: `Pay with e-Points only — ${formatPlain(product.pointsPrice)} e-Points`,
      short: 'Paid with e-Points',
      cash: 0,
      points: Number(product.pointsPrice),
    });
  }

  if (product.hybridCashPrice != null && product.hybridPoints != null) {
    options.push({
      value: 'HYBRID',
      label: `${formatPrice(product.hybridCashPrice)} + ${formatPlain(product.hybridPoints)} e-Points`,
      short: 'Cash + e-Points',
      cash: Number(product.hybridCashPrice),
      points: Number(product.hybridPoints),
    });
  }

  return options;
}

/**
 * The price block on a product: the normal price, then a checkbox per e-MART
 * card option.
 *
 * WHY CHECKBOXES THAT ACT LIKE RADIOS
 * -----------------------------------
 * The spec asks for checkboxes, but only one option can apply to a purchase.
 * So they render as checkboxes and behave as a radio group: ticking one clears
 * the others, and ticking the ticked one clears back to the normal price.
 * Native radios cannot be un-picked, which would trap someone who changed
 * their mind.
 *
 * The handler sits on the <input>'s onChange, NOT on a click handler on the
 * surrounding <label>. A label that both wraps an input and carries its own
 * click handler receives the event twice — once from the input, once from the
 * label's own forwarding — so a toggle fires, un-fires, and the tick appears
 * to do nothing at all.
 */
export default function PriceOptions({ product, value, onChange, compact = false }) {
  const options = buildPriceOptions(product);

  const toggle = (option) => onChange(value === option ? 'REGULAR' : option);

  return (
    <div className={compact ? 'space-y-1.5' : 'space-y-2'}>
      {/* Line 1 — the normal price. Never selectable: it is the fallback that
          applies whenever nothing below is ticked. */}
      <div className="flex items-baseline gap-2">
        <span className={`font-bold text-slate-900 ${compact ? 'text-lg' : 'text-2xl'}`}>
          {formatPrice(product.mrpPrice)}
        </span>
        {options.length > 0 && value === 'REGULAR' && (
          <span className="text-[11px] text-slate-400">regular price</span>
        )}
      </div>

      {options.length > 0 && (
        <div role="radiogroup" aria-label="e-MART card price options"
             className={compact ? 'space-y-1' : 'space-y-1.5'}>
          {options.map((option) => {
            const checked = value === option.value;
            return (
              <label
                key={option.value}
                className={`flex cursor-pointer items-center gap-2 rounded-md px-1.5 py-1 transition-colors
                  ${compact ? 'text-xs' : 'text-sm'}
                  ${checked
                    ? 'bg-brand-50 font-semibold text-brand-800'
                    : 'text-slate-600 hover:bg-slate-50'}`}
              >
                <input
                  type="checkbox"
                  role="radio"
                  aria-checked={checked}
                  checked={checked}
                  onChange={() => toggle(option.value)}
                  // The card around this is a link; without this a tick would
                  // navigate to the product page instead of selecting.
                  onClick={(e) => e.stopPropagation()}
                  className="h-4 w-4 shrink-0 accent-brand-600"
                />
                <span>{option.label}</span>
              </label>
            );
          })}
        </div>
      )}
    </div>
  );
}
