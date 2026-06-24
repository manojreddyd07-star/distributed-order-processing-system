import React from 'react';
import './InventoryStatusBadge.css';

const InventoryStatusBadge = ({ status }) => {
  const getStatusClass = () => {
    switch (status?.toUpperCase()) {
      case 'IN_STOCK':
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

  const formatStatus = (status) => {
    if (!status) return 'UNKNOWN';
    return status.replace(/_/g, ' ');
  };

  return (
    <span className={getStatusClass()}>
      {formatStatus(status)}
    </span>
  );
};

export default InventoryStatusBadge;
