import React from 'react';
import './ServiceStatusCard.css';

const ServiceStatusCard = ({ service }) => {
  const { name, status, port, version, uptime, metrics } = service;

  const getStatusClass = (status) => {
    switch (status.toLowerCase()) {
      case 'healthy':
        return 'healthy';
      case 'degraded':
        return 'degraded';
      case 'down':
        return 'down';
      default:
        return 'unknown';
    }
  };

  return (
    <div className="service-status-card">
      <div className="service-header">
        <h3 className="service-name">{name}</h3>
        <span className={`status-badge ${getStatusClass(status)}`}>
          {status}
        </span>
      </div>

      <div className="service-info">
        {port && (
          <div className="info-row">
            <span className="info-label">Port:</span>
            <span className="info-value">{port}</span>
          </div>
        )}
        {version && (
          <div className="info-row">
            <span className="info-label">Version:</span>
            <span className="info-value">{version}</span>
          </div>
        )}
        {uptime && (
          <div className="info-row">
            <span className="info-label">Uptime:</span>
            <span className="info-value">{uptime}</span>
          </div>
        )}
      </div>

      {metrics && Object.keys(metrics).length > 0 && (
        <div className="service-metrics">
          <div className="metrics-grid">
            {Object.entries(metrics).map(([key, value]) => (
              <div key={key} className="metric-item">
                <span className="metric-label">{key}</span>
                <span className="metric-value">{value}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ServiceStatusCard;
