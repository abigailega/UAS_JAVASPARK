async function loadCart() {
    const res = await fetch('/api/cart');
    const data = await res.json();
    const tbody = document.getElementById('cartItems');
    tbody.innerHTML = '';

    if (!data.items) return;

    data.items.forEach(it => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${it.name}</td>
            <td>Rp ${it.price.toLocaleString('id-ID')}</td>
            <td><input type="number" min="0" value="${it.quantity}" data-id="${it.id}" class="qty"></td>
            <td>Rp ${(it.price * it.quantity).toLocaleString('id-ID')}</td>
            <td><button data-id="${it.id}" class="remove">Hapus</button></td>
        `;
        tbody.appendChild(tr);
    });

    document.getElementById('cartTotals').innerText =
        `Total: Rp ${data.total.toLocaleString('id-ID')}`;

    document.querySelectorAll('.qty').forEach(input => {
        input.onchange = async e => {
            const id = e.target.dataset.id;
            const quantity = Number(e.target.value);

            await fetch('/api/cart/update', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ id, quantity })
            });

            loadCart();
        };
    });

    document.getElementById("btnBack").addEventListener("click", function () {
        window.location.href = "/index.html";
    });

    document.querySelectorAll('.remove').forEach(btn => {
        btn.onclick = async e => {
            const id = e.target.dataset.id;

            await fetch('/api/cart/remove', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ id })
            });

            loadCart();
        };
    });
}

function openQRPopup() {
    const popup = window.open(
        "",
        "QRIS",
        "width=400,height=520,top=150,left=500"
    );

    popup.document.write(`
        <html>
        <head>
            <title>QRIS Pembayaran</title>
            <style>
                body {
                    margin:0;
                    padding:20px;
                    font-family: Arial;
                    display:flex;
                    flex-direction:column;
                    justify-content:center;
                    align-items:center;
                    background:#f5f5f5;
                }
                .box {
                    background:white;
                    padding:20px;
                    border-radius:12px;
                    text-align:center;
                    box-shadow:0 0 12px rgba(0,0,0,0.2);
                }
                img {
                    width:260px;
                    border-radius:10px;
                }
                button {
                    margin-top:15px;
                    padding:8px 20px;
                    background:#333;
                    color:white;
                    border:none;
                    border-radius:6px;
                    cursor:pointer;
                }
            </style>
        </head>
        <body>
            <div class="box">
                <h3>Scan QRIS</h3>
                <img src="/images/qr.jpg">
                <button onclick="window.close()">Selesai</button>
            </div>
        </body>
        </html>
    `);

    return popup;
}

function printReceipt(j, tableNo, pay) {
    const items = j.receipt.items || [];

    let subtotal = 0;
    items.forEach(it => subtotal += it.price * it.quantity);

    const tax = Math.round(subtotal * 0.11);
    const total = subtotal + tax;

    const win = window.open('', '_blank');
    const toRp = n => 'Rp ' + Number(n).toLocaleString('id-ID');

    let itemsHtml = "";
    items.forEach(it => {
        itemsHtml += `
            <tr>
                <td class="qty">${it.quantity}x</td>
                <td class="name">${it.name}</td>
                <td class="total">${toRp(it.price * it.quantity)}</td>
            </tr>
        `;
    });

    let styles = `
        <style>
            body { font-family: Arial; padding:20px; }
            .container{ max-width:600px; margin:0 auto; }
            .center{text-align:center}
            .header{border-bottom:2px solid #000;padding-bottom:10px;margin-bottom:10px}
            .store{font-weight:700;font-size:20px}
            .meta{font-size:12px;margin-top:5px;color:#444}
            .info{margin-top:10px;font-size:14px}
            table{width:100%;margin-top:10px;font-size:14px}
            .summary{border-top:2px solid #000;margin-top:10px;padding-top:10px}
            .row{display:flex;justify-content:space-between;margin:5px 0}
            .big{font-size:18px;font-weight:bold}
            .footer{text-align:center;margin-top:20px;font-size:12px}
            @media print { .no-print{display:none} }
        </style>
    `;

    win.document.write(`
        <html>
            <head><meta charset="utf-8">${styles}</head>
            <body>
                <div class="container">
                    <div class="header center">
                        <div class="store">Warung Makan Nusantara</div>
                        <div class="meta">Jl. Contoh No.10 - Tel: 0812-3456-7890</div>
                    </div>

                    <div style="display:flex;justify-content:space-between">
                        <div><b>Nota:</b> ${j.receipt.orderId}</div>
                        <div>${new Date().toLocaleString('id-ID')}</div>
                    </div>

                    <div class="info">
                        <div><b>Nomor Meja:</b> ${tableNo}</div>
                        <div><b>Metode Pembayaran:</b> ${pay.toUpperCase()}</div>
                    </div>

                    <table><tbody>${itemsHtml}</tbody></table>

                    <div class="summary">
                        <div class="row"><div>Subtotal</div><div>${toRp(subtotal)}</div></div>
                        <div class="row"><div>Pajak 11%</div><div>${toRp(tax)}</div></div>
                        <div class="row big"><div>Total</div><div>${toRp(total)}</div></div>
                    </div>

                    <div class="footer">Terima kasih!<br>Bukti pembayaran sah.</div>
                </div>

                <script>
                    window.onload = () => window.print();
                </script>
            </body>
        </html>
    `);

    win.document.close();
}

document.getElementById('btnCheckout').onclick = async () => {
    const tableNo = document.getElementById('tableNo').value;
    const pay = document.querySelector('input[name=pay]:checked').value;

    const res = await fetch('/api/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            paymentMethod: pay,
            tableNumber: tableNo
        })
    });

    const j = await res.json();

    if (!j.ok) {
        alert('Checkout gagal!');
        return;
    }

    if (pay === 'qris') {
        const popup = openQRPopup();
        const check = setInterval(() => {
            if (popup.closed) {
                clearInterval(check);
                printReceipt(j, tableNo, pay);
            }
        }, 500);

    } else {
        printReceipt(j, tableNo, pay);
    }

    loadCart();
};

loadCart(); 