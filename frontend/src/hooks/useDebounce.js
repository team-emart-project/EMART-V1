import { useEffect, useState } from 'react';

/**
 * Delays a value until the user stops changing it.
 * Used on the search box so we do not fire a request per keystroke.
 */
export default function useDebounce(value, delay = 400) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);   // cancel if value changes first
  }, [value, delay]);

  return debounced;
}
