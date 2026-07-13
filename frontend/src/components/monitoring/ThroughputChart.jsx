import React from 'react';
import './ThroughputChart.css';

const ThroughputChart = ({ throughputData }) => {
  if (!throughputData) {
    return (
      <div className="throughput-chart">
        <h3>Throughput Metrics</h3>
        <p className="no-data">No data available</p>
      </div>
    );
  }

  const {
    totalEvents,
    eventsPerSecond,
    eventsPerMinute,
    successfulEvents,
    failedEvents
  } = throughputData;

  // Calculate success percentage for visual bar
  const successPercentage = totalEvents > 0 
    ? (successfulEvents / totalEvents) * 100 
    : 0;

  return (
    <div className="throughput-chart">
      <h3>📊 Throughput Metrics</h3>
      
      <div className="throughput-summary">
        <div className="metric-item primary">
          <div className="metric-label">Events/Second</div>
          <div className="metric-value">{eventsPerSecond.toFixed(2)}</div>
        </div>
        
        <div className="metric-item primary">
          <div className="metric-label">Events/Minute</div>
          <div className="metric-value">{eventsPerMinute.toFixed(2)}</div>
        </div>
        
        <div className="metric-item">
          <div className="metric-label">Total Events</div>
          <div className="metric-value">{totalEvents.toLocaleString()}</div>
        </div>
      </div>

      <div className="event-breakdown">
        <div className="breakdown-header">Event Distribution</div>
        
        <div className="progress-bar-container">
          <div 
            className="progress-bar success" 
            style={{ width: `${successPercentage}%` }}
          >
            {successPercentage > 10 && `${successPercentage.toFixed(1)}%`}
          </div>
          <div 
            className="progress-bar failure" 
            style={{ width: `${100 - successPercentage}%` }}
          >
            {(100 - successPercentage) > 10 && `${(100 - successPercentage).toFixed(1)}%`}
          </div>
        </div>

        <div className="breakdown-stats">
          <div className="stat-item success">
            <span className="stat-label">✓ Successful</span>
            <span className="stat-value">{successfulEvents.toLocaleString()}</span>
          </div>
          <div className="stat-item failure">
            <span className="stat-label">✗ Failed</span>
            <span className="stat-value">{failedEvents.toLocaleString()}</span>
          </div>
        </div>
      </div>

      <div className="chart-visualization">
        <div className="bar-chart">
          <div className="bar-item">
            <div className="bar-label">Success</div>
            <div className="bar-wrapper">
              <div 
                className="bar success-bar" 
                style={{ height: `${Math.min((successfulEvents / (totalEvents || 1)) * 100, 100)}%` }}
              >
                <span className="bar-value">{successfulEvents}</span>
              </div>
            </div>
          </div>
          
          <div className="bar-item">
            <div className="bar-label">Failed</div>
            <div className="bar-wrapper">
              <div 
                className="bar failure-bar" 
                style={{ height: `${Math.min((failedEvents / (totalEvents || 1)) * 100, 100)}%` }}
              >
                <span className="bar-value">{failedEvents}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ThroughputChart;
