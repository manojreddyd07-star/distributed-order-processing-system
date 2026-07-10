import React from 'react';
import './HealthGrid.css';

const HealthGrid = ({ services, databaseHealth, kafkaHealth }) => {
  const getStatusColor = (status) => {
    if (status === 'UP') return '#4caf50';
    if (status === 'DOWN') return '#f44336';
    return '#9e9e9e';
  };

  const getStatusIcon = (status) => {
    if (status === 'UP') return '✓';
    if (status === 'DOWN') return '✗';
    return '?';
  };

  return (
    <div className="health-grid">
      <h3 className="health-grid-title">Service Health Status</h3>
      
      {/* Infrastructure Health */}
      <div className="health-section">
        <h4 className="section-title">Infrastructure</h4>
        <div className="health-items">
          {databaseHealth && (
            <div className="health-item">
              <div className="health-item-header">
                <span 
                  className="health-status-indicator" 
                  style={{ backgroundColor: getStatusColor(databaseHealth.status) }}
                >
                  {getStatusIcon(databaseHealth.status)}
                </span>
                <span className="health-item-name">{databaseHealth.serviceName}</span>
              </div>
              <div className="health-item-status">{databaseHealth.status}</div>
              <div className="health-item-description">{databaseHealth.description}</div>
            </div>
          )}
          
          {kafkaHealth && (
            <div className="health-item">
              <div className="health-item-header">
                <span 
                  className="health-status-indicator" 
                  style={{ backgroundColor: getStatusColor(kafkaHealth.status) }}
                >
                  {getStatusIcon(kafkaHealth.status)}
                </span>
                <span className="health-item-name">{kafkaHealth.serviceName}</span>
              </div>
              <div className="health-item-status">{kafkaHealth.status}</div>
              <div className="health-item-description">{kafkaHealth.description}</div>
            </div>
          )}
        </div>
      </div>

      {/* Microservices Health */}
      <div className="health-section">
        <h4 className="section-title">Microservices</h4>
        <div className="health-items">
          {services && services.map((service) => (
            <div key={service.serviceName} className="health-item">
              <div className="health-item-header">
                <span 
                  className="health-status-indicator" 
                  style={{ backgroundColor: getStatusColor(service.status) }}
                >
                  {getStatusIcon(service.status)}
                </span>
                <span className="health-item-name">{service.serviceName}</span>
              </div>
              <div className="health-item-status">{service.status}</div>
              <div className="health-item-description">{service.description}</div>
              {service.details && service.details.error && (
                <div className="health-item-error">
                  Error: {service.details.error}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default HealthGrid;
