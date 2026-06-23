import React, { useState, useEffect } from 'react';
import InventoryTable from '../../components/inventory/InventoryTable';
import './InventoryPage.css';

const InventoryPage = () => {
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Mock inventory data
    const mockInventory = [
      {
        id: 1,
        productId: 101,
        productName: 'Laptop Dell XPS 15',
        availableQuantity: 25,
        inventoryStatus: 'AVAILABLE'
      },
      {
        id: 2,
        productId: 102,
        productName: 'iPhone 15 Pro',
        availableQuantity: 50,
        inventoryStatus: 'AVAILABLE'
      },
      {
        id: 3,
        productId: 103,
        productName: 'Samsung Galaxy S24',
        availableQuantity: 5,
        inventoryStatus: 'LOW_STOCK'
      },
      {
        id: 4,
        productId: 104,
        productName: 'MacBook Pro 16"',
        availableQuantity: 0,
        inventoryStatus: 'OUT_OF_STOCK'
      },
      {
        id: 5,
        productId: 105,
        productName: 'iPad Air',
        availableQuantity: 30,
        inventoryStatus: 'AVAILABLE'
      },
      {
        id: 6,
        productId: 106,
        productName: 'Sony WH-1000XM5 Headphones',
        availableQuantity: 15,
        inventoryStatus: 'AVAILABLE'
      },
      {
        id: 7,
        productId: 107,
        productName: 'LG OLED TV 55"',
        availableQuantity: 8,
        inventoryStatus: 'LOW_STOCK'
      },
      {
        id: 8,
        productId: 108,
        productName: 'Nintendo Switch OLED',
        availableQuantity: 40,
        inventoryStatus: 'AVAILABLE'
      }
    ];

    // Simulate API call delay
    setTimeout(() => {
      setInventory(mockInventory);
      setLoading(false);
    }, 500);
  }, []);

  return (
    <div className="inventory-page">
      <div className="inventory-page-header">
        <h1>Inventory</h1>
        <p>Manage and view product inventory levels</p>
      </div>
      <InventoryTable inventory={inventory} loading={loading} />
    </div>
  );
};

export default InventoryPage;
