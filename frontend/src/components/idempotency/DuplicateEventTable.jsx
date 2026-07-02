import React, { useState, useEffect } from 'react';
import ProcessingStatus from './ProcessingStatus';
import { getAllIdempotencyRecords } from '../../services/idempotencyApi';
import './DuplicateEventTable.css';

const DuplicateEventTable = ({ statusFilter }) => {
  const [records, setRecords] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchIdempotencyRecords();
  }, [statusFilter]);

  const fetchIdempotencyRecords = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const data = await getAllIdempotencyRecords();
      
      // Filter by status if specified
      let filteredData = data;
      if (statusFilter && statusFilter !== 'ALL') {
        filteredData = data.filter(record => record.processingStatus === statusFilter);
      }
      
      // Sort by processed time (most recent first)
      filteredData.sort((a, b) => new Date(b.processedAt) - new Date(a.processedAt));
      
      setRecords(filteredData);
    } catch (err) {
      setError(err.message || 'Failed to load idempotency records');
      console.error('Error fetching idempotency records:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  if (isLoading) {
    return (
      <div className="duplicate-event-table-container">
        <div className="loading-message">Loading idempotency records...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="duplicate-event-table-container">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={fetchIdempotencyRecords} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (records.length === 0) {
    return (
      <div className="duplicate-event-table-container">
        <div className="empty-message">
          {statusFilter && statusFilter !== 'ALL' 
            ? `No ${statusFilter} events found` 
            : 'No idempotency records found'}
        </div>
      </div>
    );
  }

  return (
    <div className="duplicate-event-table-container">
      <div className="table-actions">
        <div className="records-count">
          {records.length} record{records.length !== 1 ? 's' : ''} found
        </div>
        <button onClick={fetchIdempotencyRecords} className="refresh-button">
          Refresh
        </button>
      </div>
      
      <div className="table-wrapper">
        <table className="duplicate-event-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Event ID</th>
              <th>Event Type</th>
              <th>Service Name</th>
              <th>Processing Status</th>
              <th>Processed At</th>
            </tr>
          </thead>
          <tbody>
            {records.map((record) => (
              <tr key={`${record.serviceName}-${record.id}`}>
                <td>{record.id}</td>
                <td className="event-id">{record.eventId}</td>
                <td>{record.eventType}</td>
                <td className="service-name">{record.serviceName}</td>
                <td>
                  <ProcessingStatus status={record.processingStatus} />
                </td>
                <td>{formatDateTime(record.processedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default DuplicateEventTable;
