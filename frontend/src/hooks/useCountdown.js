import { useEffect, useState } from 'react';

/**
 * useCountdown
 * Ticks down to a target timestamp and returns { hours, minutes, seconds, done }.
 * Pure UI concern — no backend dependency. Backend only needs to supply
 * `dealEndsAt` (an ISO timestamp) once the promotions API exists.
 *
 * @param {number} targetTimestamp - ms since epoch
 */
export default function useCountdown(targetTimestamp) {
  const [remaining, setRemaining] = useState(targetTimestamp - Date.now());

  useEffect(() => {
    const id = setInterval(() => {
      setRemaining(targetTimestamp - Date.now());
    }, 1000);
    return () => clearInterval(id);
  }, [targetTimestamp]);

  const done = remaining <= 0;
  const clamped = Math.max(remaining, 0);

  const hours = Math.floor(clamped / (1000 * 60 * 60));
  const minutes = Math.floor((clamped / (1000 * 60)) % 60);
  const seconds = Math.floor((clamped / 1000) % 60);

  const pad = (n) => String(n).padStart(2, '0');

  return {
    hours: pad(hours),
    minutes: pad(minutes),
    seconds: pad(seconds),
    done,
  };
}
