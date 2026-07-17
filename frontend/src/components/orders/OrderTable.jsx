import React from 'react';
import OrderRow from './OrderRow';
import './OrderTable.css';

const OrderTable = React.memo(({ orders }) => {
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
});

OrderTable.displayName = 'OrderTable';

export default OrderTable;
