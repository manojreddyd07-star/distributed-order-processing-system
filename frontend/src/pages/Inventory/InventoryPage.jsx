import React, { useState, useEffect } from 'react';
import InventoryTable from '../../components/inventory/InventoryTable';
import { getAllInventory } from '../../services/inventoryApi';
import './InventoryPage.css';

const InventoryPage = () => {
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedStatus, setSelectedStatus] = useState('ALL');

  // Fetch inventory data from API
  const fetchInventory = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const data = await getAllInventory();
      setInventory(data);
    } catch (err) {
      console.error('Error fetching inventory:', err);
      setError('Failed to load inventory data. Please try again.');
    } finally {
      setLoading(false);
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

      <InventoryTable inventory={filteredInventory} loading={loading} />
    </div>
  );
};

export default InventoryPage;
