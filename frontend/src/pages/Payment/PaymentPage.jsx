import React from 'react';
import PaymentTable from '../../components/payment/PaymentTable';
import './PaymentPage.css';

const PaymentPage = () => {
  return (
    <div className="payment-page">
      <div className="payment-page-header">
        <h1>Payments</h1>
        <p>View and monitor payment transactions</p>
      </div>
      <PaymentTable />
    </div>
  );
};

export default PaymentPage;
