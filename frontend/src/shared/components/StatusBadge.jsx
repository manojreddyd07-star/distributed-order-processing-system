import React from 'react';
import './StatusBadge.css';

/**
 * Generic StatusBadge component for displaying status with color coding
 * @param {string} status - The status to display
 * @param {string} type - The type of status (optional: 'order', 'validation', 'payment', 'inventory', 'fulfillment')
 */
const StatusBadge = ({ status, type = 'default' }) => {
  const getStatusClass = () => {
    const normalizedStatus = status?.toUpperCase() || 'UNKNOWN';

    // Success states
    if (['COMPLETED', 'SUCCESS', 'VALIDATED', 'RESERVED', 'SHIPPED', 'DELIVERED'].includes(normalizedStatus)) {
      return 'status-badge status-success';
    }

    // Failed states
    if (['FAILED', 'VALIDATION_FAILED', 'REJECTED', 'CANCELLED'].includes(normalizedStatus)) {
      return 'status-badge status-failed';
    }

    // Pending/Processing states
    if (['PENDING', 'PROCESSING', 'IN_PROGRESS', 'CREATED', 'PREPARING'].includes(normalizedStatus)) {
      return 'status-badge status-pending';
    }

    // Default
    return 'status-badge status-default';
  };

  return (
    <span className={getStatusClass()}>
      {status || 'UNKNOWN'}
    </span>
  );
};

export default StatusBadge;
