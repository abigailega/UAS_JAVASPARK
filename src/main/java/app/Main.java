package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import app.dao.OrderDao;
import app.model.CartItem;
import app.model.MenuItem;
import app.model.OrderItem;
import app.service.MenuService;
import spark.Session;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

public class Main {
    static Gson gson = new Gson();

    public static String jsonOk(Object obj) {
        return gson.toJson(Map.of("ok", true, "data", obj));
    }

    public static String jsonError(String msg) {
        return gson.toJson(Map.of("ok", false, "error", msg));
    }

    public static void main(String[] args) {

        port(8080);
        staticFiles.location("/public");

        options("/*", (req, res) -> {
            String headers = req.headers("Access-Control-Request-Headers");
            if (headers != null)
                res.header("Access-Control-Allow-Headers", headers);
            String methods = req.headers("Access-Control-Request-Method");
            if (methods != null)
                res.header("Access-Control-Allow-Methods", methods);
            return "OK";
        });

        before((req, res) -> res.header("Access-Control-Allow-Origin", "*"));

        exception(Exception.class, (e, req, res) -> {
            res.type("application/json");
            res.status(500);
            res.body(jsonError(e.getMessage()));
        });

        MenuService menuService = new MenuService();
        OrderDao orderDao = new OrderDao();

        // ==============================
        // ROUTES MENUS
        // ==============================

        get("/api/menus", (req, res) -> {
            res.type("application/json");
            return gson.toJson(menuService.getAll());
        });

        get("/api/menus/category/:c", (req, res) -> {
            res.type("application/json");
            return gson.toJson(menuService.getByCategory(req.params("c")));
        });

        // ==============================
        // ROUTE TAMBAH MENU
        // ==============================
        post("/api/menus/add", (req, res) -> {
            res.type("application/json");
            try {
                MenuItem newItem = gson.fromJson(req.body(), MenuItem.class);
                boolean saved = menuService.addMenu(newItem);
                if (!saved) {
                    res.status(400);
                    return gson.toJson(Map.of("ok", false, "error", "ID sudah ada atau gagal disimpan"));
                }
                return gson.toJson(Map.of("ok", true));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("ok", false, "error", e.getMessage()));
            }
        });

        // ==============================
        // ROUTES CART
        // ==============================
        post("/api/cart/add", (req, res) -> {
            res.type("application/json");
            Map<String, Object> payload = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {
            }.getType());
            String id = (String) payload.get("id");
            int quantity = ((Double) payload.getOrDefault("quantity", 1.0)).intValue();

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
                existing.get().setQuantity(existing.get().getQuantity() + quantity);
            } else {
                cart.add(new CartItem(item, quantity));
            }

            return gson.toJson(Map.of("ok", true));
        });

        get("/api/cart", (req, res) -> {
            res.type("application/json");
            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");
            if (cart == null)
                cart = new ArrayList<>();

            List<Map<String, Object>> out = new ArrayList<>();
            double total = 0;
            int totalQty = 0;

            for (CartItem ci : cart) {
                out.add(Map.of(
                        "id", ci.getItem().getId(),
                        "name", ci.getItem().getName(),
                        "price", ci.getItem().getPrice(),
                        "quantity", ci.getQuantity(),
                        "total", ci.getTotal()));
                total += ci.getTotal();
                totalQty += ci.getQuantity();
            }

            return gson.toJson(Map.of("items", out, "total", total, "totalQty", totalQty));
        });

        post("/api/cart/update", (req, res) -> {
            res.type("application/json");
            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {
            }.getType());

            String id = (String) p.get("id");
            int quantity = ((Double) p.get("quantity")).intValue();

            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");
            if (cart == null)
                return gson.toJson(Map.of("ok", false));

            cart.stream().filter(ci -> ci.getItem().getId().equals(id)).findFirst()
                    .ifPresent(ci -> {
                        if (quantity <= 0)
                            cart.remove(ci);
                        else
                            ci.setQuantity(quantity);
                    });

            return gson.toJson(Map.of("ok", true));
        });

        post("/api/cart/remove", (req, res) -> {
            res.type("application/json");
            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {
            }.getType());
            String id = (String) p.get("id");

            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");
            if (cart != null)
                cart.removeIf(ci -> ci.getItem().getId().equals(id));

            return gson.toJson(Map.of("ok", true));
        });

        post("/api/checkout", (req, res) -> {
            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {
            }.getType());

            String paymentMethod = (String) p.get("paymentMethod");
            String tableNumber = (String) p.getOrDefault("tableNumber", "");
String serviceType   = (String) p.getOrDefault("serviceType", "dine-in");
            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");

            if (cart == null || cart.isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("ok", false, "error", "Cart empty"));
            }

            double total = cart.stream().mapToDouble(CartItem::getTotal).sum();
            String orderId = "ORD-" + System.currentTimeMillis();

            List<OrderItem> orderItems = cart.stream()
                    .map(ci -> new OrderItem(
                            ci.getItem().getId(),
                            ci.getItem().getName(),
                            ci.getQuantity(),
                            ci.getItem().getPrice()))
                    .collect(Collectors.toList());

            boolean saved = orderDao.saveOrder(orderId, "Guest", tableNumber, cart);
            cart.clear();

            Map<String, Object> receipt = Map.of(
                    "items", orderItems.stream().map(oi -> Map.of(
                            "name", oi.getName(),
                            "quantity", oi.getQuantity(),
                            "price", oi.getPrice(),
                            "total", oi.getTotal())).toArray(),
                    "total", total,
                    "paymentMethod", paymentMethod,
                    "tableNumber", tableNumber,"serviceType", serviceType,
                    "timestamp", System.currentTimeMillis());

            return gson.toJson(Map.of("ok", saved, "receipt", receipt));
        });

    }
}
