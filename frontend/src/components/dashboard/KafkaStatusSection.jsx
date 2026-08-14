import React from 'react';
import ServiceStatusCard from './ServiceStatusCard';

const KafkaStatusSection = React.memo(({ healthData }) => {
  const kafkaServices = healthData?.kafkaHealth ? [{
    name: healthData.kafkaHealth.serviceName,
    status: healthData.kafkaHealth.status,
    metrics: healthData.kafkaHealth.description
      ? { Status: healthData.kafkaHealth.description }
      : undefined,
  }] : [{ name: 'Apache Kafka', status: 'UNKNOWN' }];

  return (
    <div className="dashboard-section">
      <h2>Kafka Infrastructure</h2>
      <div className="service-cards-grid">
        {kafkaServices.map((service) => (
          <ServiceStatusCard key={service.name} service={service} />
        ))}
      </div>
    </div>
  );
});

KafkaStatusSection.displayName = 'KafkaStatusSection';

export default KafkaStatusSection;
