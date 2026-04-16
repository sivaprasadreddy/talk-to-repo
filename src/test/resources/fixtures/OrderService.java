package com.example.orders;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing customer orders.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Creates a new order for the given customer.
     */
    @Transactional
    public Order createOrder(String customerId, List<String> items) {
        Order order = new Order(customerId, items);
        return orderRepository.save(order);
    }

    /**
     * Cancels an existing order.
     */
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.cancel();
        orderRepository.save(order);
    }

    public Optional<Order> findById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> findByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
}
