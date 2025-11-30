package app.service;

import app.model.CartItem;
import app.model.Order;
import app.model.OrderItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class OrderService {

    private final Map<String, Order> store = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(1000);

    public Order createFromCart(String customerName, String tableNumber, List<CartItem> cart) {
        if (cart == null || cart.isEmpty())
            return null;

        List<OrderItem> items = cart.stream()
                .map(ci -> new OrderItem(
                        ci.getItem().getId(),
                        ci.getItem().getName(),
                        ci.getQuantity(),
                        ci.getItem().getPrice()))
                .collect(Collectors.toList());

        double total = items.stream().mapToDouble(OrderItem::getTotal).sum();
        String id = String.valueOf(seq.getAndIncrement());
        Order o = new Order(id, customerName, tableNumber, items, total, System.currentTimeMillis(), "NEW");

        store.put(id, o);
        return o;
    }

    @SuppressWarnings("unchecked")
    public Order createFromPayload(String customerName, String tableNumber, List<Map<String, Object>> itemsRaw) {
        if (itemsRaw == null || itemsRaw.isEmpty())
            return null;

        List<OrderItem> items = new ArrayList<>();
        for (Map<String, Object> m : itemsRaw) {
            String id = String.valueOf(m.get("id"));
            String name = String.valueOf(m.getOrDefault("name", "Item"));
            int qty = ((Number) m.getOrDefault("quantity", 1)).intValue();
            double price = ((Number) m.getOrDefault("price", 0)).doubleValue();
            items.add(new OrderItem(id, name, qty, price));
        }

        double total = items.stream().mapToDouble(OrderItem::getTotal).sum();
        String id = String.valueOf(seq.getAndIncrement());
        Order o = new Order(id, customerName, tableNumber, items, total, System.currentTimeMillis(), "NEW");

        store.put(id, o);
        return o;
    }

    public Order findById(String id) {
        return store.get(id);
    }

    public List<Order> getAll() {
        return new ArrayList<>(store.values());
    }

    public boolean updateStatus(String id, String status) {
        Order o = store.get(id);
        if (o != null) {
            o.setStatus(status);
            return true;
        }
        return false;
    }

    public boolean delete(String id) {
        return store.remove(id) != null;
    }
}
