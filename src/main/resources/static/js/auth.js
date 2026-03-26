document.addEventListener("DOMContentLoaded", function () {
    const urlParams = new URLSearchParams(window.location.search);
    const urlToken = urlParams.get('token');
    let urlUsername = urlParams.get('username');
    const currentPath = window.location.pathname.replace(/\/$/, "");

    // ==========================================
    // 1. TỰ ĐỘNG DỌN DẸP TOKEN CŨ KHI VÀO TRANG LOGIN
    // ==========================================
    if ((currentPath === "/login" || currentPath.includes("login.html")) && !urlToken) {
        localStorage.removeItem("jwtToken");
        localStorage.removeItem("username");
    }

    // ==========================================
    // 2. NHẬN TOKEN TỪ GOOGLE
    // ==========================================
    if (urlToken) {
        if (urlUsername) urlUsername = decodeURIComponent(urlUsername.replace(/\+/g, ' '));
        else urlUsername = "Google User";

        localStorage.setItem("jwtToken", urlToken);
        localStorage.setItem("username", urlUsername);
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    // ==========================================
    // 3. XỬ LÝ ĐĂNG NHẬP FORM THƯỜNG
    // ==========================================
    const loginForm = document.querySelector('form[action*="/api/auth/login"]') || document.querySelector('.login-card form') || document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            // SỬA CHÍNH XÁC LỖI "CON MẮT" TẠI ĐÂY:
            // Luôn ưu tiên tìm kiếm bằng ID ('userName' và 'password') trước
            const usernameInput = document.getElementById('userName') || loginForm.querySelector('input[name="userName"]') || loginForm.querySelector('input[type="text"]');
            const passwordInput = document.getElementById('password') || loginForm.querySelector('input[name="password"]') || loginForm.querySelector('input[type="password"]');

            if (!usernameInput || !passwordInput) return alert("Lỗi giao diện: Không tìm thấy ô nhập liệu.");

            const btnSubmit = loginForm.querySelector('button[type="submit"]');
            const originalText = btnSubmit ? btnSubmit.innerHTML : 'Login';
            if (btnSubmit) { btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processing...'; btnSubmit.disabled = true; }

            try {
                const response = await fetch("/api/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ userName: usernameInput.value.trim(), password: passwordInput.value })
                });

                if (response.ok) {
                    const data = await response.json();
                    localStorage.setItem("jwtToken", data.token);
                    localStorage.setItem("username", data.userName);
                    window.location.href = "/";
                } else {
                    const errData = await response.json();
                    alert("Đăng nhập thất bại: " + (errData.message || "Sai tài khoản hoặc mật khẩu."));
                }
            } catch (error) {
                alert("Lỗi kết nối máy chủ!");
            } finally {
                if (btnSubmit) { btnSubmit.innerHTML = originalText; btnSubmit.disabled = false; }
            }
        });
    }

    // ==========================================
    // 4. PHÂN LUỒNG QUYỀN LỰC (ADMIN & USER)
    // ==========================================
    const savedToken = localStorage.getItem("jwtToken");
    const navLogin = document.getElementById("nav-login");
    const navUser = document.getElementById("nav-user");
    const displayUsername = document.getElementById("display-username");

    function shortenName(name) { return (!name) ? "" : (name.length > 15 ? name.substring(0, 15) + '...' : name); }

    if (savedToken) {
        fetch("/api/users/my-profile", {
            method: "GET",
            headers: {
                "Authorization": "Bearer " + savedToken,
                "Cache-Control": "no-cache"
            }
        })
            .then(res => {
                if (!res.ok) throw new Error("Token invalid");
                return res.json();
            })
            .then(user => {
                if (navLogin) navLogin.style.display = 'none';
                if (navUser) navUser.style.display = 'block';

                let displayName = user.userName;
                if (!displayName || displayName.trim() === "") displayName = user.email ? user.email.split('@')[0] : "User";
                if (displayUsername) displayUsername.innerText = shortenName(displayName);

                const roleId = Number(user.roleId);

                if (roleId === 1) {
                    if (currentPath === "" || currentPath === "/" || currentPath.includes("login") || currentPath.includes("index")) {
                        window.location.href = "/admin/home";
                    } else if (currentPath.includes("profile") && !currentPath.includes("admin")) {
                        window.location.href = "/admin/profile";
                    }
                    const profileLink = document.querySelector('a[href="/profile"]');
                    if (profileLink) profileLink.href = "/admin/profile";
                }
                else {
                    if (currentPath.includes("/admin")) {
                        window.location.href = "/";
                    } else if (currentPath.includes("login") || currentPath.includes("register")) {
                        window.location.href = "/";
                    }
                }
            })
            .catch(err => {
                localStorage.removeItem("jwtToken");
                localStorage.removeItem("username");
                if (currentPath.includes("/admin") || currentPath.includes("/profile")) window.location.href = "/login";
            });
    } else {
        if (navLogin) navLogin.style.display = 'block';
        if (navUser) navUser.style.display = 'none';
    }

    // ==========================================
    // 5. BẮT SỰ KIỆN LOGOUT (Cả nút JS và Form Spring)
    // ==========================================
    const btnLogout = document.getElementById("btn-logout");
    if (btnLogout) {
        btnLogout.addEventListener("click", async function (e) {
            e.preventDefault();
            if (confirm("Bạn có chắc chắn muốn đăng xuất?")) {
                localStorage.removeItem("jwtToken");
                try { await fetch("/api/auth/logout", { method: "POST", headers: { "Authorization": "Bearer " + savedToken } }); } catch (error) {}
                window.location.href = "/login";
            }
        });
    }

    const logoutForms = document.querySelectorAll('form[action*="/logout"]');
    logoutForms.forEach(form => {
        form.addEventListener("submit", function() {
            localStorage.removeItem("jwtToken");
            localStorage.removeItem("username");
        });
    });
});