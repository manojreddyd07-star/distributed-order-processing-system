import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './Navigation.css';

const Navigation = () => {
  const location = useLocation();

  const isActive = (path) => {
    return location.pathname === path ? 'active' : '';
  };

  return (
    <nav className="navigation">
      <div className="nav-container">
        <div className="nav-brand">
          <Link to="/dashboard">Order Processing System</Link>
        </div>
        <ul className="nav-links">
          <li>
            <Link to="/dashboard" className={isActive('/dashboard')}>
              Dashboard
            </Link>
          </li>
          <li>
            <Link to="/orders" className={isActive('/orders')}>
              Orders
            </Link>
          </li>
          <li>
            <Link to="/validations" className={isActive('/validations')}>
              Validations
            </Link>
          </li>
          <li>
            <Link to="/payments" className={isActive('/payments')}>
              Payments
            </Link>
          </li>
          <li>
            <Link to="/inventory" className={isActive('/inventory')}>
              Inventory
            </Link>
          </li>
          <li>
            <Link to="/fulfillment" className={isActive('/fulfillment')}>
              Fulfillment
            </Link>
          </li>
          <li>
            <Link to="/idempotency" className={isActive('/idempotency')}>
              Idempotency
            </Link>
          </li>
          <li>
            <Link to="/retry" className={isActive('/retry')}>
              Retry
            </Link>
          </li>
          <li>
            <Link to="/dlq" className={isActive('/dlq')}>
              DLQ
            </Link>
          </li>
          <li>
            <Link to="/replay" className={isActive('/replay')}>
              Replay
            </Link>
          </li>
          <li>
            <Link to="/audit" className={isActive('/audit')}>
              Audit
            </Link>
          </li>
          <li>
            <Link to="/monitoring" className={isActive('/monitoring')}>
              Monitoring
            </Link>
          </li>
        </ul>
      </div>
    </nav>
  );
};

export default Navigation;
