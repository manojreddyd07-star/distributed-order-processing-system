import React from 'react';
import InventoryStatusBadge from './InventoryStatusBadge';
import './InventoryTable.css';

const InventoryTable = ({ inventory, loading }) => {
  // Loading state
  if (loading) {
    return (
      <div className="inventory-table-container">
        <div className="loading-message">Loading inventory...</div>
      </div>
    );
  }

  // Empty state
  if (inventory.length === 0) {
    return (
      <div className="inventory-table-container">
        <div className="empty-message">No inventory items found</div>
      </div>
    );
  }

  // Inventory table
  return (
    <div className="inventory-table-container">
      <div className="table-actions">
        <span className="inventory-count">{inventory.length} product(s)</span>
      </div>
      
      <table className="inventory-table">
        <thead>
          <tr>
            <th>Product ID</th>
            <th>Product Name</th>
            <th>Available Quantity</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {inventory.map((item) => (
            <tr key={item.id}>
              <td className="product-id">{item.productId}</td>
              <td className="product-name">{item.productName}</td>
              <td className="available-quantity">{item.availableQuantity}</td>
              <td className="status">
                <InventoryStatusBadge status={item.inventoryStatus} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default InventoryTable;
