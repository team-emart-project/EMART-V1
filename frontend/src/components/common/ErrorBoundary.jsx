import { Component } from 'react';
import { AlertTriangle } from 'lucide-react';
import Button from '@/components/ui/Button';

/**
 * Catches render-time crashes so one broken component cannot blank the whole
 * app. Must be a class — there is no hook equivalent of componentDidCatch.
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error('Unhandled UI error:', error, info);
  }

  render() {
    if (!this.state.hasError) return this.props.children;

    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center px-6 text-center">
        <div className="mb-4 rounded-2xl bg-rose-50 p-4">
          <AlertTriangle className="h-8 w-8 text-rose-500" />
        </div>
        <h2 className="text-xl font-semibold text-slate-900">Something broke</h2>
        <p className="mt-2 max-w-md text-sm text-slate-500">
          {this.state.error?.message || 'An unexpected error occurred while rendering this page.'}
        </p>
        <Button className="mt-6" onClick={() => window.location.reload()}>
          Reload the page
        </Button>
      </div>
    );
  }
}
