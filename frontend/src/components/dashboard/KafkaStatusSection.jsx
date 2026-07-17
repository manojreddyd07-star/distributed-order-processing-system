import React from 'react';
import ServiceStatusCard from './ServiceStatusCard';

const KafkaStatusSection = React.memo(() => {
  const kafkaServices = [
    {
      name: 'Zookeeper',
      status: 'Healthy',
      port: '2181',
      version: '7.5.0',
      uptime: '2h 15m',
      metrics: {
        'Connections': '3',
        'Nodes': '1'
      }
    },
    {
      name: 'Kafka Broker',
      status: 'Healthy',
      port: '9092',
      version: '7.5.0',
      uptime: '2h 14m',
      metrics: {
        'Topics': '1',
        'Partitions': '3',
        'Messages/sec': '125',
        'Throughput': '2.5 MB/s'
      }
    }
  ];

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
