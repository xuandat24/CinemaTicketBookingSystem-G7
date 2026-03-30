document.addEventListener("DOMContentLoaded", async function () {
    const token = localStorage.getItem("jwtToken");

    // 1. Chặn người dùng chưa đăng nhập
    if (!token) {
        alert("Cảnh báo: Bạn cần đăng nhập bằng tài khoản Quản trị viên!");
        window.location.href = "/login";
        return;
    }

    try {
        // 2. Kiểm tra danh tính bằng API my-profile
        const response = await fetch("/api/users/my-profile", {
            method: "GET",
            headers: {
                "Authorization": "Bearer " + token
            }
        });

        if (response.ok) {
            const userProfile = await response.json();

            // 3. Khóa luồng User thường (Role = 1 là Admin theo DataSeeder)
            if (userProfile.roleId !== 1) {
                alert("Truy cập bị từ chối! Khu vực này chỉ dành cho Admin.");
                window.location.href = "/"; // Đá văng về trang chủ
            }
            // Nếu là Admin (roleId === 1) thì cho phép ở lại trang
        } else {
            // Token bị hỏng hoặc hết hạn
            alert("Phiên đăng nhập không hợp lệ hoặc đã hết hạn!");
            localStorage.removeItem("jwtToken");
            localStorage.removeItem("username");
            window.location.href = "/login";
        }
    } catch (error) {
        console.error("Lỗi xác thực:", error);
        window.location.href = "/login";
    }
});