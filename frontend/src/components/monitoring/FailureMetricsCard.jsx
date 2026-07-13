import React from 'react';
import './FailureMetricsCard.css';

const FailureMetricsCard = ({ failureData }) => {
  if (!failureData) {
    return (
      <div className="failure-metrics-card">
        <h3>Failure Metrics</h3>
        <p className="no-data">No data available</p>
      </div>
    );
  }

  const {
    totalFailures,
    failureRate,
    successRate,
    consecutiveFailures
  } = failureData;

  // Determine health status
  const getHealthStatus = (failRate) => {
    if (failRate === 0) return { status: 'healthy', color: '#28a745', icon: '✅', label: 'Healthy' };
    if (failRate < 5) return { status: 'good', color: '#20c997', icon: '✓', label: 'Good' };
    if (failRate < 15) return { status: 'warning', color: '#ffc107', icon: '⚠️', label: 'Warning' };
    if (failRate < 30) return { status: 'poor', color: '#fd7e14', icon: '⚡', label: 'Poor' };
    return { status: 'critical', color: '#dc3545', icon: '❌', label: 'Critical' };
  };

  const healthStatus = getHealthStatus(failureRate);

  // Check if there's an alert condition
  const hasAlert = consecutiveFailures >= 3 || failureRate > 20;

  return (
    <div className="failure-metrics-card">
      <div className="card-header">
        <h3>🎯 Failure & Success Metrics</h3>
        {hasAlert && (
          <div className="alert-badge pulse">
            🚨 Alert
          </div>
        )}
      </div>

      <div className="health-status-banner" style={{ backgroundColor: healthStatus.color }}>
        <span className="status-icon">{healthStatus.icon}</span>
        <span className="status-label">{healthStatus.label}</span>
        <span className="status-subtitle">System Health</span>
      </div>

      <div className="metrics-grid">
        <div className="metric-box success-box">
          <div className="metric-icon">✅</div>
          <div className="metric-content">
            <div className="metric-label">Success Rate</div>
            <div className="metric-value">{successRate.toFixed(2)}%</div>
            <div className="metric-bar">
              <div 
                className="metric-bar-fill success" 
                style={{ width: `${successRate}%` }}
              />
            </div>
          </div>
        </div>

        <div className="metric-box failure-box">
          <div className="metric-icon">❌</div>
          <div className="metric-content">
            <div className="metric-label">Failure Rate</div>
            <div className="metric-value">{failureRate.toFixed(2)}%</div>
            <div className="metric-bar">
              <div 
                className="metric-bar-fill failure" 
                style={{ width: `${failureRate}%` }}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="detailed-metrics">
        <div className="detail-row">
          <div className="detail-item">
            <span className="detail-icon">📊</span>
            <div className="detail-content">
              <div className="detail-label">Total Failures</div>
              <div className="detail-value">{totalFailures.toLocaleString()}</div>
            </div>
          </div>

          <div className="detail-item">
            <span className="detail-icon">🔄</span>
            <div className="detail-content">
              <div className="detail-label">Consecutive Failures</div>
              <div className="detail-value consecutive">
                {consecutiveFailures}
                {consecutiveFailures >= 3 && (
                  <span className="warning-indicator">⚠️</span>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="rate-comparison">
        <div className="comparison-header">Success vs Failure Distribution</div>
        <div className="pie-chart">
          <svg viewBox="0 0 200 200" className="pie-svg">
            <circle
              cx="100"
              cy="100"
              r="80"
              fill="none"
              stroke="#28a745"
              strokeWidth="40"
              strokeDasharray={`${successRate * 5.03} 503`}
              transform="rotate(-90 100 100)"
              className="pie-segment success-segment"
            />
            <circle
              cx="100"
              cy="100"
              r="80"
              fill="none"
              stroke="#dc3545"
              strokeWidth="40"
              strokeDasharray={`${failureRate * 5.03} 503`}
              strokeDashoffset={`-${successRate * 5.03}`}
              transform="rotate(-90 100 100)"
              className="pie-segment failure-segment"
            />
            <text
              x="100"
              y="95"
              textAnchor="middle"
              fontSize="24"
              fontWeight="700"
              fill="#495057"
            >
              {successRate.toFixed(1)}%
            </text>
            <text
              x="100"
              y="115"
              textAnchor="middle"
              fontSize="12"
              fill="#6c757d"
            >
              Success
            </text>
          </svg>
        </div>
        <div className="legend">
          <div className="legend-item">
            <span className="legend-color success"></span>
            <span className="legend-label">Success ({successRate.toFixed(1)}%)</span>
          </div>
          <div className="legend-item">
            <span className="legend-color failure"></span>
            <span className="legend-label">Failure ({failureRate.toFixed(1)}%)</span>
          </div>
        </div>
      </div>

      {hasAlert && (
        <div className="alert-message">
          <span className="alert-icon">⚠️</span>
          <div className="alert-content">
            <strong>Action Required:</strong> 
            {consecutiveFailures >= 3 && ` ${consecutiveFailures} consecutive failures detected.`}
            {failureRate > 20 && ` High failure rate: ${failureRate.toFixed(1)}%.`}
          </div>
        </div>
      )}
    </div>
  );
};

export default FailureMetricsCard;
