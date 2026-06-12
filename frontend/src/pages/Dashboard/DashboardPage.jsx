import React from 'react';
import './DashboardPage.css';
import KafkaStatusSection from '../../components/dashboard/KafkaStatusSection';
import BackendStatusSection from '../../components/dashboard/BackendStatusSection';

const DashboardPage = () => {
  return (
    <div className="dashboard-page">
      <h1>System Dashboard</h1>
      <p className="dashboard-subtitle">Monitor the health and status of all services</p>
      
      <div className="dashboard-sections">
        <BackendStatusSection />
        <KafkaStatusSection />
      </div>
    </div>
  );
};

export default DashboardPage;
