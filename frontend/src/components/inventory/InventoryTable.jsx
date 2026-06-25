import React, { useState } from 'react';
import InventoryStatusBadge from './InventoryStatusBadge';
import InventoryDetailsPanel from './InventoryDetailsPanel';
import './InventoryTable.css';

const InventoryTable = ({ inventory, loading, onRefresh }) => {
  const [selectedInventory, setSelectedInventory] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  // Handle row click to show details
  const handleRowClick = (item) => {
    setSelectedInventory(item);
  };

  // Close details panel
  const handleCloseDetails = () => {
    setSelectedInventory(null);
  };

  // Handle refresh button click
  const handleRefresh = async () => {
    if (onRefresh) {
      setRefreshing(true);
      try {
        await onRefresh();
      } finally {
        setRefreshing(false);
      }
    }
  };

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
    <>
      <div className="inventory-table-container">
        <div className="table-actions">
          <span className="inventory-count">{inventory.length} product(s)</span>
          <button 
            className="refresh-button" 
            onClick={handleRefresh}
            disabled={loading || refreshing}
          >
            {refreshing ? '🔄 Refreshing...' : '🔄 Refresh'}
          </button>
        </div>
        
        <table className="inventory-table">
          <thead>
            <tr>
              <th>Product ID</th>
              <th>Product Name</th>
              <th>Available Quantity</th>
              <th>Reserved Quantity</th>
              <th>Total Quantity</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {inventory.map((item) => (
              <tr 
                key={item.id} 
                onClick={() => handleRowClick(item)}
                className="inventory-row"
              >
                <td className="product-id">{item.productId}</td>
                <td className="product-name">{item.productName}</td>
                <td className="available-quantity">{item.availableQuantity}</td>
                <td className="reserved-quantity">{item.reservedQuantity}</td>
                <td className="total-quantity">{item.totalQuantity}</td>
                <td className="status">
                  <InventoryStatusBadge status={item.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selectedInventory && (
        <InventoryDetailsPanel 
          inventory={selectedInventory} 
          onClose={handleCloseDetails} 
        />
      )}
    </>
  );
};

export default InventoryTable;
