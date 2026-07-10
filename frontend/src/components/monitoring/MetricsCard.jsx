import React from 'react';
import './MetricsCard.css';

const MetricsCard = ({ title, value, unit, status, icon, details }) => {
  const getStatusClass = () => {
    if (status === 'UP' || status === 'healthy') return 'status-up';
    if (status === 'DOWN' || status === 'error') return 'status-down';
    if (status === 'WARNING' || status === 'warning') return 'status-warning';
    return 'status-unknown';
  };

  return (
    <div className={`metrics-card ${getStatusClass()}`}>
      <div className="metrics-card-header">
        {icon && <span className="metrics-icon">{icon}</span>}
        <h3 className="metrics-title">{title}</h3>
      </div>
      <div className="metrics-card-body">
        <div className="metrics-value">
          {value !== null && value !== undefined ? value : 'N/A'}
          {unit && <span className="metrics-unit"> {unit}</span>}
        </div>
        {status && (
          <div className={`metrics-status ${getStatusClass()}`}>
            {status}
          </div>
        )}
        {details && (
          <div className="metrics-details">
            {Object.entries(details).map(([key, val]) => (
              <div key={key} className="metrics-detail-item">
                <span className="detail-key">{key}:</span>
                <span className="detail-value">{String(val)}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default MetricsCard;
