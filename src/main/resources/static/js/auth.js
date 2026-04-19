document.addEventListener("DOMContentLoaded", function () {
    console.log("=== SESSION SECURITY SYSTEM INITIALIZED ===");

    localStorage.removeItem("jwtToken");
    localStorage.removeItem("username");

    const currentPath = window.location.pathname.replace(/\/$/, "") || "/";
    const urlParams = new URLSearchParams(window.location.search);
    const urlToken = urlParams.get('token');

    // 1. NHẬN TOKEN TỪ GOOGLE LOGIN
    if (urlToken) {
        let urlUsername = urlParams.get('username') || "Google User";
        urlUsername = decodeURIComponent(urlUsername.replace(/\+/g, ' '));
        sessionStorage.setItem("jwtToken", urlToken);
        sessionStorage.setItem("username", urlUsername);

        // BẮT BUỘC PHẢI THÊM 2 DÒNG NÀY:
        document.cookie = "jwtToken=" + urlToken + "; path=/; max-age=86400;";
        document.cookie = "username=" + encodeURIComponent(urlUsername) + "; path=/; max-age=86400;";

        window.history.replaceState({}, document.title, window.location.pathname);
    }

    // 2. DỌN DẸP GHOST SESSION
    const activeToken = sessionStorage.getItem("jwtToken");
    if (!activeToken && !urlToken) {
        if (!sessionStorage.getItem("ghost_cleaned")) {
            fetch("/api/auth/logout", {method: "POST"}).catch(() => {
            });
            sessionStorage.setItem("ghost_cleaned", "true");
        }
    }

    // 3. XỬ LÝ ẨN/HIỆN MẬT KHẨU (LOGIN VÀ REGISTER)
    const toggleFields = [
        {btn: "toggleLoginPassword", input: "password"}, // Login ID
        {btn: "toggleRegPassword", input: "password"}    // Register ID
    ];

    toggleFields.forEach(field => {
        const toggleBtn = document.getElementById(field.btn);
        const inputField = document.getElementById(field.input);
        if (toggleBtn && inputField) {
            toggleBtn.addEventListener("click", function () {
                const type = inputField.getAttribute("type") === "password" ? "text" : "password";
                inputField.setAttribute("type", type);
                this.classList.toggle("fa-eye");
                this.classList.toggle("fa-eye-slash");
            });
        }
    });

    // 4. ANIMATION SLIDER TRANG LOGIN
    let currentSlide = 0;
    const slides = document.querySelectorAll('.login-slide');
    if (slides.length > 0) {
        setInterval(() => {
            slides[currentSlide].classList.remove('active');
            currentSlide = (currentSlide + 1) % slides.length;
            slides[currentSlide].classList.add('active');
        }, 4000);
    }

    // 5. XỬ LÝ FORM LOCAL LOGIN
    const loginForms = document.querySelectorAll('form[action*="/login"], form[action*="/api/auth/login"], .login-card form');
    loginForms.forEach(form => {
        const isLoginForm = form.querySelector('input[name="userName"], input[id="username"]');
        if (isLoginForm && form.id !== "registerForm") {
            form.addEventListener("submit", async function (e) {
                e.preventDefault();
                const usernameInput = form.querySelector('input[name="userName"], input[id="username"]');
                const passwordInput = form.querySelector('input[name="password"], input[id="password"]');

                if (!usernameInput || !passwordInput) return;

                const btnSubmit = form.querySelector('button[type="submit"], .login-btn-submit');
                const originalText = btnSubmit ? btnSubmit.innerHTML : 'Đăng nhập';
                if (btnSubmit) {
                    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processing...';
                    btnSubmit.disabled = true;
                }

                try {
                    const response = await fetch("/api/auth/login", {
                        method: "POST",
                        headers: {"Content-Type": "application/json"},
                        body: JSON.stringify({userName: usernameInput.value.trim(), password: passwordInput.value})
                    });

                    let data;
                    try {
                        data = await response.json();
                    } catch (err) {
                        data = {message: "Lỗi Server"};
                    }

                    if (response.ok) {
                        const token = data.token || data.jwtToken || data.jwt || data.accessToken;
                        const finalUsername = data.userName || usernameInput.value.trim();
                        sessionStorage.setItem("jwtToken", token);
                        sessionStorage.setItem("username", finalUsername);

                        // BẮT BUỘC PHẢI THÊM 2 DÒNG NÀY:
                        document.cookie = "jwtToken=" + token + "; path=/; max-age=86400;";
                        document.cookie = "username=" + encodeURIComponent(finalUsername) + "; path=/; max-age=86400;";

                        window.location.replace("/");
                    } else {
                        alert("Login failed: " + (data.message || "Sai thông tin!"));
                        if (btnSubmit) {
                            btnSubmit.innerHTML = originalText;
                            btnSubmit.disabled = false;
                        }
                    }
                } catch (error) {
                    alert("Server connection error!");
                    if (btnSubmit) {
                        btnSubmit.innerHTML = originalText;
                        btnSubmit.disabled = false;
                    }
                }
            });
        }
    });

    // 6. HIỂN THỊ MENU VÀ KIỂM TRA QUYỀN (TÍCH HỢP ĐỒNG BỘ AVATAR)
    const navLogin = document.querySelectorAll("#navLogin, #nav-login");
    const navUser = document.querySelectorAll("#navUser, #nav-user");
    const displayUsername = document.getElementById("displayUsername") || document.getElementById("display-username");
    const displayAvatar = document.getElementById("display-user-avatar"); // Bắt lấy thẻ Avatar ở Header

    if (activeToken && activeToken !== "null" && activeToken !== "undefined") {
        fetch("/api/users/my-profile", {
            method: "GET",
            headers: {"Authorization": "Bearer " + activeToken, "Cache-Control": "no-cache"}
        })
            .then(res => {
                if (!res.ok) throw new Error("Error code");
                return res.json();
            })
            .then(user => {
                navLogin.forEach(el => {
                    el.classList.remove('d-flex');
                    el.classList.add('d-none');
                    el.style.display = 'none';
                });
                navUser.forEach(el => {
                    el.classList.remove('d-none');
                    el.classList.add('d-flex');
                    el.style.display = '';
                });

                const safeStr = (str) => (str && str !== "null") ? str : "";
                let dName = "User";

                // Gắn Tên người dùng ưu tiên Tên Thật -> Username -> Email
                if (displayUsername) {
                    dName = `${safeStr(user.firstName)} ${safeStr(user.lastName)}`.trim();
                    if (!dName) dName = safeStr(user.userName) || (user.email ? user.email.split('@')[0] : "User");
                    dName = dName.replace("@gmail.com", "");
                    dName = dName.length > 15 ? dName.substring(0, 15) + '...' : dName;
                    displayUsername.innerText = dName;
                }

                // GẮN ẢNH AVATAR LÊN HEADER
                if (displayAvatar) {
                    if (user.avatar && user.avatar !== "null") {
                        displayAvatar.src = user.avatar;
                    } else {
                        displayAvatar.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(dName)}&background=d96c2c&color=fff&size=100`;
                    }
                }

                // Điều hướng theo Role
                const roleId = Number(user.roleId);
                if (roleId === 1) {
                    if (currentPath === "/" || currentPath.includes("/login")) window.location.replace("/admin/home");
                } else {
                    if (currentPath.includes("/admin")) window.location.replace("/");
                    else if (currentPath.includes("/login") || currentPath.includes("/register")) window.location.replace("/");
                }
            })
            .catch(err => {
                sessionStorage.clear();
                if (currentPath.includes("/admin") || currentPath.includes("/profile")) window.location.replace("/login");
            });
    } else {
        navUser.forEach(el => {
            el.classList.remove('d-flex');
            el.classList.add('d-none');
            el.style.display = 'none';
        });
        navLogin.forEach(el => {
            el.classList.remove('d-none');
            el.classList.add('d-flex');
            el.style.display = '';
        });
        if (currentPath.includes("/admin") || currentPath.includes("/profile")) window.location.replace("/login");
    }

    // 7. XỬ LÝ LOGOUT USER
    const btnLogout = document.getElementById("btn-logout");
    if (btnLogout) {
        btnLogout.addEventListener("click", async function (e) {
            e.preventDefault();
            const confirmMsg = (window.I18N && window.I18N.logoutConfirm) ? window.I18N.logoutConfirm : "Are you sure you want to log out?";

            if (confirm(confirmMsg)) {
                const t = sessionStorage.getItem("jwtToken");
                sessionStorage.clear();
                try {
                    const headers = t ? {"Authorization": "Bearer " + t} : {};
                    await fetch("/api/auth/logout", {method: "POST", headers: headers});
                } catch (err) {
                }
                window.location.replace("/login");
            }
        });
    }

    // 8. XỬ LÝ FORM ĐĂNG KÝ
    const registerForm = document.getElementById("registerForm");
    if (registerForm) {
        registerForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            const MSG = {
                errFirstname: registerForm.getAttribute("data-err-firstname") || "Please enter your First Name!",
                errLastname: registerForm.getAttribute("data-err-lastname") || "Please enter your Last Name!",
                errUsername: registerForm.getAttribute("data-err-username") || "Please enter a Username!",
                errEmail: registerForm.getAttribute("data-err-email") || "Please enter your Email!",
                errGender: registerForm.getAttribute("data-err-gender") || "Please select your gender!",
                errPhone: registerForm.getAttribute("data-err-phone") || "Please enter your Phone Number!",
                errPhoneFormat: registerForm.getAttribute("data-err-phone-format") || "Phone number must be 10 digits and start with 03/09!",
                errDob: registerForm.getAttribute("data-err-dob") || "Please enter your Date of Birth!",
                errDobInvalid: registerForm.getAttribute("data-err-dob-invalid") || "Invalid year!",
                errDobMin: registerForm.getAttribute("data-err-dob-min") || "Invalid year of birth!",
                errDobFuture: registerForm.getAttribute("data-err-dob-future") || "Year of birth cannot be in the future!",
                errPwd: registerForm.getAttribute("data-err-pwd") || "Please enter a Password!",
                errPwdLength: registerForm.getAttribute("data-err-pwd-length") || "Password must be at least 8 characters!",
                errPwdMatch: registerForm.getAttribute("data-err-pwd-match") || "Passwords do not match!",
                errNetwork: registerForm.getAttribute("data-err-network") || "Server connection error!",
                successMsg: registerForm.getAttribute("data-success") || "Registration successful!"
            };

            document.querySelectorAll('.error-text').forEach(el => el.innerText = '');
            let hasError = false;

            const getVal = (id) => {
                const el = document.getElementById(id);
                return el ? el.value.trim() : "";
            };

            const firstName = getVal("firstName");
            const lastName = getVal("lastName");
            const nameRegex = /^[\p{L}\s]+$/u;
            const userName = getVal("userName");
            const phone = getVal("phoneNumber");
            const email = getVal("email");
            const gender = getVal("gender");
            const dob = getVal("dob");
            const password = getVal("password");
            const confirmPassword = getVal("confirmPassword");

            if (!firstName) {
                document.getElementById("firstNameError").innerText = MSG.errFirstname;
                hasError = true;
            } else if (!nameRegex.test(firstName)) {
                document.getElementById("firstNameError").innerText = registerForm.getAttribute("data-err-name-format") || "First name can only contain letters!";
                hasError = true;
            }

            if (!lastName) {
                document.getElementById("lastNameError").innerText = MSG.errLastname;
                hasError = true;
            } else if (!nameRegex.test(lastName)) {
                document.getElementById("lastNameError").innerText = registerForm.getAttribute("data-err-name-format") || "Last name can only contain letters!";
                hasError = true;
            }

            if (!userName) {
                document.getElementById("userNameError").innerText = MSG.errUsername;
                hasError = true;
            }
            if (!email) {
                document.getElementById("emailError").innerText = MSG.errEmail;
                hasError = true;
            }
            if (!gender) {
                document.getElementById("genderError").innerText = MSG.errGender;
                hasError = true;
            }

            if (!phone) {
                document.getElementById("phoneError").innerText = MSG.errPhone;
                hasError = true;
            } else if (!/^(03|09)\d{8}$/.test(phone)) {
                document.getElementById("phoneError").innerText = MSG.errPhoneFormat;
                hasError = true;
            }

            const dobErrorDiv = document.getElementById("dobError");
            if (!dob) {
                if (dobErrorDiv) dobErrorDiv.innerText = MSG.errDob;
                hasError = true;
            } else {
                const yearStr = dob.split('-')[0];
                const yearNum = parseInt(yearStr, 10);
                const currentYear = new Date().getFullYear();

                if (yearStr.length > 4) {
                    if (dobErrorDiv) dobErrorDiv.innerText = MSG.errDobInvalid;
                    hasError = true;
                } else if (yearNum < 1900) {
                    if (dobErrorDiv) dobErrorDiv.innerText = MSG.errDobMin;
                    hasError = true;
                } else if (yearNum > currentYear) {
                    if (dobErrorDiv) dobErrorDiv.innerText = MSG.errDobFuture;
                    hasError = true;
                }
            }

            if (!password) {
                document.getElementById("passwordError").innerText = MSG.errPwd;
                hasError = true;
            } else if (password.length < 8) {
                document.getElementById("passwordError").innerText = MSG.errPwdLength;
                hasError = true;
            }

            if (password !== confirmPassword) {
                document.getElementById("confirmPasswordError").innerText = MSG.errPwdMatch;
                hasError = true;
            }

            if (hasError) return;

            const btnSubmit = document.querySelector(".reg-btn-submit") || registerForm.querySelector("button[type='submit']");
            let originalText = "GET STARTED";
            if (btnSubmit) {
                originalText = btnSubmit.innerHTML;
                btnSubmit.disabled = true;
                btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processing...';
            }

            const registerData = {
                firstName: firstName, lastName: lastName, userName: userName,
                phone: phone, email: email, gender: gender, dob: dob, password: password
            };

            try {
                const res = await fetch("/api/auth/register", {
                    method: "POST",
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify(registerData)
                });

                if (res.ok) {
                    alert(MSG.successMsg);
                    window.location.href = "/login";
                } else {
                    const errorData = await res.json();
                    if (errorData.errors && errorData.errors.length > 0) {
                        errorData.errors.forEach(err => {
                            const errorElement = document.getElementById(err.field + "Error");
                            if (errorElement) errorElement.innerText = err.defaultMessage;
                        });
                    } else if (errorData.message) {
                        alert("ERROR: " + errorData.message);
                    }
                }
            } catch (error) {
                alert(MSG.errNetwork);
            } finally {
                if (btnSubmit) {
                    btnSubmit.disabled = false;
                    btnSubmit.innerHTML = originalText;
                }
            }
        });
    }
});