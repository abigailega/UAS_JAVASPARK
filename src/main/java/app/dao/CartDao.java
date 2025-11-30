package app.dao;

import app.model.CartItem;
import app.model.MenuItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import spark.Session;

public class CartDao {

    public CartDao() {
    }

    public List<CartItem> getCart(Session session) {
        List<CartItem> cart = session.attribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.attribute("cart", cart);
        }
        return cart;
    }

    public void addToCart(Session session, MenuItem menuItem, int quantity) {
        List<CartItem> cart = getCart(session);

        Optional<CartItem> existing = cart.stream()
                .filter(ci -> ci.getItem().getId().equals(menuItem.getId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            cart.add(new CartItem(menuItem, quantity));
        }
    }

    public void updateCart(Session session, MenuItem menuItem, int quantity) {
        List<CartItem> cart = getCart(session);
        Optional<CartItem> existing = cart.stream()
                .filter(ci -> ci.getItem().getId().equals(menuItem.getId()))
                .findFirst();

        existing.ifPresent(ci -> {
            if (quantity <= 0) {
                cart.remove(ci);
            } else {
                ci.setQuantity(quantity);
            }
        });
    }

    public void removeFromCart(Session session, MenuItem menuItem) {
        List<CartItem> cart = getCart(session);
        cart.removeIf(ci -> ci.getItem().getId().equals(menuItem.getId()));
    }

    public double getTotal(Session session) {
        List<CartItem> cart = getCart(session);
        return cart.stream().mapToDouble(CartItem::getTotal).sum();
    }

    public int getTotalQty(Session session) {
        List<CartItem> cart = getCart(session);
        return cart.stream().mapToInt(CartItem::getQuantity).sum();
    }

}