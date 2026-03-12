// ==========================================
// 1. XỬ LÝ ĐĂNG NHẬP (LOGIN)
// ==========================================
const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function(e){
        e.preventDefault();

        // Lấy dữ liệu từ form login.html
        const username = document.getElementById("username").value;
        const password = document.getElementById("password").value;

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    userName: username,
                    password: password
                })
            });

            if(!response.ok){
                alert("Tài khoản hoặc mật khẩu không đúng!");
                return;
            }

            const data = await response.json();
            // Lưu token
            localStorage.setItem("jwtToken", data.token);

            alert("Đăng nhập thành công!");
            window.location.href = "/"; // Chuyển về trang chủ
        } catch (error) {
            console.error("Lỗi hệ thống:", error);
        }
    });
}
document.addEventListener("DOMContentLoaded", function() {
    const navLogin = document.getElementById('nav-login');
    const navUser = document.getElementById('nav-user');
    const displayUsername = document.getElementById('display-username');
    const btnLogout = document.getElementById('btn-logout');

    // 1. Kiểm tra trạng thái đăng nhập
    const savedUsername = localStorage.getItem('username'); // Hoặc key bạn đã đặt khi login

    if (savedUsername) {
        // Nếu có user: Ẩn nút Login, hiện Dropdown Username
        if(navLogin) navLogin.style.display = 'none';
        if(navUser) navUser.style.display = 'block';
        if(displayUsername) displayUsername.innerText = savedUsername;
    } else {
        // Nếu không có: Hiện nút Login, ẩn Dropdown
        if(navLogin) navLogin.style.display = 'block';
        if(navUser) navUser.style.display = 'none';
    }

    // 2. Xử lý sự kiện Logout
    if (btnLogout) {
        btnLogout.addEventListener('click', function(e) {
            e.preventDefault();
            localStorage.removeItem('username'); // Xóa dữ liệu
            // localStorage.clear(); // Hoặc xóa hết nếu cần
            window.location.reload(); // Load lại trang để cập nhật giao diện
        });
    }
});

// ==========================================
// 2. XỬ LÝ ĐĂNG KÝ (REGISTER)
// ==========================================
const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener("submit", async function (e) {
        e.preventDefault();

        // 1. Xóa tất cả thông báo lỗi cũ trước khi gửi yêu cầu mới
        document.querySelectorAll('.text-danger').forEach(el => el.innerText = '');
        const generalError = document.getElementById("errorMsg");
        if (generalError) generalError.innerText = '';

        // 2. Thu thập dữ liệu
        const formData = {
            firstName: document.getElementById("firstName").value,
            lastName: document.getElementById("lastName").value,
            userName: document.getElementById("userName").value,
            phone: document.getElementById("phone").value,
            email: document.getElementById("email").value,
            password: document.getElementById("password").value,
            roleId: 2
        };

        try {
            const response = await fetch("/api/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                const errorData = await response.json(); // Bây giờ 100% là JSON

                // 1. Nếu có lỗi chi tiết từng trường (Validation)
                if (errorData.errors) {
                    errorData.errors.forEach(err => {
                        const errorDiv = document.getElementById(`error-${err.field}`);
                        if (errorDiv) errorDiv.innerText = err.defaultMessage;
                    });
                }

                // 2. Hiển thị thông báo lỗi tổng quát (Dùng cho cả lỗi logic và lỗi validation)
                const generalError = document.getElementById("errorMsg");
                if (generalError) generalError.innerText = errorData.message;

                return;
            }

            // NẾU THÀNH CÔNG
            alert("Đăng ký thành công!");
            window.location.href = "/login";

        } catch (error) {
            console.error("Lỗi hệ thống:", error);
            if (generalError) generalError.innerText = "Không thể kết nối tới máy chủ!";
        }
    });
}

// ==========================================
// 3. CÁC HÀM TIỆN ÍCH DÙNG CHUNG (Để ở ngoài cùng)
// ==========================================
function logout() {
    localStorage.removeItem("jwtToken");
    window.location.href = "/login";
}

function checkLogin() {
    const token = localStorage.getItem("jwtToken");
    if (!token) {
        window.location.href = "/login";
    }
}