package app;

import static spark.Spark.*;
import spark.Session;
import spark.Spark;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import app.model.*;
import app.service.MenuService;
import app.dao.OrderDao;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

        get("/api/menus", (req, res) -> {
            res.type("application/json");
            return gson.toJson(menuService.getAll());
        });

        get("/api/menus/category/:c", (req, res) -> {
            res.type("application/json");
            return gson.toJson(menuService.getByCategory(req.params("c")));
        });

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

        // ==============================
        // ROUTES LOGIN
        // ==============================

        Spark.post("/api/login", (req, res) -> {
            res.type("application/json");
            try {
                // Ambil data dari body request
                Map<String, Object> p = (Map) gson.fromJson(req.body(), (new TypeToken<Map<String, Object>>() {
                }).getType());
                String username = (String) p.get("username");
                String password = (String) p.get("password");

                if ("admin".equals(username) && "12345".equals(password)) {

                    // Otentikasi Berhasil
                    return jsonOk(Map.of("message", "Login successful"));
                } else {
                    // Otentikasi Gagal
                    res.status(401); // Unauthorized
                    return jsonError("Invalid username or password");
                }
            } catch (Exception e) {
                res.status(500);
                return jsonError(e.getMessage());
            }
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
                cart = new ArrayList<>();

            // CARI ITEM DI CART
            Optional<CartItem> exists = cart.stream()
                    .filter(ci -> ci.getItem().getId().equals(id))
                    .findFirst();

            if (exists.isPresent()) {
                // UPDATE / HAPUS
                if (quantity <= 0) {
                    cart.remove(exists.get());
                } else {
                    exists.get().setQuantity(quantity);
                }
            } else {
                // TAMBAH ITEM BARU KE CART
                if (quantity > 0) {
                    MenuItem item = menuService.findById(id); // ← perbaikan
                    if (item != null) {
                        cart.add(new CartItem(item, quantity));
                    } else {
                        return gson.toJson(Map.of("ok", false, "error", "Item not found"));
                    }
                }
            }

            session.attribute("cart", cart);

            return gson.toJson(Map.of("ok", true, "cartSize", cart.size()));
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
            res.type("application/json");

            Map<String, Object> p = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {
            }.getType());
            String table = (String) p.get("tableNumber");
            String pay = (String) p.get("paymentMethod");

            Session session = req.session(true);
            List<CartItem> cart = session.attribute("cart");

            // Tambahan: ambil items dari body jika cart session kosong
            if ((cart == null || cart.isEmpty()) && p.get("items") != null) {
                List<Map<String, Object>> itemsReq = (List<Map<String, Object>>) p.get("items");
                cart = new ArrayList<>();

                for (Map<String, Object> itemReq : itemsReq) {
                    Map<String, Object> objItem = (Map<String, Object>) itemReq.get("item");
                    String id = (String) objItem.get("id");
                    int qty = ((Double) itemReq.get("quantity")).intValue();

                    MenuItem m = menuService.findById(id);
                    if (m != null && qty > 0) {
                        cart.add(new CartItem(m, qty));
                    }
                }

                // simpan cart ke session
                session.attribute("cart", cart);
            }

            // kalau tetap kosong → error
            if (cart == null || cart.isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("ok", false, "error", "Cart empty"));
            }

            // hitung total
            double total = cart.stream().mapToDouble(CartItem::getTotal).sum();

            // buat nomor order
            String orderId = "ORD-" + System.currentTimeMillis();

            Map<String, Object> receipt = new HashMap<>();
            receipt.put("orderId", orderId);
            receipt.put("tableNumber", table);
            receipt.put("paymentMethod", pay);
            receipt.put("items", cart.stream().map(ci -> Map.of(
                    "id", ci.getItem().getId(),
                    "name", ci.getItem().getName(),
                    "price", ci.getItem().getPrice(),
                    "quantity", ci.getQuantity())).toList());
            receipt.put("total", total);
            receipt.put("timestamp", System.currentTimeMillis());

            // checkout sukses → kosongkan session cart
            session.attribute("cart", new ArrayList<>());

            return gson.toJson(Map.of("ok", true, "receipt", receipt));
        });
    }
}