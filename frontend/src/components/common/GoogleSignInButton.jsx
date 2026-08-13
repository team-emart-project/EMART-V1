import { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate, useLocation } from 'react-router-dom';
import { googleLogin } from '@/store/slices/authSlice';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';

/**
 * "Sign in with Google" button, rendered by Google Identity Services (GIS).
 *
 * HOW THIS WORKS
 * --------------
 * 1. We load Google's script and hand it our CLIENT ID.
 * 2. Google renders its own button into an empty <div> we provide.
 * 3. The user clicks it and completes the Google popup.
 * 4. Google calls our callback with a CREDENTIAL — a signed ID token (a JWT).
 * 5. We post that token to /api/auth/google, which verifies it and returns our
 *    own JWT. From then on the session is identical to a password login.
 *
 * WHY GOOGLE RENDERS THE BUTTON INSTEAD OF US STYLING OUR OWN
 * -----------------------------------------------------------
 * Google's branding guidelines require their exact button, and more usefully
 * their button carries the "One Tap" / account-chooser behaviour. A custom
 * <button> would need us to drive the flow manually and would break whenever
 * Google changed it.
 *
 * WHY THERE IS NO CLIENT SECRET HERE
 * ----------------------------------
 * There is nothing to keep secret in a browser — anything shipped to the client
 * is readable by the user. The client ID is public by design; the client secret
 * is never used in this flow at all, on either side.
 */
export default function GoogleSignInButton({ text = 'signin_with' }) {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();

  const containerRef = useRef(null);
  const [status, setStatus] = useState('loading');   // loading | ready | unavailable

  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  useEffect(() => {
    if (!clientId) {
      // Fail visibly rather than rendering a button that silently does nothing.
      setStatus('unavailable');
      return;
    }

    let cancelled = false;

    /** Where to send the user after a successful sign-in. */
    const redirectTarget = () => location.state?.from?.pathname || '/';

    const handleCredential = async (response) => {
      if (!response?.credential) {
        dispatch(toastError('Google did not return a sign-in token. Please try again.'));
        return;
      }

      const result = await dispatch(googleLogin(response.credential));

      if (googleLogin.fulfilled.match(result)) {
        dispatch(toastSuccess(`Welcome, ${result.payload.user.firstName}`));
        navigate(redirectTarget(), { replace: true });
      } else {
        // The backend explains exactly why (unverified email, deactivated
        // account, and so on), so surface its message rather than a generic one.
        dispatch(toastError(result.payload || 'Google sign-in failed. Please try again.'));
      }
    };

    const initialise = () => {
      if (cancelled || !window.google?.accounts?.id || !containerRef.current) return;

      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: handleCredential,
      });

      window.google.accounts.id.renderButton(containerRef.current, {
        theme: 'outline',
        size: 'large',
        width: 320,
        text,
        shape: 'rectangular',
        logo_alignment: 'left',
      });

      setStatus('ready');
    };

    // The script may already be present if another page mounted this component
    // first — loading it twice would re-register the callback.
    const existing = document.getElementById('google-identity-services');
    if (existing) {
      if (window.google?.accounts?.id) initialise();
      else existing.addEventListener('load', initialise, { once: true });
      return () => { cancelled = true; };
    }

    const script = document.createElement('script');
    script.id = 'google-identity-services';
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = initialise;
    script.onerror = () => {
      if (!cancelled) setStatus('unavailable');
    };
    document.head.appendChild(script);

    return () => { cancelled = true; };
    // location.state is read inside the closure; re-running on every change
    // would re-render Google's button unnecessarily, so it is left out.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientId, dispatch, navigate, text]);

  if (status === 'unavailable') {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-3 text-center text-xs text-slate-500">
        {clientId
          ? 'Google sign-in is unavailable right now. Please use your email and password.'
          : 'Google sign-in is not configured. Set VITE_GOOGLE_CLIENT_ID in frontend/.env'}
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center">
      {/* Google injects its button in here. Reserved height stops the form
          jumping when the script finishes loading. */}
      <div ref={containerRef} className="min-h-[44px]" />
      {status === 'loading' && (
        <div className="h-[44px] w-[320px] animate-pulse rounded-lg bg-slate-100" />
      )}
    </div>
  );
}
