import React from 'react';
import './InventoryStatusBadge.css';

const InventoryStatusBadge = ({ status }) => {
  const getStatusClass = () => {
    switch (status?.toUpperCase()) {
      case 'AVAILABLE':
        return 'inventory-badge status-available';
      case 'LOW_STOCK':
        return 'inventory-badge status-low-stock';
      case 'OUT_OF_STOCK':
        return 'inventory-badge status-out-of-stock';
      case 'RESERVED':
        return 'inventory-badge status-reserved';
      default:
        return 'inventory-badge status-default';
    }
  };

  return (
    <span className={getStatusClass()}>
      {status || 'UNKNOWN'}
    </span>
  );
};

export default InventoryStatusBadge;
