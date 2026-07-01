import React, { useState, useEffect } from 'react';
import { getAllOrders } from '../../shared/api/orderApi';
import OrderRow from './OrderRow';
import './OrderTable.css';

const OrderTable = ({ refreshTrigger }) => {
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Fetch orders on component mount and when refreshTrigger changes
  useEffect(() => {
    fetchOrders();
  }, [refreshTrigger]);

  const fetchOrders = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const data = await getAllOrders();
      setOrders(data);
    } catch (err) {
      setError(err.message || 'Failed to load orders');
    } finally {
      setIsLoading(false);
    }
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="order-table-container">
        <div className="loading-message">Loading orders...</div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="order-table-container">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={fetchOrders} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  // Empty state
  if (orders.length === 0) {
    return (
      <div className="order-table-container">
        <div className="empty-message">No orders found</div>
      </div>
    );
  }

  // Orders table
  return (
    <div className="order-table-container">
      <table className="order-table">
        <thead>
          <tr>
            <th>Order ID</th>
            <th>Customer ID</th>
            <th>Status</th>
            <th>Total Amount</th>
            <th>Created At</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <OrderRow key={order.id} order={order} />
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default OrderTable;
