import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Navigation from './components/Navigation/Navigation';
import DashboardPage from './pages/Dashboard/DashboardPage';
import OrdersPage from './pages/Orders/OrdersPage';
import ValidationPage from './pages/Validation/ValidationPage';
import PaymentPage from './pages/Payment/PaymentPage';
import InventoryPage from './pages/Inventory/InventoryPage';
import FulfillmentPage from './pages/Fulfillment/FulfillmentPage';
import IdempotencyPage from './pages/Idempotency/IdempotencyPage';
import RetryPage from './pages/Retry/RetryPage';
import DLQPage from './pages/DLQ/DLQPage';
import ReplayPage from './pages/Replay/ReplayPage';
import AuditPage from './pages/Audit/AuditPage';
import MonitoringPage from './pages/Monitoring/MonitoringPage';
import './App.css';

function App() {
  return (
    <Router>
      <div className="app">
        <Navigation />
        <main className="main-content">
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
        </main>
      </div>
    </Router>
  );
}

export default App;
