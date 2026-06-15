import React from 'react';
import OrderTable from '../../components/orders/OrderTable';
import OrderEventTimeline from '../../components/orders/OrderEventTimeline';
import './OrdersPage.css';

const OrdersPage = () => {
  return (
    <div className="orders-page">
      <div className="orders-page-header">
        <h1>Orders</h1>
        <p>Manage and view all orders in the system</p>
      </div>
      <OrderTable />
      <OrderEventTimeline />
    </div>
  );
};

export default OrdersPage;
