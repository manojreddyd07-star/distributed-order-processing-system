import React from 'react';
import './RetryMetricsCard.css';

const RetryMetricsCard = ({ retryRecords }) => {
  const totalRetries = retryRecords.length;
  const pendingRetries = retryRecords.filter(r => r.retryStatus === 'PENDING').length;
  const successfulRetries = retryRecords.filter(r => r.retryStatus === 'SUCCESS').length;
  const exhaustedRetries = retryRecords.filter(r => r.retryStatus === 'EXHAUSTED').length;
  
  // Calculate success rate
  const completedRetries = successfulRetries + exhaustedRetries;
  const successRate = completedRetries > 0 
    ? ((successfulRetries / completedRetries) * 100).toFixed(1) 
    : 0;

  return (
    <div className="retry-metrics-container">
      <div className="retry-metric-card">
        <div className="metric-icon total">📊</div>
        <div className="metric-content">
          <div className="metric-value">{totalRetries}</div>
          <div className="metric-label">Total Retries</div>
        </div>
      </div>

      <div className="retry-metric-card">
        <div className="metric-icon pending">⏳</div>
        <div className="metric-content">
          <div className="metric-value">{pendingRetries}</div>
          <div className="metric-label">Pending</div>
        </div>
      </div>

      <div className="retry-metric-card">
        <div className="metric-icon success">✅</div>
        <div className="metric-content">
          <div className="metric-value">{successfulRetries}</div>
          <div className="metric-label">Successful</div>
        </div>
      </div>

      <div className="retry-metric-card">
        <div className="metric-icon exhausted">❌</div>
        <div className="metric-content">
          <div className="metric-value">{exhaustedRetries}</div>
          <div className="metric-label">Exhausted</div>
        </div>
      </div>

      <div className="retry-metric-card">
        <div className="metric-icon rate">📈</div>
        <div className="metric-content">
          <div className="metric-value">{successRate}%</div>
          <div className="metric-label">Success Rate</div>
        </div>
      </div>
    </div>
  );
};

export default RetryMetricsCard;
