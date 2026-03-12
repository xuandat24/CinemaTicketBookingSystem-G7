// ==========================================
// 1. XỬ LÝ ĐĂNG NHẬP BẰNG TÀI KHOẢN (LOGIN THƯỜNG)
// ==========================================
const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function(e){
        e.preventDefault();

        const username = document.getElementById("username").value;
        const password = document.getElementById("password").value;

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ userName: username, password: password })
            });

            if(!response.ok){
                alert("Tài khoản hoặc mật khẩu không đúng!");
                return;
            }

            const data = await response.json();

            // Lưu thông tin vào LocalStorage
            localStorage.setItem("jwtToken", data.token);
            localStorage.setItem("username", username);

            alert("Đăng nhập thành công!");
            window.location.href = "/"; // Chuyển về trang chủ
        } catch (error) {
            console.error("Lỗi hệ thống:", error);
        }
    });
}

// ==========================================
// 2. XỬ LÝ ĐĂNG KÝ (REGISTER)
// ==========================================
const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener("submit", async function (e) {
        e.preventDefault();

        document.querySelectorAll('.text-danger').forEach(el => el.innerText = '');
        const generalError = document.getElementById("errorMsg");
        if (generalError) generalError.innerText = '';

        const formData = {
            firstName: document.getElementById("firstName")?.value || "",
            lastName: document.getElementById("lastName")?.value || "",
            userName: document.getElementById("userName")?.value || "",
            phone: document.getElementById("phone")?.value || "",
            email: document.getElementById("email")?.value || "",
            password: document.getElementById("password")?.value || "",
            roleId: 2
        };

        try {
            const response = await fetch("/api/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                if (errorData.errors) {
                    errorData.errors.forEach(err => {
                        const errorDiv = document.getElementById(`error-${err.field}`);
                        if (errorDiv) errorDiv.innerText = err.defaultMessage;
                    });
                }
                if (generalError) generalError.innerText = errorData.message || "Đăng ký thất bại.";
                return;
            }

            alert("Đăng ký thành công! Vui lòng đăng nhập.");
            window.location.href = "/login";

        } catch (error) {
            console.error("Lỗi hệ thống:", error);
            if (generalError) generalError.innerText = "Không thể kết nối tới máy chủ!";
        }
    });
}

// ==========================================
// 3. BẮT URL GOOGLE (CHẠY NGAY LẬP TỨC ĐỂ KHÔNG BỊ TRỄ)
// ==========================================
const urlParams = new URLSearchParams(window.location.search);
const tokenFromUrl = urlParams.get('token');
let usernameFromUrl = urlParams.get('username');

if (tokenFromUrl) {
    // Giải mã tên hiển thị (tránh lỗi font chữ tiếng Việt hoặc dấu cộng)
    if (usernameFromUrl) {
        usernameFromUrl = decodeURIComponent(usernameFromUrl.replace(/\+/g, ' '));
    } else {
        usernameFromUrl = "Google User";
    }

    // Lưu vào kho LocalStorage ngay lập tức
    localStorage.setItem("jwtToken", tokenFromUrl);
    localStorage.setItem("username", usernameFromUrl);

    // Xóa tham số trên thanh địa chỉ URL cho sạch đẹp
    window.history.replaceState({}, document.title, window.location.pathname);
}

// ==========================================
// 4. QUẢN LÝ GIAO DIỆN NAVBAR (ĐỢI HTML LOAD XONG MỚI CHẠY)
// ==========================================
document.addEventListener("DOMContentLoaded", function() {
    const navLogin = document.getElementById('nav-login');
    const navUser = document.getElementById('nav-user');
    const displayUsername = document.getElementById('display-username');
    const btnLogout = document.getElementById('btn-logout');

    // Lấy giá trị từ LocalStorage
    const savedUsername = localStorage.getItem('username');
    const savedToken = localStorage.getItem('jwtToken');

    // Hàm cắt ngắn tên
    function shortenName(name) {
        if (!name || name === "null") return "User";
        if (name.includes('@')) name = name.split('@')[0];
        if (name.length > 12) return name.substring(0, 12) + "...";
        return name;
    }

    // Cập nhật UI
    if (savedToken && savedUsername && savedUsername !== "null") {
        if (navLogin) navLogin.style.display = 'none';
        if (navUser) navUser.style.display = 'block';
        if (displayUsername) displayUsername.innerText = shortenName(savedUsername);
    } else {
        if (navLogin) navLogin.style.display = 'block';
        if (navUser) navUser.style.display = 'none';
    }

    // Xử lý nút đăng xuất
    if (btnLogout) {
        btnLogout.addEventListener('click', function(e) {
            e.preventDefault();
            localStorage.removeItem("jwtToken");
            localStorage.removeItem("username");
            window.location.href = "/";
        });
    }
});