package app.controller;

import static spark.Spark.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import app.service.MenuService;
import app.model.CartItem;
import spark.Session;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class CartController {

    public static void init(MenuService menuService, Gson gson) {

        // Add item to session cart
        post("/api/cart/add", (req, res) -> {
            res.type("application/json");
            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {
            }.getType());
            String id = (String) p.get("id");
            int qty = ((Number) p.getOrDefault("quantity", 1)).intValue();

            Session s = req.session(true);
            List<CartItem> cart = s.attribute("cart");
            if (cart == null) {
                cart = new ArrayList<>();
                s.attribute("cart", cart);
            }

            // find menu item
            var menuItem = menuService.findById(id);
            if (menuItem == null) {
                res.status(404);
                return gson.toJson(Map.of("ok", false, "error", "Item not found"));
            }

            // add or update
            var existing = cart.stream().filter(ci -> ci.getItem().getId().equals(id)).findFirst();
            if (existing.isPresent()) {
                existing.get().setQuantity(existing.get().getQuantity() + qty);
            } else {
                cart.add(new CartItem(menuItem, qty));
            }

            return gson.toJson(Map.of("ok", true));
        });

        // Get cart from session
        get("/api/cart", (req, res) -> {
            res.type("application/json");
            Session s = req.session(true);
            List<CartItem> cart = s.attribute("cart");
            if (cart == null)
                cart = new ArrayList<>();
            double total = cart.stream().mapToDouble(CartItem::getTotal).sum();
            int totalQty = cart.stream().mapToInt(CartItem::getQuantity).sum();
            return gson.toJson(Map.of("items", cart, "total", total, "totalQty", totalQty));
        });

        // Update quantity
        post("/api/cart/update", (req, res) -> {
            res.type("application/json");
            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {
            }.getType());
            String id = (String) p.get("id");
            int qty = ((Number) p.getOrDefault("quantity", 0)).intValue();

            Session s = req.session(true);
            List<CartItem> cart = s.attribute("cart");
            if (cart == null)
                return gson.toJson(Map.of("ok", false));

            cart.stream().filter(ci -> ci.getItem().getId().equals(id)).findFirst()
                    .ifPresent(ci -> {
                        if (qty <= 0)
                            cart.remove(ci);
                        else
                            ci.setQuantity(qty);
                    });

            return gson.toJson(Map.of("ok", true));
        });

        // Remove item
        post("/api/cart/remove", (req, res) -> {
            res.type("application/json");
            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {
            }.getType());
            String id = (String) p.get("id");

            Session s = req.session(true);
            List<CartItem> cart = s.attribute("cart");
            if (cart != null)
                cart.removeIf(ci -> ci.getItem().getId().equals(id));

            return gson.toJson(Map.of("ok", true));
        });
    }
}