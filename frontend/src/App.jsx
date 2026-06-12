import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Navigation from './components/Navigation/Navigation';
import DashboardPage from './pages/Dashboard/DashboardPage';
import OrdersPage from './pages/Orders/OrdersPage';
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
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
