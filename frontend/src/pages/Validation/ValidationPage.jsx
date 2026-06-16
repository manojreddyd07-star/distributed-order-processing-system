import React from 'react';
import ValidationTable from '../../components/validation/ValidationTable';
import './ValidationPage.css';

const ValidationPage = () => {
  return (
    <div className="validation-page">
      <div className="validation-page-header">
        <h1>Validations</h1>
        <p>View and monitor order validation results</p>
      </div>
      <ValidationTable />
    </div>
  );
};

export default ValidationPage;
