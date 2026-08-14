import React from 'react';
import ServiceStatusCard from './ServiceStatusCard';

const BackendStatusSection = React.memo(({ healthData }) => {
  const backendServices = [
    ...(healthData?.servicesHealth || []).map(service => ({
      name: service.serviceName,
      status: service.status,
      metrics: service.description ? { Status: service.description } : undefined,
    })),
    ...(healthData?.databaseHealth ? [{
      name: healthData.databaseHealth.serviceName,
      status: healthData.databaseHealth.status,
      metrics: healthData.databaseHealth.description
        ? { Status: healthData.databaseHealth.description }
        : undefined,
    }] : []),
  ];

  return (
    <div className="dashboard-section">
      <h2>Backend Services</h2>
      <div className="service-cards-grid">
        {backendServices.length === 0 && (
          <ServiceStatusCard service={{ name: 'Backend Services', status: 'UNKNOWN' }} />
        )}
        {backendServices.map((service) => (
          <ServiceStatusCard key={service.name} service={service} />
        ))}
      </div>
    </div>
  );
});

BackendStatusSection.displayName = 'BackendStatusSection';

export default BackendStatusSection;
