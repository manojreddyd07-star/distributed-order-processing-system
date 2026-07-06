import React from 'react';
import './RetryAttemptsTable.css';

const RetryAttemptsTable = ({ retryRecords }) => {
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'PENDING':
        return 'status-badge status-pending';
      case 'SUCCESS':
        return 'status-badge status-success';
      case 'EXHAUSTED':
        return 'status-badge status-exhausted';
      default:
        return 'status-badge';
    }
  };

  const getStatusIndicator = (status) => {
    switch (status) {
      case 'PENDING':
        return '⏳';
      case 'SUCCESS':
        return '✅';
      case 'EXHAUSTED':
        return '❌';
      default:
        return '⚪';
    }
  };

  if (retryRecords.length === 0) {
    return (
      <div className="no-retry-records">
        <p>No retry records found</p>
      </div>
    );
  }

  return (
    <div className="retry-table-container">
      <table className="retry-table">
        <thead>
          <tr>
            <th>Status</th>
            <th>Event ID</th>
            <th>Event Type</th>
            <th>Service</th>
            <th>Retry Count</th>
            <th>Last Retry Time</th>
            <th>Next Retry Time</th>
            <th>Failure Reason</th>
          </tr>
        </thead>
        <tbody>
          {retryRecords.map((record) => (
            <tr key={record.retryId}>
              <td>
                <span className={getStatusBadgeClass(record.retryStatus)}>
                  <span className="status-indicator">{getStatusIndicator(record.retryStatus)}</span>
                  {record.retryStatus}
                </span>
              </td>
              <td>
                <span className="event-id" title={record.originalEventId}>
                  {record.originalEventId.substring(0, 8)}...
                </span>
              </td>
              <td className="event-type">{record.eventType}</td>
              <td>
                <span className="service-badge">{record.serviceName}</span>
              </td>
              <td className="retry-count">
                <span className="retry-progress">
                  {record.retryCount} / {record.maxRetries}
                </span>
                <div className="retry-progress-bar">
                  <div 
                    className="retry-progress-fill"
                    style={{ width: `${(record.retryCount / record.maxRetries) * 100}%` }}
                  ></div>
                </div>
              </td>
              <td className="timestamp">{formatDate(record.lastRetryTime)}</td>
              <td className="timestamp">
                {record.nextRetryTime ? formatDate(record.nextRetryTime) : 'N/A'}
              </td>
              <td className="failure-reason">
                <span title={record.failureReason}>
                  {record.failureReason 
                    ? (record.failureReason.length > 50 
                        ? record.failureReason.substring(0, 50) + '...' 
                        : record.failureReason)
                    : 'N/A'}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default RetryAttemptsTable;
