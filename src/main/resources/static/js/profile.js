document.addEventListener("DOMContentLoaded", async function () {
    const token = sessionStorage.getItem("jwtToken");
    if (!token) {
        alert("Please login to view your profile.");
        window.location.href = "/login";
        return;
    }

    const safeStr = (str) => (str && str !== "null") ? str : "";
    let currentAvatarUrl = "";

    // 1. TẢI THÔNG TIN NGƯỜI DÙNG TỪ API
    try {
        const res = await fetch("/api/users/my-profile", {
            method: "GET",
            headers: {"Authorization": "Bearer " + token}
        });

        if (res.ok) {
            const user = await res.json();

            document.getElementById("firstName").value = safeStr(user.firstName);
            document.getElementById("lastName").value = safeStr(user.lastName);
            document.getElementById("email").value = safeStr(user.email);
            document.getElementById("phone").value = safeStr(user.phone);

            let dobStr = "";
            if (user.dob && user.dob !== "null") {
                if (Array.isArray(user.dob)) {
                    let y = user.dob[0];
                    let m = String(user.dob[1]).padStart(2, '0');
                    let d = String(user.dob[2]).padStart(2, '0');
                    dobStr = `${y}-${m}-${d}`;
                } else if (typeof user.dob === 'string' && user.dob.includes('/')) {
                    let parts = user.dob.split('/');
                    if (parts.length === 3) dobStr = `${parts[2]}-${parts[1]}-${parts[0]}`;
                } else if (typeof user.dob === 'string') {
                    dobStr = user.dob.split('T')[0];
                }
            }
            document.getElementById("dob").value = dobStr;

            let g = safeStr(user.gender);
            if (g) {
                g = g.charAt(0).toUpperCase() + g.slice(1).toLowerCase();
                document.getElementById("gender").value = ["Male", "Female", "Other"].includes(g) ? g : "Other";
            } else {
                document.getElementById("gender").value = "Male";
            }

            currentAvatarUrl = safeStr(user.avatar);
            const fullName = `${safeStr(user.firstName)} ${safeStr(user.lastName)}`.trim();
            const defaultAvatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(fullName || "User")}&background=d96c2c&color=fff&size=160`;

            document.getElementById("profile-avatar").src = currentAvatarUrl ? currentAvatarUrl : defaultAvatar;

            document.getElementById("display-fullname-top").innerText = fullName || safeStr(user.userName) || "User";

            // ĐÃ ĐỒNG BỘ: Sử dụng chuẩn FontAwesome v4 (fa fa-star, fa fa-user)
            document.getElementById("display-role-top").innerHTML = user.roleId === 1 ? '<i class="fa fa-star text-warning me-1"></i> Admin' : '<i class="fa fa-user text-secondary me-1"></i> Member';

        } else {
            console.error("Failed to load profile data.");
        }
    } catch (error) {
        console.error("Error loading profile:", error);
    }

    // 2. XỬ LÝ UPLOAD ẢNH LÊN GIAO DIỆN & SERVER
    const avatarUpload = document.getElementById("avatarUpload");
    if (avatarUpload) {
        avatarUpload.addEventListener("change", async function () {
            if (this.files && this.files[0]) {
                const file = this.files[0];
                const reader = new FileReader();
                reader.onload = function (e) {
                    document.getElementById("profile-avatar").src = e.target.result;
                };
                reader.readAsDataURL(file);

                const formData = new FormData();
                formData.append("file", file);

                try {
                    const uploadRes = await fetch("/api/users/upload-avatar", {
                        method: "POST",
                        headers: {"Authorization": "Bearer " + token},
                        body: formData
                    });

                    if (uploadRes.ok) {
                        const data = await uploadRes.json();
                        currentAvatarUrl = data.imagePath;

                        const headerAvatar = document.getElementById("display-user-avatar");
                        if (headerAvatar) headerAvatar.src = data.imagePath;
                    }
                } catch (e) {
                    console.error("Lỗi upload ảnh:", e);
                }
            }
        });
    }

    // 3. XỬ LÝ LƯU & VALIDATE NGHIÊM NGẶT
    const profileForm = document.getElementById("profile-form");
    if (profileForm) {
        profileForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            document.querySelectorAll('.error-message').forEach(el => el.innerText = '');
            let hasError = false;

            const firstName = document.getElementById("firstName").value.trim();
            const lastName = document.getElementById("lastName").value.trim();
            const nameRegex = /^[\p{L}\s]+$/u;
            const phone = document.getElementById("phone").value.trim();
            const dobVal = document.getElementById("dob").value;
            const gender = document.getElementById("gender").value;

            if (!firstName) {
                document.getElementById("firstNameError").innerText = "First Name cannot be empty!";
                hasError = true;
            } else if (!nameRegex.test(firstName)) {
                document.getElementById("firstNameError").innerText = "First Name can only contain letters!";
                hasError = true;
            }
            if (!lastName) {
                document.getElementById("lastNameError").innerText = "Last Name cannot be empty!";
                hasError = true;
            } else if (!nameRegex.test(lastName)) {
                document.getElementById("lastNameError").innerText = "Last Name can only contain letters!";
                hasError = true;
            }
            if (!phone) {
                document.getElementById("phoneError").innerText = "Phone number cannot be empty!";
                hasError = true;
            } else if (!/^(03|09)\d{8}$/.test(phone)) {
                document.getElementById("phoneError").innerText = "Phone must be 10 digits and start with 03 or 09!";
                hasError = true;
            }

            const dobErrorDiv = document.getElementById("dobError");
            if (!dobVal) {
                if (dobErrorDiv) dobErrorDiv.innerText = "Please enter your Date of Birth!";
                hasError = true;
            } else {
                const yearNum = parseInt(dobVal.split('-')[0], 10);
                const currentYear = new Date().getFullYear();
                if (yearNum < 1900) {
                    if (dobErrorDiv) dobErrorDiv.innerText = "Invalid year of birth! (Must be >= 1900)";
                    hasError = true;
                } else if (yearNum > currentYear) {
                    if (dobErrorDiv) dobErrorDiv.innerText = "Year of birth cannot be in the future!";
                    hasError = true;
                }
            }

            if (hasError) return;

            const btnSubmit = profileForm.querySelector("button[type='submit']");
            const originalText = btnSubmit.innerHTML;

            // ĐÃ ĐỒNG BỘ: Sử dụng chuẩn FontAwesome v4 (fa fa-spinner)
            btnSubmit.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i> Saving...';
            btnSubmit.disabled = true;

            const updateData = {
                firstName: firstName,
                lastName: lastName,
                phone: phone,
                dob: dobVal,
                gender: gender,
                avatar: currentAvatarUrl
            };

            try {
                const res = await fetch("/api/users/update-profile", {
                    method: "PUT",
                    headers: {"Content-Type": "application/json", "Authorization": "Bearer " + token},
                    body: JSON.stringify(updateData)
                });

                if (res.ok) {
                    alert("Profile updated successfully!");
                    const newName = firstName + " " + lastName;
                    document.getElementById("display-fullname-top").innerText = newName;
                    if (!currentAvatarUrl) {
                        document.getElementById("profile-avatar").src = `https://ui-avatars.com/api/?name=${encodeURIComponent(newName)}&background=d96c2c&color=fff&size=160`;
                    }
                } else {
                    const errorData = await res.json();
                    if (errorData.errors && errorData.errors.length > 0) {
                        errorData.errors.forEach(err => {
                            const errorElement = document.getElementById(err.field + "Error");
                            if (errorElement) errorElement.innerText = err.defaultMessage;
                        });
                    } else {
                        alert("ERROR: " + (errorData.message || "Failed to update profile."));
                    }
                }
            } catch (error) {
                alert("Server connection error!");
            } finally {
                btnSubmit.innerHTML = originalText;
                btnSubmit.disabled = false;
            }
        });
    }

    // 4. XỬ LÝ NÚT XÓA TÀI KHOẢN
    const btnDelete = document.getElementById("btn-delete-account");
    if (btnDelete) {
        btnDelete.addEventListener("click", async function () {
            if (confirm("WARNING: Are you sure you want to permanently delete this account? This action cannot be undone.")) {
                try {
                    const res = await fetch("/api/users/delete-profile", {
                        method: "DELETE", headers: {"Authorization": "Bearer " + token}
                    });
                    if (res.ok) {
                        alert("Account deleted successfully!");
                        sessionStorage.clear();
                        window.location.href = "/";
                    } else {
                        alert("Error deleting account!");
                    }
                } catch (error) {
                    alert("Connection error while deleting account!");
                }
            }
        });
    }
});