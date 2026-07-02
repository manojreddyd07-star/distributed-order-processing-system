import React from 'react';
import './ProcessingStatus.css';

const ProcessingStatus = ({ status }) => {
  const getStatusClass = () => {
    switch (status?.toUpperCase()) {
      case 'PROCESSED':
        return 'status-processed';
      case 'FAILED':
        return 'status-failed';
      case 'PROCESSING':
        return 'status-processing';
      default:
        return 'status-unknown';
    }
  };

  return (
    <span className={`processing-status ${getStatusClass()}`}>
      {status || 'UNKNOWN'}
    </span>
  );
};

export default ProcessingStatus;
