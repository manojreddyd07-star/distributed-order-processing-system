import React, { useState, useEffect } from 'react';
import './SharedTable.css';

/**
 * Generic SharedTable component for displaying data tables
 * @param {Function} fetchData - Function to fetch data
 * @param {Array} columns - Array of column definitions {key, label, render (optional)}
 * @param {any} refreshTrigger - Trigger to refresh data
 * @param {string} emptyMessage - Message to display when table is empty
 * @param {string} loadingMessage - Message to display while loading
 * @param {Function} rowKey - Function to get unique key for each row (default: row => row.id)
 */
const SharedTable = ({
  fetchData,
  columns,
  refreshTrigger,
  emptyMessage = 'No data available',
  loadingMessage = 'Loading data...',
  rowKey = (row) => row.id
}) => {
  const [data, setData] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadData();
  }, [refreshTrigger]);

  const loadData = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const result = await fetchData();
      setData(result);
    } catch (err) {
      setError(err.message || 'Failed to load data');
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="shared-table-container">
        <div className="loading-message">{loadingMessage}</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="shared-table-container">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={loadData} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="shared-table-container">
        <div className="empty-message">{emptyMessage}</div>
      </div>
    );
  }

  return (
    <div className="shared-table-container">
      <table className="shared-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key}>{column.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((column) => (
                <td key={column.key}>
                  {column.render ? column.render(row) : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default SharedTable;
