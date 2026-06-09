import React from 'react';
import OrderTable from '../../components/orders/OrderTable';
import './OrdersPage.css';

const OrdersPage = () => {
  return (
    <div className="orders-page">
      <div className="orders-page-header">
        <h1>Orders</h1>
        <p>Manage and view all orders in the system</p>
      </div>
      <OrderTable />
    </div>
  );
};

export default OrdersPage;
