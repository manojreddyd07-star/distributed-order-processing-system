import React from 'react';
import FulfillmentTable from '../../components/fulfillment/FulfillmentTable';
import './FulfillmentPage.css';

const FulfillmentPage = () => {
  return (
    <div className="fulfillment-page">
      <div className="fulfillment-page-header">
        <h1>Fulfillment</h1>
        <p>View and monitor order fulfillment</p>
      </div>
      <FulfillmentTable />
    </div>
  );
};

export default FulfillmentPage;
