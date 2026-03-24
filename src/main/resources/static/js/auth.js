document.addEventListener("DOMContentLoaded", function () {
    // ==========================================
    // 1. BẮT URL GOOGLE NGAY LẬP TỨC
    // ==========================================
    const urlParams = new URLSearchParams(window.location.search);
    const urlToken = urlParams.get('token');
    let urlUsername = urlParams.get('username');

    if (urlToken) {
        if (urlUsername) {
            urlUsername = decodeURIComponent(urlUsername.replace(/\+/g, ' '));
        } else {
            urlUsername = "Google User";
        }

        localStorage.setItem("jwtToken", urlToken);
        localStorage.setItem("username", urlUsername);

        window.history.replaceState({}, document.title, window.location.pathname);

        // THÊM ĐOẠN NÀY ĐỂ TỰ ĐỘNG CHUYỂN TRANG ADMIN KHI ĐĂNG NHẬP GOOGLE
        fetch("/api/users/my-profile", {
            method: "GET",
            headers: { "Authorization": "Bearer " + urlToken }
        }).then(res => res.json())
            .then(userProfile => {
                if (userProfile.roleId === 1) { // Nếu là Admin
                    window.location.href = "/admin/home";
                }
            }).catch(err => console.error(err));
    }
// ... Các code khác giữ nguyên

    // ==========================================
    // 2. XỬ LÝ ĐĂNG NHẬP BẰNG TÀI KHOẢN (LOGIN THƯỜNG)
    // ==========================================
    const loginForm = document.getElementById("loginForm");

    if (loginForm) {
        const usernameInput = document.getElementById("username");
        const passwordInput = document.getElementById("password");
        const rememberCheckbox = document.getElementById("rememberMe");

        // TỰ ĐỘNG ĐIỀN TÀI KHOẢN NẾU TRƯỚC ĐÓ ĐÃ CHỌN "GHI NHỚ"
        if (usernameInput && passwordInput && rememberCheckbox) {
            const savedUser = localStorage.getItem("rememberUsername");
            const savedPass = localStorage.getItem("rememberPassword");

            if (savedUser && savedPass) {
                usernameInput.value = savedUser;
                passwordInput.value = savedPass;
                rememberCheckbox.checked = true; // Tự động tích lại vào ô
            }
        }

        loginForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            const username = usernameInput.value;
            const password = passwordInput.value;

            try {
                const response = await fetch("/api/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ userName: username, password: password })
                });

                if (response.ok) {
                    const data = await response.json();

                    // LƯU TOKEN VÀ USERNAME ĐỂ DUY TRÌ ĐĂNG NHẬP
                    localStorage.setItem("jwtToken", data.token);
                    localStorage.setItem("username", usernameInput.value);

                    // XỬ LÝ TÍNH NĂNG "REMEMBER ME"
                    if (rememberCheckbox && rememberCheckbox.checked) {
                        // Nếu tích chọn, lưu tài khoản & mật khẩu vào máy
                        localStorage.setItem("rememberUsername", username);
                        localStorage.setItem("rememberPassword", password);
                    } else {
                        // Nếu không tích chọn, xóa thông tin đã lưu đi
                        localStorage.removeItem("rememberUsername");
                        localStorage.removeItem("rememberPassword");
                    }

                    alert("Đăng nhập thành công!");

                    // ==========================================
                    // THÊM MỚI: KIỂM TRA ROLE ĐỂ CHUYỂN HƯỚNG
                    // ==========================================
                    try {
                        // Gọi API lấy profile để biết roleId của người vừa đăng nhập
                        const profileRes = await fetch("/api/users/my-profile", {
                            method: "GET",
                            headers: {
                                "Authorization": "Bearer " + data.token
                            }
                        });

                        if (profileRes.ok) {
                            const userProfile = await profileRes.json();

                            // Kiểm tra roleId: 1 là Admin (theo DataSeeder của bạn)
                            if (userProfile.roleId === 1) {
                                window.location.href = "/admin/home"; // Chuyển hướng vào trang Admin
                                return; // Kết thúc hàm tại đây để không chạy lệnh chuyển về "/" bên dưới
                            }
                        }
                    } catch (err) {
                        console.error("Không thể lấy thông tin phân quyền:", err);
                    }

                    // Nếu là User thường (roleId = 2) hoặc có lỗi khi lấy profile, cho về trang chủ
                    window.location.href = "/";
                } else {
                    const err = await response.json();
                    alert("Đăng nhập thất bại: " + (err.message || "Tài khoản hoặc mật khẩu không đúng!"));
                }
            } catch (error) {
                alert("Lỗi kết nối máy chủ!");
            }
        });
    }

    // ==========================================
    // 3. CẬP NHẬT UI (ẨN HIỆN NÚT ĐĂNG NHẬP / TÊN USER)
    // ==========================================
    const navLogin = document.getElementById("nav-login");
    const navUser = document.getElementById("nav-user");
    const displayUsername = document.getElementById("display-username");
    const btnLogout = document.getElementById("btn-logout");

    const savedToken = localStorage.getItem('jwtToken');
    const savedUsername = localStorage.getItem('username');

    // Hàm cắt ngắn tên hiển thị trên thanh điều hướng
    function shortenName(name) {
        if (!name || name === "null") return "User";
        if (name.includes('@')) name = name.split('@')[0];
        if (name.length > 12) return name.substring(0, 12) + "...";
        return name;
    }

    if (savedToken && savedUsername && savedUsername !== "null") {
        if (navLogin) navLogin.style.display = 'none';
        if (navUser) navUser.style.display = 'block';
        if (displayUsername) displayUsername.innerText = shortenName(savedUsername);
    } else {
        if (navLogin) navLogin.style.display = 'block';
        if (navUser) navUser.style.display = 'none';
    }

    // ==========================================
    // 4. XỬ LÝ NÚT ĐĂNG XUẤT (Đã bao gồm xác nhận)
    // ==========================================

    // Đã xóa dòng: const btnLogout = document.getElementById("btn-logout"); vì đã có ở phần 3

    if (btnLogout) {
        // Ghi đè bằng 1 sự kiện duy nhất để không bị lặp
        btnLogout.onclick = async function (e) {
            e.preventDefault();

            // Chỉ chạy logic khi người dùng bấm "OK / Có"
            if (confirm("Bạn có chắc chắn muốn đăng xuất?")) {
                try {
                    // Xóa session trên Server
                    await fetch("/api/auth/logout", { method: "POST" });
                } catch (error) {
                    console.error("Lỗi khi xóa session backend:", error);
                }

                // Dọn dẹp LocalStorage
                localStorage.removeItem("jwtToken");
                localStorage.removeItem("username");

                // Chuyển hướng thẳng về trang đăng nhập
                window.location.href = "/login";
            }
        };
    }
}); // (Dấu đóng của DOMContentLoaded ở cuối file)