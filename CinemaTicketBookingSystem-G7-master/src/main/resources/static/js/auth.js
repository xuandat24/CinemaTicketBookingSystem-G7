// ==========================================
// 1. LOGIN
// ==========================================
const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function (e) {
        e.preventDefault();

        const username = document.getElementById("username").value;
        const password = document.getElementById("password").value;

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    userName: username,
                    password: password
                })
            });

            if (!response.ok) {
                alert("Tài khoản hoặc mật khẩu không đúng!");
                return;
            }

            const data = await response.json();

            localStorage.setItem("jwtToken", data.token);
            localStorage.setItem("username", username);

            alert("Đăng nhập thành công!");

            // 👉 redirect sang booking nếu có lưu trước đó
            const redirectUrl = localStorage.getItem("redirectAfterLogin");
            if (redirectUrl) {
                localStorage.removeItem("redirectAfterLogin");
                window.location.href = redirectUrl;
            } else {
                window.location.href = "/";
            }

        } catch (error) {
            console.error("Lỗi hệ thống:", error);
        }
    });
}


// ==========================================
// 2. REGISTER
// ==========================================
const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener("submit", async function (e) {
        e.preventDefault();

        document.querySelectorAll('.text-danger').forEach(el => el.innerText = '');

        const password = document.getElementById("password")?.value || "";
        const confirmPassword = document.getElementById("confirmPassword")?.value || "";

        if (password !== confirmPassword) {
            alert("Mật khẩu xác nhận không khớp!");
            return;
        }

        const formData = {
            firstName: document.getElementById("firstName")?.value || "",
            lastName: document.getElementById("lastName")?.value || "",
            userName: document.getElementById("userName")?.value || "",
            phone: document.getElementById("phone")?.value || "",
            email: document.getElementById("email")?.value || "",
            password: password,
            confirmPassword: confirmPassword,
            gender: document.getElementById("gender")?.value || "Khác",
            dob: document.getElementById("dob")?.value || null,
            roleId: 2
        };

        try {
            const response = await fetch("/api/auth/register", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                alert("Đăng ký thất bại!");
                return;
            }

            alert("Đăng ký thành công!");
            window.location.href = "/login";

        } catch (error) {
            console.error("Lỗi hệ thống:", error);
        }
    });
}


// ==========================================
// 3. GOOGLE LOGIN (TOKEN FROM URL)
// ==========================================
const urlParams = new URLSearchParams(window.location.search);
const tokenFromUrl = urlParams.get('token');
let usernameFromUrl = urlParams.get('username');

if (tokenFromUrl) {

    if (usernameFromUrl) {
        usernameFromUrl = decodeURIComponent(usernameFromUrl.replace(/\+/g, ' '));
    } else {
        usernameFromUrl = "Google User";
    }

    localStorage.setItem("jwtToken", tokenFromUrl);
    localStorage.setItem("username", usernameFromUrl);

    window.history.replaceState({}, document.title, window.location.pathname);
}


// ==========================================
// 4. NAVBAR UI
// ==========================================
document.addEventListener("DOMContentLoaded", function () {

    const navLogin = document.getElementById('nav-login');
    const navUser = document.getElementById('nav-user');
    const displayUsername = document.getElementById('display-username');
    const btnLogout = document.getElementById('btn-logout');

    const savedUsername = localStorage.getItem('username');
    const savedToken = localStorage.getItem('jwtToken');

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

    if (btnLogout) {
        btnLogout.addEventListener('click', function (e) {
            e.preventDefault();
            localStorage.removeItem("jwtToken");
            localStorage.removeItem("username");
            window.location.href = "/";
        });
    }
});


// ==========================================
// 5. GO TO BOOKING (🔥 FIX LOOP LOGIN)
// ==========================================
function goToBooking(movieId) {
    const username = localStorage.getItem("username");

    if (!username) {
        localStorage.setItem("redirectAfterLogin", "/booking/" + movieId);
        window.location.href = "/login";
        return;
    }

    window.location.href = "/booking/" + movieId + "?username=" + username;
}