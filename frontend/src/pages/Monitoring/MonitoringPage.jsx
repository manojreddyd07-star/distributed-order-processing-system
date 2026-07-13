import React, { useState, useEffect } from 'react';
import MetricsCard from '../../components/monitoring/MetricsCard';
import HealthGrid from '../../components/monitoring/HealthGrid';
import ThroughputChart from '../../components/monitoring/ThroughputChart';
import LatencyChart from '../../components/monitoring/LatencyChart';
import FailureMetricsCard from '../../components/monitoring/FailureMetricsCard';
import { getHealthMetrics, getApplicationMetrics, getPerformanceMetrics } from '../../services/monitoringApi';
import './MonitoringPage.css';

const MonitoringPage = () => {
  const [healthData, setHealthData] = useState(null);
  const [metricsData, setMetricsData] = useState(null);
  const [performanceData, setPerformanceData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [timeWindow, setTimeWindow] = useState(5); // Default 5 minutes

  const fetchData = async () => {
    try {
      setError(null);
      const [health, metrics, performance] = await Promise.all([
        getHealthMetrics(),
        getApplicationMetrics(),
        getPerformanceMetrics(timeWindow),
      ]);
      
      setHealthData(health);
      setMetricsData(metrics);
      setPerformanceData(performance);
      setLastUpdated(new Date());
      setLoading(false);
    } catch (err) {
      console.error('Error fetching monitoring data:', err);
      setError('Failed to fetch monitoring data. Please ensure the monitoring service is running.');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [timeWindow]);

  useEffect(() => {
    if (autoRefresh) {
      const interval = setInterval(() => {
        fetchData();
      }, 30000); // Refresh every 30 seconds

      return () => clearInterval(interval);
    }
  }, [autoRefresh, timeWindow]);

  const handleRefresh = () => {
    setLoading(true);
    fetchData();
  };

  const toggleAutoRefresh = () => {
    setAutoRefresh(!autoRefresh);
  };

  const formatBytes = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
  };

  const formatPercentage = (value) => {
    return value ? `${value.toFixed(2)}%` : '0%';
  };

  if (loading && !healthData && !metricsData) {
    return (
      <div className="monitoring-page">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Loading monitoring data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="monitoring-page">
      <div className="monitoring-header">
        <div>
          <h1>System Monitoring Dashboard</h1>
          {lastUpdated && (
            <p className="last-updated">
              Last updated: {lastUpdated.toLocaleTimeString()}
            </p>
          )}
        </div>
        <div className="monitoring-controls">
          <button
            className={`auto-refresh-btn ${autoRefresh ? 'active' : ''}`}
            onClick={toggleAutoRefresh}
          >
            {autoRefresh ? '⏸ Pause Auto-Refresh' : '▶ Enable Auto-Refresh'}
          </button>
          <button className="refresh-btn" onClick={handleRefresh} disabled={loading}>
            {loading ? '↻ Refreshing...' : '↻ Refresh Now'}
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner">
          <span className="error-icon">⚠</span>
          <span>{error}</span>
        </div>
      )}

      {/* Overall Status */}
      {healthData && (
        <div className="status-overview">
          <MetricsCard
            title="Overall System Status"
            value={healthData.overallStatus}
            status={healthData.overallStatus}
            icon="🖥"
          />
        </div>
      )}

      {/* Service Health Grid */}
      {healthData && (
        <div className="health-section">
          <HealthGrid
            services={healthData.servicesHealth}
            databaseHealth={healthData.databaseHealth}
            kafkaHealth={healthData.kafkaHealth}
          />
        </div>
      )}

      {/* Application Metrics */}
      {metricsData && (
        <div className="metrics-section">
          <h2 className="section-title">Application Metrics</h2>

          {/* JVM Metrics */}
          <div className="metrics-category">
            <h3 className="category-title">JVM Metrics</h3>
            <div className="metrics-grid">
              <MetricsCard
                title="Memory Usage"
                value={formatPercentage(metricsData.jvmMetrics?.memoryUsagePercentage)}
                icon="💾"
                details={{
                  'Used': formatBytes(metricsData.jvmMetrics?.memoryUsed),
                  'Max': formatBytes(metricsData.jvmMetrics?.memoryMax),
                  'Committed': formatBytes(metricsData.jvmMetrics?.memoryCommitted),
                }}
              />
              <MetricsCard
                title="Memory Used"
                value={formatBytes(metricsData.jvmMetrics?.memoryUsed)}
                icon="📊"
              />
              <MetricsCard
                title="Threads Live"
                value={metricsData.jvmMetrics?.threadsLive}
                icon="🧵"
                details={{
                  'Peak': metricsData.jvmMetrics?.threadsPeak,
                  'Daemon': metricsData.jvmMetrics?.threadsDaemon,
                }}
              />
            </div>
          </div>

          {/* System Metrics */}
          <div className="metrics-category">
            <h3 className="category-title">System Metrics</h3>
            <div className="metrics-grid">
              <MetricsCard
                title="CPU Usage"
                value={formatPercentage(metricsData.systemMetrics?.cpuUsage)}
                icon="⚙️"
              />
              <MetricsCard
                title="CPU Count"
                value={metricsData.systemMetrics?.cpuCount}
                icon="🔢"
              />
              <MetricsCard
                title="System Load Average"
                value={metricsData.systemMetrics?.systemLoadAverage?.toFixed(2)}
                icon="📈"
              />
            </div>
          </div>

          {/* HTTP Metrics */}
          <div className="metrics-category">
            <h3 className="category-title">HTTP Metrics</h3>
            <div className="metrics-grid">
              <MetricsCard
                title="Total Requests"
                value={metricsData.httpMetrics?.totalRequests}
                icon="🌐"
              />
              <MetricsCard
                title="Successful Requests"
                value={metricsData.httpMetrics?.successfulRequests}
                icon="✅"
              />
              <MetricsCard
                title="Failed Requests"
                value={metricsData.httpMetrics?.failedRequests}
                icon="❌"
              />
            </div>
          </div>
        </div>
      )}

      {/* Performance Metrics Section */}
      {performanceData && (
        <div className="performance-section">
          <div className="section-header">
            <h2 className="section-title">Performance Metrics</h2>
            <div className="time-window-selector">
              <label>Time Window:</label>
              <select 
                value={timeWindow} 
                onChange={(e) => setTimeWindow(Number(e.target.value))}
                className="time-window-dropdown"
              >
                <option value={5}>Last 5 minutes</option>
                <option value={15}>Last 15 minutes</option>
                <option value={30}>Last 30 minutes</option>
                <option value={60}>Last 1 hour</option>
              </select>
            </div>
          </div>

          <ThroughputChart throughputData={performanceData.throughput} />
          <LatencyChart latencyData={performanceData.latency} />
          <FailureMetricsCard failureData={performanceData.failure} />
        </div>
      )}
    </div>
  );
};

export default MonitoringPage;
