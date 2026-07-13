import React from 'react';
import './LatencyChart.css';

const LatencyChart = ({ latencyData }) => {
  if (!latencyData) {
    return (
      <div className="latency-chart">
        <h3>Latency Metrics</h3>
        <p className="no-data">No data available</p>
      </div>
    );
  }

  const {
    averageProcessingTimeMs,
    averageProcessingTimeSeconds,
    minProcessingTimeMs,
    maxProcessingTimeMs
  } = latencyData;

  // Calculate relative heights for visualization
  const maxValue = Math.max(minProcessingTimeMs, averageProcessingTimeMs, maxProcessingTimeMs, 1);
  const minHeight = (minProcessingTimeMs / maxValue) * 100;
  const avgHeight = (averageProcessingTimeMs / maxValue) * 100;
  const maxHeight = (maxProcessingTimeMs / maxValue) * 100;

  // Determine latency status
  const getLatencyStatus = (avgMs) => {
    if (avgMs < 100) return { status: 'excellent', color: '#28a745', label: 'Excellent' };
    if (avgMs < 300) return { status: 'good', color: '#20c997', label: 'Good' };
    if (avgMs < 500) return { status: 'fair', color: '#ffc107', label: 'Fair' };
    if (avgMs < 1000) return { status: 'slow', color: '#fd7e14', label: 'Slow' };
    return { status: 'critical', color: '#dc3545', label: 'Critical' };
  };

  const latencyStatus = getLatencyStatus(averageProcessingTimeMs);

  return (
    <div className="latency-chart">
      <h3>⚡ Latency Metrics</h3>

      <div className="latency-status" style={{ borderLeftColor: latencyStatus.color }}>
        <div className="status-indicator" style={{ backgroundColor: latencyStatus.color }}>
          {latencyStatus.label}
        </div>
        <div className="status-text">System Performance</div>
      </div>

      <div className="latency-summary">
        <div className="latency-card primary">
          <div className="card-icon">📈</div>
          <div className="card-content">
            <div className="card-label">Average Latency</div>
            <div className="card-value">{averageProcessingTimeMs.toFixed(2)} ms</div>
            <div className="card-subtitle">{averageProcessingTimeSeconds.toFixed(3)} seconds</div>
          </div>
        </div>

        <div className="latency-card">
          <div className="card-icon">⚡</div>
          <div className="card-content">
            <div className="card-label">Min Latency</div>
            <div className="card-value">{minProcessingTimeMs} ms</div>
            <div className="card-subtitle">Fastest response</div>
          </div>
        </div>

        <div className="latency-card">
          <div className="card-icon">🐌</div>
          <div className="card-content">
            <div className="card-label">Max Latency</div>
            <div className="card-value">{maxProcessingTimeMs} ms</div>
            <div className="card-subtitle">Slowest response</div>
          </div>
        </div>
      </div>

      <div className="latency-visualization">
        <div className="visualization-header">Processing Time Distribution</div>
        <div className="bar-chart">
          <div className="bar-container">
            <div className="bar-label-top">{minProcessingTimeMs} ms</div>
            <div className="bar-wrapper">
              <div 
                className="latency-bar min-bar" 
                style={{ 
                  height: `${minHeight}%`,
                  backgroundColor: '#28a745'
                }}
              />
            </div>
            <div className="bar-label">Minimum</div>
          </div>

          <div className="bar-container">
            <div className="bar-label-top">{averageProcessingTimeMs.toFixed(1)} ms</div>
            <div className="bar-wrapper">
              <div 
                className="latency-bar avg-bar" 
                style={{ 
                  height: `${avgHeight}%`,
                  backgroundColor: latencyStatus.color
                }}
              />
            </div>
            <div className="bar-label">Average</div>
          </div>

          <div className="bar-container">
            <div className="bar-label-top">{maxProcessingTimeMs} ms</div>
            <div className="bar-wrapper">
              <div 
                className="latency-bar max-bar" 
                style={{ 
                  height: `${maxHeight}%`,
                  backgroundColor: '#dc3545'
                }}
              />
            </div>
            <div className="bar-label">Maximum</div>
          </div>
        </div>
      </div>

      <div className="latency-info">
        <div className="info-row">
          <span className="info-label">Latency Range:</span>
          <span className="info-value">{minProcessingTimeMs} - {maxProcessingTimeMs} ms</span>
        </div>
        <div className="info-row">
          <span className="info-label">Variance:</span>
          <span className="info-value">{(maxProcessingTimeMs - minProcessingTimeMs)} ms</span>
        </div>
      </div>
    </div>
  );
};

export default LatencyChart;
