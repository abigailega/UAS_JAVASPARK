package app.service;

import app.dao.MenuDao;
import app.model.MenuItem;

import java.util.List;
import java.util.stream.Collectors;

public class MenuService {

    private MenuDao menuDao;

    public MenuService() {
        this.menuDao = new MenuDao();
    }

    public List<MenuItem> getAll() {
        return menuDao.getAll();
    }

    public MenuItem findById(String id) {
        return menuDao.findById(id);
    }

    public List<MenuItem> getByCategory(String category) {
        return menuDao.getAll().stream()
                .filter(m -> m.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public boolean exists(String id) {
        return menuDao.findById(id) != null;
    }

    public double getPrice(String id) {
        MenuItem item = menuDao.findById(id);
        return (item != null) ? item.getPrice() : 0;
    }

    public String getName(String id) {
        MenuItem item = menuDao.findById(id);
        return (item != null) ? item.getName() : "";
    }

    public String getImage(String id) {
        MenuItem item = menuDao.findById(id);
        return (item != null) ? item.getImage() : "";
    }

    public boolean addMenu(MenuItem item) {
        if (exists(item.getId())) {
            return false; 
        }
        return menuDao.add(item);
    }
}
