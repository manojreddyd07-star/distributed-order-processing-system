import React, { lazy, Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Navigation from './components/Navigation/Navigation';
import './App.css';

// Lazy load pages for better initial load performance
const DashboardPage = lazy(() => import('./pages/Dashboard/DashboardPage'));
const OrdersPage = lazy(() => import('./pages/Orders/OrdersPage'));
const ValidationPage = lazy(() => import('./pages/Validation/ValidationPage'));
const PaymentPage = lazy(() => import('./pages/Payment/PaymentPage'));
const InventoryPage = lazy(() => import('./pages/Inventory/InventoryPage'));
const FulfillmentPage = lazy(() => import('./pages/Fulfillment/FulfillmentPage'));
const IdempotencyPage = lazy(() => import('./pages/Idempotency/IdempotencyPage'));
const RetryPage = lazy(() => import('./pages/Retry/RetryPage'));
const DLQPage = lazy(() => import('./pages/DLQ/DLQPage'));
const ReplayPage = lazy(() => import('./pages/Replay/ReplayPage'));
const AuditPage = lazy(() => import('./pages/Audit/AuditPage'));
const MonitoringPage = lazy(() => import('./pages/Monitoring/MonitoringPage'));

// Loading fallback component
const LoadingFallback = () => (
  <div className="loading-container" style={{ 
    display: 'flex', 
    justifyContent: 'center', 
    alignItems: 'center', 
    height: '100vh' 
  }}>
    <div>Loading...</div>
  </div>
);

function App() {
  return (
    <Router>
      <div className="app">
        <Navigation />
        <main className="main-content">
          <Suspense fallback={<LoadingFallback />}>
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/orders" element={<OrdersPage />} />
              <Route path="/validations" element={<ValidationPage />} />
              <Route path="/payments" element={<PaymentPage />} />
              <Route path="/inventory" element={<InventoryPage />} />
              <Route path="/fulfillment" element={<FulfillmentPage />} />
              <Route path="/idempotency" element={<IdempotencyPage />} />
              <Route path="/retry" element={<RetryPage />} />
              <Route path="/dlq" element={<DLQPage />} />
              <Route path="/replay" element={<ReplayPage />} />
              <Route path="/audit" element={<AuditPage />} />
              <Route path="/monitoring" element={<MonitoringPage />} />
            </Routes>
          </Suspense>
        </main>
      </div>
    </Router>
  );
}

export default App;
