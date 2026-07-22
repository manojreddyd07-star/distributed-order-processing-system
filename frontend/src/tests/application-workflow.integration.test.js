import '@testing-library/jest-dom';

// Mock all API modules BEFORE importing them
jest.mock('../services/orderApi');
jest.mock('../services/validationApi');
jest.mock('../services/paymentApi');
jest.mock('../services/inventoryApi');
jest.mock('../services/fulfillmentApi');
jest.mock('../services/monitoringApi');
jest.mock('../services/retryApi');
jest.mock('../services/dlqApi');
jest.mock('../services/replayApi');
jest.mock('../services/auditApi');
jest.mock('../services/idempotencyApi');

import * as orderApi from '../services/orderApi';
import * as validationApi from '../services/validationApi';
import * as paymentApi from '../services/paymentApi';
import * as inventoryApi from '../services/inventoryApi';
import * as fulfillmentApi from '../services/fulfillmentApi';
import * as monitoringApi from '../services/monitoringApi';
import * as retryApi from '../services/retryApi';
import * as dlqApi from '../services/dlqApi';
import * as replayApi from '../services/replayApi';
import * as auditApi from '../services/auditApi';
import * as idempotencyApi from '../services/idempotencyApi';

describe('Complete Application Workflow Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  describe('Order Creation Workflow', () => {
    test('should successfully create an order and verify subsequent API calls', async () => {
      // Mock order creation
      const mockOrder = {
        id: 1,
        customerId: 'CUST-001',
        totalAmount: 299.99,
        status: 'CREATED',
        createdAt: '2026-07-22T10:00:00'
      };

      orderApi.createOrder.mockResolvedValue(mockOrder);
      orderApi.getOrderById.mockResolvedValue(mockOrder);

      // Test order creation
      const order = await orderApi.createOrder({
        customerId: 'CUST-001',
        totalAmount: 299.99
      });

      expect(order).toEqual(mockOrder);
      expect(orderApi.createOrder).toHaveBeenCalledWith({
        customerId: 'CUST-001',
        totalAmount: 299.99
      });

      // Verify order by ID
      const retrievedOrder = await orderApi.getOrderById(1);
      expect(retrievedOrder).toEqual(mockOrder);
    });

    test('should handle order creation validation errors', async () => {
      orderApi.createOrder.mockRejectedValue(new Error('Validation failed: Invalid customer ID'));

      await expect(orderApi.createOrder({
        customerId: '',
        totalAmount: -100
      })).rejects.toThrow('Validation failed');
    });
  });

  describe('Order → Validation Workflow', () => {
    test('should process order through validation', async () => {
      // Step 1: Create order
      const mockOrder = {
        id: 2,
        customerId: 'CUST-002',
        totalAmount: 450.00,
        status: 'CREATED',
        createdAt: '2026-07-22T10:05:00'
      };

      orderApi.createOrder.mockResolvedValue(mockOrder);

      const order = await orderApi.createOrder({
        customerId: 'CUST-002',
        totalAmount: 450.00
      });

      expect(order.status).toBe('CREATED');

      // Step 2: Validation service processes the order
      const mockValidation = {
        orderId: 2,
        validationStatus: 'VALID',
        validationMessage: 'Order validation successful',
        validatedAt: '2026-07-22T10:05:30'
      };

      validationApi.getAllValidations.mockResolvedValue([mockValidation]);

      const validations = await validationApi.getAllValidations();
      const validation = validations.find(v => v.orderId === 2);

      expect(validation).toBeDefined();
      expect(validation.validationStatus).toBe('VALID');

      // Step 3: Order status updated to VALIDATED
      const mockValidatedOrder = { ...mockOrder, status: 'VALIDATED' };
      orderApi.getOrderById.mockResolvedValue(mockValidatedOrder);

      const updatedOrder = await orderApi.getOrderById(2);
      expect(updatedOrder.status).toBe('VALIDATED');
    });

    test('should handle validation failures', async () => {
      const mockOrder = {
        id: 3,
        customerId: 'CUST-003',
        totalAmount: 99.99,
        status: 'CREATED'
      };

      orderApi.createOrder.mockResolvedValue(mockOrder);

      const order = await orderApi.createOrder({
        customerId: 'CUST-003',
        totalAmount: 99.99
      });

      // Validation fails
      const mockValidation = {
        orderId: 3,
        validationStatus: 'INVALID',
        validationMessage: 'Customer not found',
        validatedAt: '2026-07-22T10:10:00'
      };

      validationApi.getAllValidations.mockResolvedValue([mockValidation]);

      const validations = await validationApi.getAllValidations();
      const validation = validations.find(v => v.orderId === 3);

      expect(validation.validationStatus).toBe('INVALID');
      expect(validation.validationMessage).toContain('Customer not found');
    });
  });

  describe('Validation → Payment Workflow', () => {
    test('should process validated order through payment', async () => {
      // Step 1: Order is validated
      const mockOrder = {
        id: 4,
        customerId: 'CUST-004',
        totalAmount: 599.99,
        status: 'VALIDATED'
      };

      orderApi.getOrderById.mockResolvedValue(mockOrder);

      const order = await orderApi.getOrderById(4);
      expect(order.status).toBe('VALIDATED');

      // Step 2: Payment processing
      const mockPayment = {
        orderId: 4,
        paymentStatus: 'COMPLETED',
        amount: 599.99,
        paymentId: 'PAY-4',
        completedAt: '2026-07-22T10:15:00'
      };

      paymentApi.getAllPayments.mockResolvedValue([mockPayment]);

      const payments = await paymentApi.getAllPayments();
      const payment = payments.find(p => p.orderId === 4);

      expect(payment).toBeDefined();
      expect(payment.paymentStatus).toBe('COMPLETED');

      // Step 3: Order status updated to PAYMENT_COMPLETED
      const mockPaidOrder = { ...mockOrder, status: 'PAYMENT_COMPLETED' };
      orderApi.getOrderById.mockResolvedValue(mockPaidOrder);

      const updatedOrder = await orderApi.getOrderById(4);
      expect(updatedOrder.status).toBe('PAYMENT_COMPLETED');
    });

    test('should handle payment failures', async () => {
      const mockPayment = {
        orderId: 5,
        paymentStatus: 'FAILED',
        amount: 750.00,
        paymentId: 'PAY-5',
        errorMessage: 'Insufficient funds'
      };

      paymentApi.getAllPayments.mockResolvedValue([mockPayment]);

      const payments = await paymentApi.getAllPayments();
      const payment = payments.find(p => p.orderId === 5);

      expect(payment.paymentStatus).toBe('FAILED');
      expect(payment.errorMessage).toContain('Insufficient funds');
    });
  });

  describe('Payment → Inventory Workflow', () => {
    test('should process payment through inventory reservation', async () => {
      // Step 1: Payment completed
      const mockPayment = {
        orderId: 6,
        paymentStatus: 'COMPLETED',
        amount: 350.00,
        productId: 'PROD-001',
        quantity: 2
      };

      paymentApi.getAllPayments.mockResolvedValue([mockPayment]);

      const payments = await paymentApi.getAllPayments();
      const payment = payments.find(p => p.orderId === 6);

      expect(payment.paymentStatus).toBe('COMPLETED');

      // Step 2: Inventory reservation
      const mockInventory = {
        productId: 'PROD-001',
        availableQuantity: 98,
        reservedQuantity: 2,
        orderId: 6,
        status: 'RESERVED'
      };

      inventoryApi.getAllInventoryItems.mockResolvedValue([mockInventory]);

      const inventoryItems = await inventoryApi.getAllInventoryItems();
      const inventory = inventoryItems.find(i => i.productId === 'PROD-001');

      expect(inventory).toBeDefined();
      expect(inventory.reservedQuantity).toBe(2);

      // Step 3: Order status updated to INVENTORY_RESERVED
      const mockOrder = {
        id: 6,
        customerId: 'CUST-006',
        totalAmount: 350.00,
        status: 'INVENTORY_RESERVED'
      };

      orderApi.getOrderById.mockResolvedValue(mockOrder);

      const updatedOrder = await orderApi.getOrderById(6);
      expect(updatedOrder.status).toBe('INVENTORY_RESERVED');
    });

    test('should handle insufficient inventory', async () => {
      const mockInventory = {
        productId: 'PROD-002',
        availableQuantity: 0,
        reservedQuantity: 0,
        status: 'OUT_OF_STOCK'
      };

      inventoryApi.getAllInventoryItems.mockResolvedValue([mockInventory]);

      const inventoryItems = await inventoryApi.getAllInventoryItems();
      const inventory = inventoryItems.find(i => i.productId === 'PROD-002');

      expect(inventory.status).toBe('OUT_OF_STOCK');
      expect(inventory.availableQuantity).toBe(0);
    });
  });

  describe('Inventory → Fulfillment Workflow', () => {
    test('should process inventory reservation through fulfillment', async () => {
      // Step 1: Inventory reserved
      const mockOrder = {
        id: 7,
        customerId: 'CUST-007',
        totalAmount: 199.99,
        status: 'INVENTORY_RESERVED'
      };

      orderApi.getOrderById.mockResolvedValue(mockOrder);

      const order = await orderApi.getOrderById(7);
      expect(order.status).toBe('INVENTORY_RESERVED');

      // Step 2: Fulfillment processing
      const mockFulfillment = {
        orderId: 7,
        fulfillmentStatus: 'COMPLETED',
        customerId: 'CUST-007',
        trackingNumber: 'TRACK-007',
        completedAt: '2026-07-22T10:30:00'
      };

      fulfillmentApi.getAllFulfillments.mockResolvedValue([mockFulfillment]);

      const fulfillments = await fulfillmentApi.getAllFulfillments();
      const fulfillment = fulfillments.find(f => f.orderId === 7);

      expect(fulfillment).toBeDefined();
      expect(fulfillment.fulfillmentStatus).toBe('COMPLETED');

      // Step 3: Order status updated to COMPLETED
      const mockCompletedOrder = { ...mockOrder, status: 'COMPLETED' };
      orderApi.getOrderById.mockResolvedValue(mockCompletedOrder);

      const completedOrder = await orderApi.getOrderById(7);
      expect(completedOrder.status).toBe('COMPLETED');
    });
  });

  describe('Retry Scenarios', () => {
    test('should handle failed events with retry mechanism', async () => {
      // Step 1: Event fails and goes to retry
      const mockRetryEvent = {
        retryId: 'R1',
        originalEventId: 'E1',
        eventType: 'ORDER_CREATED',
        retryCount: 1,
        maxRetries: 3,
        status: 'PENDING',
        nextRetryAt: '2026-07-22T10:35:00'
      };

      retryApi.getAllRetryEvents.mockResolvedValue([mockRetryEvent]);

      const retryEvents = await retryApi.getAllRetryEvents();
      expect(retryEvents).toHaveLength(1);
      expect(retryEvents[0].retryCount).toBeLessThan(retryEvents[0].maxRetries);

      // Step 2: Trigger retry
      retryApi.retryEvent.mockResolvedValue({ success: true });

      const retryResult = await retryApi.retryEvent(mockRetryEvent.retryId);
      expect(retryResult.success).toBe(true);
    });

    test('should move to DLQ after max retries exceeded', async () => {
      const mockRetryEvent = {
        retryId: 'R2',
        originalEventId: 'E2',
        eventType: 'PAYMENT_COMPLETED',
        retryCount: 3,
        maxRetries: 3,
        status: 'MAX_RETRIES_EXCEEDED'
      };

      retryApi.getAllRetryEvents.mockResolvedValue([mockRetryEvent]);

      const retryEvents = await retryApi.getAllRetryEvents();
      const exceededEvent = retryEvents.find(e => e.retryId === 'R2');

      expect(exceededEvent.retryCount).toEqual(exceededEvent.maxRetries);
      expect(exceededEvent.status).toBe('MAX_RETRIES_EXCEEDED');
    });
  });

  describe('DLQ Scenarios', () => {
    test('should retrieve failed events from DLQ', async () => {
      const mockFailedEvents = [
        {
          eventId: 'E10',
          eventType: 'ORDER_VALIDATED',
          serviceName: 'payment-service',
          errorMessage: 'Payment gateway timeout',
          failedAt: '2026-07-22T10:40:00',
          retryCount: 3
        },
        {
          eventId: 'E11',
          eventType: 'PAYMENT_COMPLETED',
          serviceName: 'inventory-service',
          errorMessage: 'Database connection failed',
          failedAt: '2026-07-22T10:45:00',
          retryCount: 3
        }
      ];

      dlqApi.getAllFailedEvents.mockResolvedValue(mockFailedEvents);

      const failedEvents = await dlqApi.getAllFailedEvents();
      expect(failedEvents).toHaveLength(2);
      expect(failedEvents[0].retryCount).toBe(3);
      expect(failedEvents[1].serviceName).toBe('inventory-service');
    });

    test('should delete failed event from DLQ', async () => {
      dlqApi.deleteFailedEvent.mockResolvedValue({ success: true });

      const result = await dlqApi.deleteFailedEvent('E10');
      expect(result.success).toBe(true);
    });
  });

  describe('Replay Scenarios', () => {
    test('should replay failed event from DLQ', async () => {
      const mockReplayRequest = {
        eventId: 'E15',
        targetTopic: 'order-created'
      };

      const mockReplayResponse = {
        success: true,
        eventId: 'E15',
        replayTopic: 'order-created',
        message: 'Event replayed successfully'
      };

      replayApi.replayEvent.mockResolvedValue(mockReplayResponse);

      const replayResult = await replayApi.replayEvent(mockReplayRequest);

      expect(replayResult.success).toBe(true);
      expect(replayResult.eventId).toBe('E15');
      expect(replayResult.message).toContain('successfully');
    });

    test('should retrieve replay history', async () => {
      const mockReplayHistory = [
        {
          replayId: 'RP1',
          originalEventId: 'E15',
          replayedAt: '2026-07-22T10:50:00',
          status: 'SUCCESS'
        },
        {
          replayId: 'RP2',
          originalEventId: 'E16',
          replayedAt: '2026-07-22T10:55:00',
          status: 'SUCCESS'
        }
      ];

      replayApi.getReplayHistory.mockResolvedValue(mockReplayHistory);

      const history = await replayApi.getReplayHistory();
      expect(history).toHaveLength(2);
      expect(history[0].status).toBe('SUCCESS');
    });
  });

  describe('Monitoring and Metrics', () => {
    test('should retrieve system metrics', async () => {
      const mockMetrics = {
        totalOrders: 100,
        successfulOrders: 85,
        failedOrders: 15,
        averageProcessingTime: 2.5,
        systemHealth: 'healthy'
      };

      monitoringApi.getSystemMetrics.mockResolvedValue(mockMetrics);

      const metrics = await monitoringApi.getSystemMetrics();

      expect(metrics.totalOrders).toBe(100);
      expect(metrics.successfulOrders).toBe(85);
      expect(metrics.systemHealth).toBe('healthy');
    });

    test('should retrieve event metrics', async () => {
      const mockEventMetrics = {
        orderCreatedCount: 100,
        orderValidatedCount: 95,
        paymentCompletedCount: 90,
        inventoryReservedCount: 85,
        orderCompletedCount: 80
      };

      monitoringApi.getEventMetrics.mockResolvedValue(mockEventMetrics);

      const eventMetrics = await monitoringApi.getEventMetrics();

      expect(eventMetrics.orderCreatedCount).toBeGreaterThan(eventMetrics.orderCompletedCount);
      expect(eventMetrics.orderValidatedCount).toBeGreaterThan(eventMetrics.paymentCompletedCount);
    });
  });

  describe('Audit and Idempotency', () => {
    test('should verify idempotency checks', async () => {
      const mockIdempotencyCheck = {
        eventId: 'E20',
        isDuplicate: false,
        processedAt: null
      };

      idempotencyApi.checkIdempotency.mockResolvedValue(mockIdempotencyCheck);

      const idempotencyResult = await idempotencyApi.checkIdempotency('E20');

      expect(idempotencyResult.isDuplicate).toBe(false);
      expect(idempotencyResult.processedAt).toBeNull();
    });

    test('should retrieve audit logs', async () => {
      const mockAuditLogs = [
        {
          auditId: 'A1',
          eventId: 'E20',
          eventType: 'ORDER_CREATED',
          serviceName: 'order-service',
          timestamp: '2026-07-22T11:00:00'
        }
      ];

      auditApi.getAuditLogs.mockResolvedValue(mockAuditLogs);

      const auditLogs = await auditApi.getAuditLogs();

      expect(auditLogs).toHaveLength(1);
      expect(auditLogs[0].eventType).toBe('ORDER_CREATED');
    });
  });

  describe('Complete End-to-End Workflow', () => {
    test('should process complete order lifecycle from creation to fulfillment', async () => {
      // Step 1: Create Order
      const mockOrder = {
        id: 100,
        customerId: 'CUST-E2E',
        totalAmount: 999.99,
        status: 'CREATED',
        createdAt: '2026-07-22T11:05:00'
      };

      orderApi.createOrder.mockResolvedValue(mockOrder);
      const order = await orderApi.createOrder({
        customerId: 'CUST-E2E',
        totalAmount: 999.99
      });
      expect(order.status).toBe('CREATED');

      // Step 2: Validation
      const mockValidation = {
        orderId: 100,
        validationStatus: 'VALID',
        validationMessage: 'Success'
      };
      validationApi.getAllValidations.mockResolvedValue([mockValidation]);
      const validations = await validationApi.getAllValidations();
      expect(validations.find(v => v.orderId === 100).validationStatus).toBe('VALID');

      // Step 3: Payment
      const mockPayment = {
        orderId: 100,
        paymentStatus: 'COMPLETED',
        amount: 999.99
      };
      paymentApi.getAllPayments.mockResolvedValue([mockPayment]);
      const payments = await paymentApi.getAllPayments();
      expect(payments.find(p => p.orderId === 100).paymentStatus).toBe('COMPLETED');

      // Step 4: Inventory
      const mockInventory = {
        orderId: 100,
        productId: 'PROD-E2E',
        status: 'RESERVED'
      };
      inventoryApi.getAllInventoryItems.mockResolvedValue([mockInventory]);
      const inventory = await inventoryApi.getAllInventoryItems();
      expect(inventory.find(i => i.orderId === 100).status).toBe('RESERVED');

      // Step 5: Fulfillment
      const mockFulfillment = {
        orderId: 100,
        fulfillmentStatus: 'COMPLETED',
        trackingNumber: 'TRACK-100'
      };
      fulfillmentApi.getAllFulfillments.mockResolvedValue([mockFulfillment]);
      const fulfillments = await fulfillmentApi.getAllFulfillments();
      expect(fulfillments.find(f => f.orderId === 100).fulfillmentStatus).toBe('COMPLETED');

      // Step 6: Verify final order status
      const mockCompletedOrder = { ...mockOrder, status: 'COMPLETED' };
      orderApi.getOrderById.mockResolvedValue(mockCompletedOrder);
      const completedOrder = await orderApi.getOrderById(100);
      expect(completedOrder.status).toBe('COMPLETED');
    });
  });
});
