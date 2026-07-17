import React from 'react';
import ServiceStatusCard from './ServiceStatusCard';

const BackendStatusSection = React.memo(() => {
  const backendServices = [
    {
      name: 'Order Service',
      status: 'Healthy',
      port: '8080',
      version: '1.0.0',
      uptime: '2h 10m',
      metrics: {
        'Total Orders': '1,247',
        'Pending': '23',
        'Completed': '1,224',
        'Avg Response': '45ms'
      }
    },
    {
      name: 'PostgreSQL',
      status: 'Healthy',
      port: '5432',
      version: '15-alpine',
      uptime: '2h 16m',
      metrics: {
        'Database Size': '156 MB',
        'Connections': '8/100',
        'Cache Hit Rate': '98.5%',
        'Active Queries': '2'
      }
    }
  ];

  return (
    <div className="dashboard-section">
      <h2>Backend Services</h2>
      <div className="service-cards-grid">
        {backendServices.map((service) => (
          <ServiceStatusCard key={service.name} service={service} />
        ))}
      </div>
    </div>
  );
});

BackendStatusSection.displayName = 'BackendStatusSection';

export default BackendStatusSection;
