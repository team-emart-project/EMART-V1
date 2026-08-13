import { useFetch } from './useApi';
import endpoints from '@/api/endpoints';
import useAuth from './useAuth';

/**
 * Current user's e-Points balance, for the "Redeem e-Points" checkbox.
 *
 * Built on the same useFetch as everything else — no bespoke fetching logic.
 * `skip` stops it firing at all for a signed-out visitor.
 */
export default function useCardBalance() {
  const { isAuthenticated } = useAuth();

  const { data, loading, refetch } = useFetch(endpoints.card.balance, {
    skip: !isAuthenticated,
  });

  return {
    cardholder: Boolean(data?.cardholder),
    pointsBalance: data?.pointsBalance ?? 0,
    cardStatus: data?.cardStatus ?? 'NONE',
    loading,
    refresh: refetch,
  };
}
