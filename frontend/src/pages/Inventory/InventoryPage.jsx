import React, { useState, useEffect } from 'react';
import InventoryTable from '../../components/inventory/InventoryTable';
import { getAllInventory } from '../../shared/api/inventoryApi';
import './InventoryPage.css';

const InventoryPage = () => {
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [notification, setNotification] = useState(null);

  // Show notification
  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  // Fetch inventory data from API
  const fetchInventory = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const data = await getAllInventory();
      setInventory(data);
      showNotification('✅ Inventory data loaded successfully', 'success');
    } catch (err) {
      console.error('Error fetching inventory:', err);
      const errorMsg = 'Failed to load inventory data. Please try again.';
      setError(errorMsg);
      showNotification('❌ ' + errorMsg, 'error');
    } finally {
      setLoading(false);
    }
  };

  // Handle manual refresh
  const handleRefresh = async () => {
    try {
      const data = await getAllInventory();
      setInventory(data);
      showNotification('✅ Inventory data refreshed successfully', 'success');
    } catch (err) {
      console.error('Error refreshing inventory:', err);
      showNotification('❌ Failed to refresh inventory data', 'error');
    }
  };

  useEffect(() => {
    fetchInventory();
  }, []);

  // Filter inventory by status
  const filteredInventory = selectedStatus === 'ALL' 
    ? inventory 
    : inventory.filter(item => item.status === selectedStatus);

  return (
    <div className="inventory-page">
      <div className="inventory-page-header">
        <h1>Inventory</h1>
        <p>Manage and view product inventory levels</p>
      </div>

      {notification && (
        <div className={`notification ${notification.type}`}>
          {notification.message}
        </div>
      )}

      {error && (
        <div className="error-message">
          <p>{error}</p>
          <button onClick={fetchInventory}>Retry</button>
        </div>
      )}

      <div className="inventory-filters">
        <label>Filter by Status:</label>
        <select 
          value={selectedStatus} 
          onChange={(e) => setSelectedStatus(e.target.value)}
          className="status-filter-select"
        >
          <option value="ALL">All</option>
          <option value="IN_STOCK">In Stock</option>
          <option value="LOW_STOCK">Low Stock</option>
          <option value="OUT_OF_STOCK">Out of Stock</option>
        </select>
      </div>

      <InventoryTable 
        inventory={filteredInventory} 
        loading={loading}
        onRefresh={handleRefresh}
      />
    </div>
  );
};

export default InventoryPage;
