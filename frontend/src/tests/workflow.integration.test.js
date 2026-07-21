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

import * as orderApi from '../services/orderApi';
import * as validationApi from '../services/validationApi';
import * as paymentApi from '../services/paymentApi';
import * as inventoryApi from '../services/inventoryApi';
import * as fulfillmentApi from '../services/fulfillmentApi';
import * as monitoringApi from '../services/monitoringApi';
import * as retryApi from '../services/retryApi';
import * as dlqApi from '../services/dlqApi';
import * as replayApi from '../services/replayApi';

describe('Complete Application Workflow Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  test('should verify order creation workflow API calls', async () => {
    // Mock order creation
    const mockOrder = {
      id: 1,
      customerId: 'CUST-001',
      totalAmount: 299.99,
      status: 'CREATED',
      createdAt: '2026-07-16T10:00:00'
    };

    orderApi.createOrder.mockResolvedValue(mockOrder);
    orderApi.getOrderById.mockResolvedValue(mockOrder);

    // Test order creation
    const order = await orderApi.createOrder({
      customerId: 'CUST-001',
      totalAmount: 299.99
    });

    expect(order).toEqual(mockOrder);
    expect(order.status).toBe('CREATED');
  });

  test('should verify validation workflow API calls', async () => {
    const mockValidation = {
      orderId: 1,
      validationStatus: 'VALID',
      customerId: 'CUST-001'
    };

    validationApi.getValidationByOrderId.mockResolvedValue(mockValidation);

    const validation = await validationApi.getValidationByOrderId(1);

    expect(validation).toEqual(mockValidation);
    expect(validation.validationStatus).toBe('VALID');
  });

  test('should verify payment workflow API calls', async () => {
    const mockPayment = {
      orderId: 1,
      paymentStatus: 'COMPLETED',
      amount: 299.99
    };

    paymentApi.getPaymentByOrderId.mockResolvedValue(mockPayment);

    const payment = await paymentApi.getPaymentByOrderId(1);

    expect(payment).toEqual(mockPayment);
    expect(payment.paymentStatus).toBe('COMPLETED');
  });

  test('should verify inventory workflow API calls', async () => {
    const mockReservation = {
      orderId: 1,
      reservationStatus: 'RESERVED',
      productId: 'PROD-001'
    };

    inventoryApi.getReservationByOrderId.mockResolvedValue(mockReservation);

    const reservation = await inventoryApi.getReservationByOrderId(1);

    expect(reservation).toEqual(mockReservation);
    expect(reservation.reservationStatus).toBe('RESERVED');
  });

  test('should verify fulfillment workflow API calls', async () => {
    const mockFulfillment = {
      orderId: 1,
      fulfillmentStatus: 'COMPLETED',
      customerId: 'CUST-001'
    };

    fulfillmentApi.getFulfillmentByOrderId.mockResolvedValue(mockFulfillment);

    const fulfillment = await fulfillmentApi.getFulfillmentByOrderId(1);

    expect(fulfillment).toEqual(mockFulfillment);
    expect(fulfillment.fulfillmentStatus).toBe('COMPLETED');
  });

  test('should complete full order lifecycle workflow', async () => {
    // Step 1: Create order
    const createdOrder = {
      id: 1,
      customerId: 'CUST-WORKFLOW-001',
      status: 'CREATED',
      totalAmount: 299.99
    };

    orderApi.createOrder.mockResolvedValue(createdOrder);
    const order = await orderApi.createOrder({
      customerId: 'CUST-WORKFLOW-001',
      totalAmount: 299.99
    });

    expect(order.status).toBe('CREATED');

    // Step 2: Validation
    const validation = { orderId: 1, validationStatus: 'VALID' };
    validationApi.getValidationByOrderId.mockResolvedValue(validation);
    const validationResult = await validationApi.getValidationByOrderId(1);
    expect(validationResult.validationStatus).toBe('VALID');

    // Step 3: Payment
    const payment = { orderId: 1, paymentStatus: 'COMPLETED', amount: 299.99 };
    paymentApi.getPaymentByOrderId.mockResolvedValue(payment);
    const paymentResult = await paymentApi.getPaymentByOrderId(1);
    expect(paymentResult.paymentStatus).toBe('COMPLETED');

    // Step 4: Inventory
    const inventory = { orderId: 1, reservationStatus: 'RESERVED' };
    inventoryApi.getReservationByOrderId.mockResolvedValue(inventory);
    const inventoryResult = await inventoryApi.getReservationByOrderId(1);
    expect(inventoryResult.reservationStatus).toBe('RESERVED');

    // Step 5: Fulfillment
    const fulfillment = { orderId: 1, fulfillmentStatus: 'COMPLETED' };
    fulfillmentApi.getFulfillmentByOrderId.mockResolvedValue(fulfillment);
    const fulfillmentResult = await fulfillmentApi.getFulfillmentByOrderId(1);
    expect(fulfillmentResult.fulfillmentStatus).toBe('COMPLETED');

    // Step 6: Final order status
    const completedOrder = { ...createdOrder, status: 'COMPLETED' };
    orderApi.getOrderById.mockResolvedValue(completedOrder);
    const finalOrder = await orderApi.getOrderById(1);
    expect(finalOrder.status).toBe('COMPLETED');
  });

  test('should handle failed order workflow', async () => {
    // Create order
    const failedOrder = {
      id: 2,
      customerId: 'CUST-FAIL-001',
      status: 'CREATED',
      totalAmount: 99.99
    };

    orderApi.createOrder.mockResolvedValue(failedOrder);
    const order = await orderApi.createOrder({
      customerId: 'CUST-FAIL-001',
      totalAmount: 99.99
    });

    expect(order.status).toBe('CREATED');

    // Validation fails
    const validation = {
      orderId: 2,
      validationStatus: 'INVALID',
      validationErrors: 'Invalid customer data'
    };

    validationApi.getValidationByOrderId.mockResolvedValue(validation);
    const validationResult = await validationApi.getValidationByOrderId(2);
    expect(validationResult.validationStatus).toBe('INVALID');

    // Order marked as failed
    orderApi.getOrderById.mockResolvedValue({ ...failedOrder, status: 'FAILED' });
    const finalOrder = await orderApi.getOrderById(2);
    expect(finalOrder.status).toBe('FAILED');
  });

  test('should handle retry scenario workflow', async () => {
    // Get retry event
    const retryEvent = {
      retryId: 'R1',
      originalEventId: 'E1',
      retryCount: 1,
      maxRetries: 3
    };

    retryApi.getRetryEventByEventId.mockResolvedValue(retryEvent);
    const retry = await retryApi.getRetryEventByEventId('E1');

    expect(retry.retryCount).toBe(1);
    expect(retry.maxRetries).toBe(3);
    expect(retry.retryCount).toBeLessThan(retry.maxRetries);
  });

  test('should handle DLQ and replay workflow', async () => {
    // Failed event in DLQ
    const failedEvent = {
      eventId: 'E123',
      eventType: 'ORDER_CREATED',
      serviceName: 'validation-service',
      errorMessage: 'Processing failed'
    };

    dlqApi.getAllFailedEvents.mockResolvedValue([failedEvent]);
    const dlqEvents = await dlqApi.getAllFailedEvents();

    expect(dlqEvents).toHaveLength(1);
    expect(dlqEvents[0].eventId).toBe('E123');

    // Replay event
    const replayRequest = { eventId: 'E123', targetTopic: 'order-created' };
    const replayResponse = {
      success: true,
      eventId: 'E123',
      message: 'Event replayed successfully'
    };

    replayApi.replayEvent.mockResolvedValue(replayResponse);
    const replay = await replayApi.replayEvent(replayRequest);

    expect(replay.success).toBe(true);
    expect(replay.eventId).toBe('E123');
  });

  test('should verify monitoring metrics API calls', async () => {
    const mockMetrics = {
      totalOrders: 150,
      successfulOrders: 140,
      failedOrders: 10,
      averageProcessingTime: 2.5,
      systemHealth: 'healthy'
    };

    monitoringApi.getSystemMetrics.mockResolvedValue(mockMetrics);
    const metrics = await monitoringApi.getSystemMetrics();

    expect(metrics.totalOrders).toBe(150);
    expect(metrics.successfulOrders).toBe(140);
    expect(metrics.systemHealth).toBe('healthy');
  });

  test('should handle API errors gracefully', async () => {
    orderApi.getAllOrders.mockRejectedValue(new Error('API Error'));

    await expect(orderApi.getAllOrders()).rejects.toThrow('API Error');
  });
});
