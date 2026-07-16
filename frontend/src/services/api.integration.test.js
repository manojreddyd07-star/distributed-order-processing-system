import * as orderApi from './orderApi';
import * as validationApi from './validationApi';
import * as paymentApi from './paymentApi';
import * as inventoryApi from './inventoryApi';
import * as fulfillmentApi from './fulfillmentApi';
import * as monitoringApi from './monitoringApi';
import * as retryApi from './retryApi';
import * as dlqApi from './dlqApi';

// Mock fetch globally
global.fetch = jest.fn();

describe('API Integration Tests', () => {
  beforeEach(() => {
    fetch.mockClear();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  describe('Order API', () => {
    test('createOrder should send POST request with correct data', async () => {
      const mockOrder = {
        customerId: 'CUST-001',
        totalAmount: 99.99
      };
      
      const mockResponse = {
        id: 1,
        customerId: 'CUST-001',
        totalAmount: 99.99,
        status: 'CREATED'
      };

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      });

      const result = await orderApi.createOrder(mockOrder);

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/orders'),
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(mockOrder)
        })
      );
      expect(result).toEqual(mockResponse);
    });

    test('getAllOrders should send GET request', async () => {
      const mockOrders = [
        { id: 1, customerId: 'CUST-001', status: 'CREATED' },
        { id: 2, customerId: 'CUST-002', status: 'COMPLETED' }
      ];

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockOrders
      });

      const result = await orderApi.getAllOrders();

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/orders'),
        expect.objectContaining({
          method: 'GET',
          headers: { 'Content-Type': 'application/json' }
        })
      );
      expect(result).toEqual(mockOrders);
    });

    test('getOrderById should send GET request with order ID', async () => {
      const mockOrder = { id: 1, customerId: 'CUST-001', status: 'CREATED' };

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockOrder
      });

      const result = await orderApi.getOrderById(1);

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/orders/1'),
        expect.objectContaining({
          method: 'GET'
        })
      );
      expect(result).toEqual(mockOrder);
    });

    test('should handle API error correctly', async () => {
      fetch.mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: 'Order not found' })
      });

      await expect(orderApi.getOrderById(999)).rejects.toThrow('Order not found');
    });
  });

  describe('Validation API', () => {
    test('getAllValidations should verify API response', async () => {
      const mockValidations = [
        { orderId: 1, validationStatus: 'VALID', validationMessage: 'Success' },
        { orderId: 2, validationStatus: 'INVALID', validationMessage: 'Failed' }
      ];

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockValidations
      });

      const result = await validationApi.getAllValidations();

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/validations'),
        expect.any(Object)
      );
      expect(result).toEqual(mockValidations);
      expect(result[0].validationStatus).toBe('VALID');
      expect(result[1].validationStatus).toBe('INVALID');
    });
  });

  describe('Payment API', () => {
    test('getAllPayments should verify API response', async () => {
      const mockPayments = [
        { orderId: 1, paymentStatus: 'COMPLETED', amount: 99.99 },
        { orderId: 2, paymentStatus: 'PENDING', amount: 150.00 }
      ];

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockPayments
      });

      const result = await paymentApi.getAllPayments();

      expect(result).toEqual(mockPayments);
      expect(result).toHaveLength(2);
      expect(result[0].paymentStatus).toBe('COMPLETED');
    });
  });

  describe('Inventory API', () => {
    test('getAllInventoryItems should verify API response', async () => {
      const mockInventory = [
        { productId: 'PROD-001', availableQuantity: 100, reservedQuantity: 10 },
        { productId: 'PROD-002', availableQuantity: 50, reservedQuantity: 5 }
      ];

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockInventory
      });

      const result = await inventoryApi.getAllInventoryItems();

      expect(result).toEqual(mockInventory);
      expect(result[0].availableQuantity).toBe(100);
      expect(result[1].reservedQuantity).toBe(5);
    });
  });

  describe('Fulfillment API', () => {
    test('getAllFulfillments should verify API response', async () => {
      const mockFulfillments = [
        { orderId: 1, fulfillmentStatus: 'COMPLETED', customerId: 'CUST-001' },
        { orderId: 2, fulfillmentStatus: 'PROCESSING', customerId: 'CUST-002' }
      ];

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockFulfillments
      });

      const result = await fulfillmentApi.getAllFulfillments();

      expect(result).toEqual(mockFulfillments);
      expect(result[0].fulfillmentStatus).toBe('COMPLETED');
    });
  });

  describe('Monitoring API', () => {
    test('getSystemMetrics should verify API response structure', async () => {
      const mockMetrics = {
        totalOrders: 150,
        successfulOrders: 140,
        failedOrders: 10,
        averageProcessingTime: 2.5,
        systemHealth: 'healthy'
      };

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockMetrics
      });

      const result = await monitoringApi.getSystemMetrics();

      expect(result).toEqual(mockMetrics);
      expect(result.totalOrders).toBe(150);
      expect(result.systemHealth).toBe('healthy');
    });

    test('getEventMetrics should verify API response', async () => {
      const mockEventMetrics = {
        orderCreatedCount: 100,
        orderValidatedCount: 95,
        paymentCompletedCount: 90,
        inventoryReservedCount: 85,
        orderCompletedCount: 80
      };

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockEventMetrics
      });

      const result = await monitoringApi.getEventMetrics();

      expect(result).toEqual(mockEventMetrics);
      expect(result.orderCreatedCount).toBeGreaterThan(result.orderCompletedCount);
    });
  });

  describe('Retry API', () => {
    test('getAllRetryEvents should verify API response', async () => {
      const mockRetryEvents = [
        { retryId: 'R1', originalEventId: 'E1', retryCount: 1, maxRetries: 3 },
        { retryId: 'R2', originalEventId: 'E2', retryCount: 2, maxRetries: 3 }
      ];

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockRetryEvents
      });

      const result = await retryApi.getAllRetryEvents();

      expect(result).toEqual(mockRetryEvents);
      expect(result[0].retryCount).toBeLessThan(result[0].maxRetries);
    });
  });

  describe('DLQ API', () => {
    test('getAllFailedEvents should verify API response', async () => {
      const mockFailedEvents = [
        { eventId: 'E1', eventType: 'ORDER_CREATED', serviceName: 'validation-service' },
        { eventId: 'E2', eventType: 'PAYMENT_COMPLETED', serviceName: 'inventory-service' }
      ];

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockFailedEvents
      });

      const result = await dlqApi.getAllFailedEvents();

      expect(result).toEqual(mockFailedEvents);
      expect(result).toHaveLength(2);
    });
  });

  describe('API Error Handling', () => {
    test('should handle network errors', async () => {
      fetch.mockRejectedValueOnce(new Error('Network error'));

      await expect(orderApi.getAllOrders()).rejects.toThrow('Network error');
    });

    test('should handle 404 errors', async () => {
      fetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        json: async () => ({ message: 'Not found' })
      });

      await expect(orderApi.getOrderById(999)).rejects.toThrow('Not found');
    });

    test('should handle 500 errors', async () => {
      fetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({ message: 'Internal server error' })
      });

      await expect(orderApi.createOrder({})).rejects.toThrow('Internal server error');
    });
  });

  describe('API Response Validation', () => {
    test('should validate order response structure', async () => {
      const mockOrder = {
        id: 1,
        customerId: 'CUST-001',
        status: 'CREATED',
        totalAmount: 99.99,
        createdAt: '2026-07-16T10:00:00'
      };

      fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockOrder
      });

      const result = await orderApi.getOrderById(1);

      expect(result).toHaveProperty('id');
      expect(result).toHaveProperty('customerId');
      expect(result).toHaveProperty('status');
      expect(result).toHaveProperty('totalAmount');
      expect(result).toHaveProperty('createdAt');
      expect(typeof result.id).toBe('number');
      expect(typeof result.totalAmount).toBe('number');
    });
  });
});
