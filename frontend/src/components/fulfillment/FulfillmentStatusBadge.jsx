import React from 'react';
import './FulfillmentStatusBadge.css';

const FulfillmentStatusBadge = ({ status }) => {
  const getStatusClass = () => {
    switch (status?.toUpperCase()) {
      case 'COMPLETED':
      case 'DELIVERED':
        return 'status-badge status-completed';
      case 'SHIPPED':
        return 'status-badge status-shipped';
      case 'IN_PROGRESS':
      case 'PROCESSING':
        return 'status-badge status-in-progress';
      case 'PENDING':
        return 'status-badge status-pending';
      case 'CANCELLED':
      case 'FAILED':
        return 'status-badge status-failed';
      default:
        return 'status-badge status-default';
    }
  };

  const formatStatus = () => {
    return status?.replace(/_/g, ' ') || 'UNKNOWN';
  };

  return (
    <span className={getStatusClass()}>
      {formatStatus()}
    </span>
  );
};

export default FulfillmentStatusBadge;
