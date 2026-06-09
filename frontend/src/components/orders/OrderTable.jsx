import React from 'react';
import OrderRow from './OrderRow';
import './OrderTable.css';

const OrderTable = () => {
  // Mock orders array
  const mockOrders = [
    {
      id: 1,
      customerId: 101,
      orderStatus: 'PENDING',
      totalAmount: 299.99,
      createdAt: '2026-06-09T10:30:00'
    },
    {
      id: 2,
      customerId: 102,
      orderStatus: 'PROCESSING',
      totalAmount: 549.50,
      createdAt: '2026-06-09T11:15:00'
    },
    {
      id: 3,
      customerId: 103,
      orderStatus: 'COMPLETED',
      totalAmount: 125.75,
      createdAt: '2026-06-09T09:45:00'
    },
    {
      id: 4,
      customerId: 101,
      orderStatus: 'PENDING',
      totalAmount: 899.00,
      createdAt: '2026-06-09T12:00:00'
    },
    {
      id: 5,
      customerId: 104,
      orderStatus: 'FAILED',
      totalAmount: 75.25,
      createdAt: '2026-06-09T08:20:00'
    }
  ];

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
          {mockOrders.map((order) => (
            <OrderRow key={order.id} order={order} />
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default OrderTable;
