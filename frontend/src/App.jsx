import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import store from '@/store';
import AppRoutes from '@/routes/AppRoutes';
import ToastContainer from '@/components/ui/Toast';
import ErrorBoundary from '@/components/common/ErrorBoundary';

export default function App() {
  return (
    <Provider store={store}>
      <BrowserRouter>
        <ErrorBoundary>
          <AppRoutes />
          <ToastContainer />
        </ErrorBoundary>
      </BrowserRouter>
    </Provider>
  );
}
