import React from 'react';
import './PaymentStatusBadge.css';

const PaymentStatusBadge = ({ status }) => {
  const getStatusClass = () => {
    switch (status?.toUpperCase()) {
      case 'COMPLETED':
      case 'SUCCESS':
        return 'status-badge status-success';
      case 'FAILED':
        return 'status-badge status-failed';
      case 'PENDING':
      case 'PROCESSING':
        return 'status-badge status-pending';
      default:
        return 'status-badge status-default';
    }
  };

  return (
    <span className={getStatusClass()}>
      {status || 'UNKNOWN'}
    </span>
  );
};

export default PaymentStatusBadge;
