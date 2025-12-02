document.addEventListener("DOMContentLoaded", () => {

    async function loadCart() {
        const res = await fetch('/api/cart');
        const data = await res.json();
        const tbody = document.getElementById('cartItems');
        tbody.innerHTML = '';

        if (!data.items || data.items.length === 0) {
            document.getElementById('cartTotals').innerText = "Total: Rp 0";
            return;
        }

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
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id, quantity })
                });

                loadCart();
            };
        });

        document.querySelectorAll('.remove').forEach(btn => {
            btn.onclick = async e => {
                const id = e.target.dataset.id;

                await fetch('/api/cart/remove', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id })
                });

                loadCart();
            };
        });
    }

    function openQRPopup() {
        const popup = window.open("", "QRIS", "width=400,height=520,top=150,left=500");
        popup.document.write(`
            <html>
            <head><title>QRIS Pembayaran</title></head>
            <body style="text-align:center; font-family:Arial; background:#f5f5f5;">
                <h3>Scan QRIS</h3>
                <img src="/images/qr.jpg" width="260" style="border-radius:10px;">
                <button onclick="window.close()" style="margin-top:10px;padding:8px 18px;">Selesai</button>
            </body>
            </html>
        `);
        return popup;
    }

    function printReceipt(j, tableNo, pay) {
        const win = window.open('', '_blank');
        const items = j.receipt.items || [];
        let subtotal = 0;
        items.forEach(it => subtotal += it.price * it.quantity);

        const tax = Math.round(subtotal * 0.11);
        const total = subtotal + tax;
        const toRp = n => 'Rp ' + Number(n).toLocaleString('id-ID');

        let itemsHtml = "";
        items.forEach(it => {
            itemsHtml += `
                <tr>
                    <td>${it.quantity}x</td>
                    <td>${it.name}</td>
                    <td>${toRp(it.price * it.quantity)}</td>
                </tr>`;
        });

        win.document.write(`
            <html>
<head>
<meta charset="UTF-8">
<style>
    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
    .wrapper { max-width: 500px; margin: auto; }
    .title { text-align: center; font-size: 22px; font-weight: bold; }
    .subtitle { text-align: center; font-size: 13px; margin-top: 4px; color: #444; }
    .divider { border-top: 2px dashed #888; margin: 12px 0; }
    .info { font-size: 14px; line-height: 18px; }
    table { width: 100%; font-size: 15px; margin-top: 10px; border-collapse: collapse; }
    table th { text-align: left; border-bottom: 1px solid #aaa; padding-bottom: 6px; }
    table td { padding: 4px 0; }
    .amount { text-align: right; }
    .summary { font-size: 15px; margin-top: 12px; }
    .row-sum { display: flex; justify-content: space-between; margin: 3px 0; }
    .total { font-size: 19px; margin-top: 8px; font-weight: bold; border-top: 2px solid #000; padding-top: 6px; }
    .footer { text-align: center; font-size: 12px; margin-top: 20px; color: #444; }
</style>
</head>

<body>
<div class="wrapper">

    <div class="title">Warung Makan Nusantara</div>
    <div class="subtitle">Jl. Contoh No.10 — 0812-3456-7890</div>

    <div class="divider"></div>

    <div class="info">
        <p><b>No Pesanan:</b> ${j.receipt.orderId}</p>
        <p><b>Nomor Meja:</b> ${tableNo}</p>
        <p><b>Metode Pembayaran:</b> ${pay.toUpperCase()}</p>
        <p><b>Tanggal:</b> ${new Date().toLocaleString('id-ID')}</p>
    </div>

    <table>
        <thead>
            <tr>
                <th>Menu</th>
                <th class="amount">Harga</th>
            </tr>
        </thead>
        <tbody>
            ${itemsHtml}
        </tbody>
    </table>

    <div class="summary">
        <div class="row-sum"><span>Subtotal</span> <span>${toRp(subtotal)}</span></div>
        <div class="row-sum"><span>Pajak (11%)</span> <span>${toRp(tax)}</span></div>
        <div class="total"><span>Total</span> <span style="float:right">${toRp(total)}</span></div>
    </div>

    <div class="footer">
        Terima kasih telah berbelanja<br>
        — Bukti pembayaran sah —
    </div>
</div>

<script>
    window.onload = () => window.print();
</script>
</body>
</html>

        `);
        win.document.close();
    }

    document.getElementById("btnBack").onclick = () => {
        window.location.href = "/index.html";
    };

    document.getElementById('btnCheckout').onclick = async () => {
        const tableNo = document.getElementById('tableNo').value;
        const pay = document.querySelector('input[name=pay]:checked').value;

        const res = await fetch('/api/checkout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                tableNumber: tableNo,
                paymentMethod: pay
            })
        });

        const j = await res.json();
        console.log(res.status, j);

        if (!j.ok) {
            alert("Checkout gagal — pastikan keranjang berisi item!");
            return;
        }

        if (pay === 'qris') {
            const popup = openQRPopup();
            const wait = setInterval(() => {
                if (popup.closed) {
                    clearInterval(wait);
                    printReceipt(j, tableNo, pay);
                }
            }, 500);
        } else {
            printReceipt(j, tableNo, pay);
        }

        loadCart();
    };

    loadCart();
});
