package app;

import static spark.Spark.*;
import spark.Session;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import app.model.*;
import app.service.MenuService;

import java.util.*;

public class Main {
    static Gson gson = new Gson();

    public static void main(String[] args) {

        port(8080);
        staticFiles.location("/public"); 

        MenuService menuService = new MenuService();

        get("/api/menus", (req, res) -> {
            res.type("application/json");
            return gson.toJson(menuService.getAll());
        });

        get("/api/menus/category/:c", (req, res) -> {
            res.type("application/json");
            return gson.toJson(menuService.getByCategory(req.params("c")));
        });

        post("/api/login", (req, res) -> {
            res.type("application/json");

            Map<String, Object> body = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
            String username = (String) body.get("username");
            String password = (String) body.get("password");

            if (!"".equals(username) || !"".equals(password)) {
                return gson.toJson(Map.of("ok", false, "msg", "Username atau password salah"));
            }

            Session session = req.session(true);
            session.attribute("user", new User(username, "Administrator"));
            session.attribute("cart", new ArrayList<CartItem>());

            return gson.toJson(Map.of("ok", true));
        });

        post("/api/cart/add", (req, res) -> {
            res.type("application/json");

            Map<String, Object> payload = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
            String id = (String) payload.get("id");
            Double qd = (Double) payload.getOrDefault("quantity", 1.0);
            int quantity = qd.intValue();

            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");
            if (cart == null) {
                cart = new ArrayList<>();
                session.attribute("cart", cart);
            }

            MenuItem item = menuService.findById(id);
            if (item == null) {
                res.status(404);
                return gson.toJson(Map.of("ok", false, "error", "Item not found"));
            }

            Optional<CartItem> existing = cart.stream().filter(ci -> ci.getItem().getId().equals(id)).findFirst();
            if (existing.isPresent()) {
                CartItem ci = existing.get();
                ci.setQuantity(ci.getQuantity() + quantity);
            } else {
                cart.add(new CartItem(item, quantity));
            }

            return gson.toJson(Map.of("ok", true));
        });

        get("/api/cart", (req, res) -> {
            res.type("application/json");

            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");
            if (cart == null) cart = new ArrayList<>();

            List<Map<String,Object>> out = new ArrayList<>();
            double total = 0;
            int totalQty = 0;

            for (CartItem ci : cart) {
                out.add(Map.of(
                    "id", ci.getItem().getId(),
                    "name", ci.getItem().getName(),
                    "price", ci.getItem().getPrice(),
                    "quantity", ci.getQuantity(),
                    "total", ci.getTotal()
                ));
                total += ci.getTotal();
                totalQty += ci.getQuantity();
            }

            return gson.toJson(Map.of("items", out, "total", total, "totalQty", totalQty));
        });

        post("/api/cart/update", (req, res) -> {
            res.type("application/json");
            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());

            String id = (String) p.get("id");
            Double qd = (Double) p.get("quantity");
            int q = qd.intValue();

            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");
            if (cart == null) return gson.toJson(Map.of("ok", false));

            cart.stream().filter(ci -> ci.getItem().getId().equals(id)).findFirst()
                .ifPresent(ci -> {
                    if (q <= 0) cart.remove(ci);
                    else ci.setQuantity(q);
                });

            return gson.toJson(Map.of("ok", true));
        });

        post("/api/cart/remove", (req, res) -> {
            res.type("application/json");
            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
            String id = (String) p.get("id");

            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");
            if (cart != null) cart.removeIf(ci -> ci.getItem().getId().equals(id));

            return gson.toJson(Map.of("ok", true));
        });

        post("/api/checkout", (req, res) -> {
            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());

            String paymentMethod = (String) p.get("paymentMethod");
            String tableNumber = (String) p.getOrDefault("tableNumber", "");

            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");

            if (cart == null || cart.isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("ok", false, "error", "Cart empty"));
            }

            double total = cart.stream().mapToDouble(CartItem::getTotal).sum();

            Map<String,Object> receipt = Map.of(
                    "items", cart.stream().map(ci -> Map.of(
                            "name", ci.getItem().getName(),
                            "quantity", ci.getQuantity(),
                            "price", ci.getItem().getPrice(),
                            "total", ci.getTotal()
                    )).toArray(),
                    "total", total,
                    "paymentMethod", paymentMethod,
                    "tableNumber", tableNumber,
                    "timestamp", System.currentTimeMillis()
            );

            cart.clear();
            return gson.toJson(Map.of("ok", true, "receipt", receipt));
        });
    }
}
