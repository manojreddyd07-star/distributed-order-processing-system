import React, { useState, useEffect } from 'react';
import FulfillmentStatusBadge from './FulfillmentStatusBadge';
import { getAllFulfillments } from '../../services/fulfillmentApi';
import './FulfillmentTable.css';

const FulfillmentTable = ({ refreshTrigger }) => {
  const [fulfillments, setFulfillments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
    fetchFulfillments();
  }, [refreshTrigger]);

  const fetchFulfillments = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const data = await getAllFulfillments();
      setFulfillments(data);
      setSuccessMessage('Fulfillments loaded successfully');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err) {
      console.error('Error loading fulfillments:', err);
      setError(err.message || 'Failed to load fulfillments. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRefresh = () => {
    fetchFulfillments();
  };

  // Format date/time for display
  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="fulfillment-table-container">
        <div className="loading-message">Loading fulfillments...</div>
      </div>
    );
  }

  // Main table view
  return (
    <div className="fulfillment-table-container">
      {/* Success Notification */}
      {successMessage && (
        <div className="notification success-notification">
          <span className="notification-icon">✓</span>
          {successMessage}
        </div>
      )}
      
      {/* Error Notification */}
      {error && (
        <div className="notification error-notification">
          <span className="notification-icon">⚠</span>
          {error}
          <button 
            className="retry-button"
            onClick={handleRefresh}
          >
            Retry
          </button>
        </div>
      )}
      
      <div className="table-header">
        <h2>Fulfillment Records</h2>
        <div className="table-actions">
          <div className="table-info">
            Total: <strong>{fulfillments.length}</strong> fulfillments
          </div>
          <button 
            className="refresh-button" 
            onClick={handleRefresh}
            disabled={isLoading}
            title="Refresh fulfillments"
          >
            <span className="refresh-icon">↻</span>
            Refresh
          </button>
        </div>
      </div>
      
      <div className="table-wrapper">
        {fulfillments.length === 0 ? (
          <div className="empty-message">No fulfillments found</div>
        ) : (
          <table className="fulfillment-table">
            <thead>
              <tr>
                <th>Fulfillment ID</th>
                <th>Order ID</th>
                <th>Customer ID</th>
                <th>Tracking Number</th>
                <th>Status</th>
                <th>Created Date</th>
                <th>Updated Date</th>
              </tr>
            </thead>
            <tbody>
              {fulfillments.map((fulfillment) => (
                <tr key={fulfillment.fulfillmentId} className="fulfillment-row">
                  <td className="fulfillment-id">{fulfillment.fulfillmentId}</td>
                  <td className="order-id">{fulfillment.orderId}</td>
                  <td className="customer-id">{fulfillment.customerId}</td>
                  <td className="tracking-number">{fulfillment.trackingNumber}</td>
                  <td>
                    <FulfillmentStatusBadge status={fulfillment.fulfillmentStatus} />
                  </td>
                  <td className="created-date">
                    {formatDateTime(fulfillment.createdAt)}
                  </td>
                  <td className="updated-date">
                    {formatDateTime(fulfillment.updatedAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default FulfillmentTable;
