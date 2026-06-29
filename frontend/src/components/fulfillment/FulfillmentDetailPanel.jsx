import React from 'react';
import FulfillmentStatusBadge from './FulfillmentStatusBadge';
import './FulfillmentDetailPanel.css';

const FulfillmentDetailPanel = ({ fulfillment, onClose }) => {
  if (!fulfillment) return null;

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
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

  return (
    <div className="fulfillment-details-overlay" onClick={onClose}>
      <div className="fulfillment-details-panel" onClick={(e) => e.stopPropagation()}>
        <div className="panel-header">
          <h2>Fulfillment Details</h2>
          <button className="close-btn" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="panel-content">
          <div className="detail-section">
            <h3>Fulfillment Information</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <label>Fulfillment ID</label>
                <div className="detail-value fulfillment-id-value">{fulfillment.fulfillmentId}</div>
              </div>
              <div className="detail-item">
                <label>Order ID</label>
                <div className="detail-value order-id-value">{fulfillment.orderId}</div>
              </div>
            </div>
          </div>

          <div className="detail-section">
            <h3>Customer Information</h3>
            <div className="detail-item">
              <label>Customer ID</label>
              <div className="detail-value customer-id-value">{fulfillment.customerId}</div>
            </div>
          </div>

          <div className="detail-section">
            <h3>Tracking Information</h3>
            <div className="detail-item">
              <label>Tracking Number</label>
              <div className="detail-value tracking-number-value">{fulfillment.trackingNumber}</div>
            </div>
          </div>

          <div className="detail-section">
            <h3>Status</h3>
            <div className="detail-item">
              <label>Fulfillment Status</label>
              <div className="detail-value">
                <FulfillmentStatusBadge status={fulfillment.fulfillmentStatus} />
              </div>
            </div>
          </div>

          <div className="detail-section">
            <h3>Timestamps</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <label>Created At</label>
                <div className="detail-value timestamp">{formatDate(fulfillment.createdAt)}</div>
              </div>
              <div className="detail-item">
                <label>Updated At</label>
                <div className="detail-value timestamp">{formatDate(fulfillment.updatedAt)}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FulfillmentDetailPanel;
