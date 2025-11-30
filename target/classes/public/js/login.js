document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    
    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault(); // Mencegah reload halaman
            
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            
            try {
                const res = await fetch('/api/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                const data = await res.json();

                if (data.ok) {
                    alert('Login Berhasil!');
                    // Arahkan ke halaman admin/dashboard setelah login
                    window.location.href = '/admin-dashboard.html'; 
                } else {
                    alert('Login Gagal: ' + data.error);
                }
            } catch (error) {
                console.error('Error saat login:', error);
                alert('Terjadi kesalahan saat menghubungi server.');
            }
        });
    }
});