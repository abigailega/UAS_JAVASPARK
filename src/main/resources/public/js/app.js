async function fetchMenus(category = null) {
    let url = '/api/menus';
    if (category && category !== "All") {
        url = '/api/menus/category/' + category;
    }
    const res = await fetch(url);
    return res.json();
}

function createCard(item) {
    const div = document.createElement('div');
    div.className = 'card';
    div.innerHTML = `
        <img src="${item.image || '/images/placeholder.png'}" alt="${item.name}">
        <h4>${item.name}</h4>
        <p>${item.description}</p>
        <p>Rp ${item.price.toLocaleString('id-ID')}</p>
        <div class="actions">
            <button class="add-btn" data-id="${item.id}">Tambah</button>
        </div>
    `;
    return div;
}

async function refreshMenu(category = null) {
    const menus = await fetchMenus(category);
    const menuGrid = document.getElementById('menuGrid');
    menuGrid.innerHTML = '';

    menus.forEach(m => menuGrid.appendChild(createCard(m)));
}

async function fetchCart() {
    const res = await fetch('/api/cart');
    return res.json();
}

async function updateCartPreview() {
    try {
        const data = await fetchCart();
        const cartCount = document.getElementById('cartCount');
        if (cartCount) cartCount.innerText = data.totalQty;

        updateFloatingCart(data.items);
    } catch (err) {
        console.error('Gagal fetch cart:', err);
    }
}

function updateFloatingCart(cartItems) {
    const container = document.getElementById('floatingCartContent');
    if (!container) return;

    if (!cartItems || cartItems.length === 0) {
        container.innerHTML = '<p>Cart kosong</p>';
    } else {
        let html = '<ul>';
        cartItems.forEach(ci => {
            const name = ci.item ? ci.item.name : ci.name;
            const price = ci.item ? ci.item.price : ci.price;
            html += `<li>${name} x ${ci.quantity} - Rp ${price.toLocaleString('id-ID')}</li>`;
        });
        html += '</ul>';
        container.innerHTML = html;
    }
}

async function addToCart(menuId, quantity = 1) {
    try {
        await fetch('/api/cart/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: menuId, quantity })
        });
        await updateCartPreview();
    } catch (err) {
        console.error('Gagal menambahkan ke cart:', err);
    }
}

document.addEventListener('click', function(e) {
    if (e.target.classList.contains('add-btn')) {
        const menuId = e.target.dataset.id;
        addToCart(menuId, 1);
    }
});

document.querySelectorAll('.tab').forEach(tabBtn => {
    tabBtn.addEventListener('click', () => {
        const cat = tabBtn.dataset.cat;
        refreshMenu(cat);
    });
});

document.getElementById('btnLogin').addEventListener('click', () => window.location.href = '/login.html');

document.getElementById('confirmFloatingCart').addEventListener('click', () => {
    window.location.href = '/cart.html';
});

refreshMenu().then(updateCartPreview);