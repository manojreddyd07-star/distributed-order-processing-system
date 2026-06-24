import React from 'react';
import InventoryStatusBadge from './InventoryStatusBadge';
import './InventoryDetailsPanel.css';

const InventoryDetailsPanel = ({ inventory, onClose }) => {
  if (!inventory) return null;

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="inventory-details-overlay" onClick={onClose}>
      <div className="inventory-details-panel" onClick={(e) => e.stopPropagation()}>
        <div className="panel-header">
          <h2>Inventory Details</h2>
          <button className="close-btn" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="panel-content">
          <div className="detail-section">
            <h3>Product Information</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <label>Product ID</label>
                <div className="detail-value product-id-value">{inventory.productId}</div>
              </div>
              <div className="detail-item">
                <label>Product Name</label>
                <div className="detail-value">{inventory.productName}</div>
              </div>
            </div>
          </div>

          <div className="detail-section">
            <h3>Quantity Information</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <label>Available Quantity</label>
                <div className="detail-value quantity-available">{inventory.availableQuantity}</div>
              </div>
              <div className="detail-item">
                <label>Reserved Quantity</label>
                <div className="detail-value quantity-reserved">{inventory.reservedQuantity}</div>
              </div>
              <div className="detail-item">
                <label>Total Quantity</label>
                <div className="detail-value quantity-total">{inventory.totalQuantity}</div>
              </div>
            </div>
          </div>

          <div className="detail-section">
            <h3>Status</h3>
            <div className="detail-item">
              <label>Inventory Status</label>
              <div className="detail-value">
                <InventoryStatusBadge status={inventory.status} />
              </div>
            </div>
          </div>

          <div className="detail-section">
            <h3>Timestamps</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <label>Created At</label>
                <div className="detail-value timestamp">{formatDate(inventory.createdAt)}</div>
              </div>
              <div className="detail-item">
                <label>Updated At</label>
                <div className="detail-value timestamp">{formatDate(inventory.updatedAt)}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InventoryDetailsPanel;
