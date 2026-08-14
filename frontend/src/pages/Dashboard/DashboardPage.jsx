import React, { useEffect, useState } from 'react';
import './DashboardPage.css';
import KafkaStatusSection from '../../components/dashboard/KafkaStatusSection';
import BackendStatusSection from '../../components/dashboard/BackendStatusSection';
import { getHealthMetrics } from '../../services/monitoringApi';

const DashboardPage = () => {
  const [healthData, setHealthData] = useState(null);

  useEffect(() => {
    let active = true;

    const loadHealth = async () => {
      try {
        const data = await getHealthMetrics();
        if (active) setHealthData(data);
      } catch (error) {
        console.error('Error loading dashboard health:', error);
        if (active) setHealthData({ servicesHealth: [] });
      }
    };

    loadHealth();
    return () => { active = false; };
  }, []);

  return (
    <div className="dashboard-page">
      <h1>System Dashboard</h1>
      <p className="dashboard-subtitle">Monitor the health and status of all services</p>
      
      <div className="dashboard-sections">
        <BackendStatusSection healthData={healthData} />
        <KafkaStatusSection healthData={healthData} />
      </div>
    </div>
  );
};

export default DashboardPage;
