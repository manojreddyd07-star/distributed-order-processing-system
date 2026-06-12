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
        </ul>
      </div>
    </nav>
  );
};

export default Navigation;
