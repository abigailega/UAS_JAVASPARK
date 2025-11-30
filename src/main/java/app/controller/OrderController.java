package app.controller;

import static spark.Spark.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import app.service.OrderService;
import app.service.MenuService;
import app.model.Order;
import app.model.CartItem;
import spark.Session;

import java.util.List;
import java.util.Map;

public class OrderController {

    public static void init(OrderService orderService, MenuService menuService, Gson gson) {

        post("/api/order", (req, res) -> {
            res.type("application/json");

            Map<String, Object> body = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {
            }.getType());

            String name = (String) body.getOrDefault("name", "Customer");
            String tableNumber = String.valueOf(body.getOrDefault("tableNumber", ""));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) body.get("items");

            if (itemsRaw == null || itemsRaw.isEmpty()) {
                Session s = req.session(true);
                @SuppressWarnings("unchecked")
                List<CartItem> cart = s.attribute("cart");
                if (cart == null || cart.isEmpty()) {
                    res.status(400);
                    return gson.toJson(Map.of("ok", false, "error", "Cart empty"));
                }
                Order o = orderService.createFromCart(name, tableNumber, cart);
                cart.clear();
                return gson.toJson(Map.of("ok", true, "order", o));
            } else {
                Order o = orderService.createFromPayload(name, tableNumber, itemsRaw);
                return gson.toJson(Map.of("ok", true, "order", o));
            }
        });

        get("/api/order/:id", (req, res) -> {
            res.type("application/json");
            var o = orderService.findById(req.params("id"));
            if (o == null) {
                res.status(404);
                return gson.toJson(Map.of("ok", false, "error", "Order not found"));
            }
            return gson.toJson(Map.of("ok", true, "order", o));
        });

        get("/api/orders", (req, res) -> {
            res.type("application/json");
            return gson.toJson(orderService.getAll());
        });
    }
}
