import React from 'react';
import './BaseChart.css';

/**
 * Base chart component with common structure and styling
 * Provides consistent layout for all chart types
 */
const BaseChart = ({ 
  title, 
  icon, 
  data, 
  noDataMessage = 'No data available',
  children 
}) => {
  if (!data) {
    return (
      <div className="base-chart">
        <h3>{icon} {title}</h3>
        <p className="no-data">{noDataMessage}</p>
      </div>
    );
  }

  return (
    <div className="base-chart">
      <h3>{icon} {title}</h3>
      <div className="chart-content">
        {children}
      </div>
    </div>
  );
};

export default BaseChart;
