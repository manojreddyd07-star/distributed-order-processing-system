import React from 'react';
import './ValidationStatusBadge.css';

const ValidationStatusBadge = ({ status }) => {
  const getStatusClass = () => {
    switch (status?.toUpperCase()) {
      case 'VALIDATED':
      case 'VALID':
      case 'SUCCESS':
        return 'status-badge status-success';
      case 'FAILED':
      case 'INVALID':
      case 'VALIDATION_FAILED':
        return 'status-badge status-failed';
      case 'PENDING':
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

export default ValidationStatusBadge;
