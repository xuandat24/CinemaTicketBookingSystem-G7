document.addEventListener("DOMContentLoaded", async function () {
    const token = sessionStorage.getItem("jwtToken");

    if (!token || token === "null" || token === "undefined") {
        window.location.replace("/login");
        return;
    }

    const setVal = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.value = (val && val !== "null") ? val : "";
    };

    let currentAvatarUrl = "";

    // 1. TẢI DỮ LIỆU
    try {
        const response = await fetch("/api/users/my-profile", {
            headers: {"Authorization": "Bearer " + token}
        });

        if (response.ok) {
            const user = await response.json();
            setVal('prof-email', user.email);
            setVal('prof-username', user.userName);
            setVal('prof-firstname', user.firstName);
            setVal('prof-lastname', user.lastName);
            setVal('prof-phone', user.phone);

            let g = user.gender;
            if (g && g.trim() !== "" && g !== "null") {
                g = g.charAt(0).toUpperCase() + g.slice(1).toLowerCase();
                document.getElementById('prof-gender').value = (g === "Male" || g === "Female") ? g : "Other";
            } else {
                document.getElementById('prof-gender').value = "Male";
            }

            if (user.dob && user.dob !== "null") {
                let dobStr = "";
                if (Array.isArray(user.dob)) {
                    dobStr = `${user.dob[0]}-${String(user.dob[1]).padStart(2, '0')}-${String(user.dob[2]).padStart(2, '0')}`;
                } else if (typeof user.dob === 'string') {
                    dobStr = user.dob.split('T')[0];
                }
                setVal('dob', dobStr);
            }

            const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.userName || "Admin";
            document.getElementById("display-fullname").innerText = fullName;

            currentAvatarUrl = (user.avatar && user.avatar !== "null") ? user.avatar : "";
            const adminAvatarImg = document.getElementById("admin-avatar");

            if (currentAvatarUrl) {
                adminAvatarImg.src = currentAvatarUrl;
            } else {
                adminAvatarImg.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(fullName)}&background=d96c2c&color=fff&size=150&bold=true`;
            }
        }
    } catch (error) {
        console.error("Error loading admin profile");
    }

    // 2. UPLOAD ẢNH AVATAR
    const adminAvatarUpload = document.getElementById("adminAvatarUpload");
    if (adminAvatarUpload) {
        adminAvatarUpload.addEventListener("change", async function () {
            if (this.files && this.files[0]) {
                const file = this.files[0];
                const reader = new FileReader();
                reader.onload = function (e) {
                    document.getElementById("admin-avatar").src = e.target.result;
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
                        const sidebarAvatar = document.getElementById("admin-sidebar-avatar");
                        if (sidebarAvatar) sidebarAvatar.src = currentAvatarUrl;
                    } else {
                        alert("Failed to upload avatar.");
                    }
                } catch (e) {
                    alert("Error connecting to server.");
                }
            }
        });
    }

    // 3. XỬ LÝ LƯU & VALIDATE NGHIÊM NGẶT (NHƯ TRANG REGISTER)
    const adminProfileForm = document.getElementById("adminProfileForm");
    if (adminProfileForm) {
        adminProfileForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            // Xóa toàn bộ thông báo lỗi cũ
            document.querySelectorAll('.error-message').forEach(el => el.innerText = '');
            let hasError = false;

            const firstName = document.getElementById("prof-firstname").value.trim();
            const lastName = document.getElementById("prof-lastname").value.trim();
            const nameRegex = /^[\p{L}\s]+$/u; // Chỉ cho phép chữ cái và khoảng trắng (Hỗ trợ tiếng Việt)
            const phone = document.getElementById("prof-phone").value.trim();
            const dobVal = document.getElementById("dob").value;
            const gender = document.getElementById("prof-gender").value;

            // KIỂM TRA FIRST NAME
            if (!firstName) {
                document.getElementById("firstNameError").innerText = "First Name cannot be empty!";
                hasError = true;
            } else if (!nameRegex.test(firstName)) {
                document.getElementById("firstNameError").innerText = "First Name can only contain letters!";
                hasError = true;
            }

            // KIỂM TRA LAST NAME
            if (!lastName) {
                document.getElementById("lastNameError").innerText = "Last Name cannot be empty!";
                hasError = true;
            } else if (!nameRegex.test(lastName)) {
                document.getElementById("lastNameError").innerText = "Last Name can only contain letters!";
                hasError = true;
            }
            if (!gender) {
                document.getElementById("genderError").innerText = MSG.errGender;
                hasError = true;
            }
            // KIỂM TRA SỐ ĐIỆN THOẠI
            if (!phone) {
                document.getElementById("phoneError").innerText = "Phone number cannot be empty!";
                hasError = true;
            } else if (!/^(03|09)\d{8}$/.test(phone)) {
                document.getElementById("phoneError").innerText = "Phone must be 10 digits and start with 03 or 09!";
                hasError = true;
            }

            // KIỂM TRA NGÀY SINH
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

            // Dừng ngay lập tức nếu có bất kỳ lỗi nào
            if (hasError) return;

            const btnSave = document.getElementById("btn-save-profile");
            btnSave.disabled = true;
            btnSave.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Saving...';

            const updateData = {
                firstName: firstName,
                lastName: lastName,
                phone: phone,
                gender: gender,
                dob: dobVal,
                email: document.getElementById("prof-email").value,
                userName: document.getElementById("prof-username").value,
                avatar: currentAvatarUrl
            };

            try {
                const res = await fetch("/api/users/update-profile", {
                    method: "PUT",
                    headers: {
                        "Authorization": "Bearer " + token,
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(updateData)
                });

                if (res.ok) {
                    alert("Cập nhật thông tin Quản trị viên thành công!");
                    location.reload();
                } else {
                    const errorData = await res.json();
                    if (errorData.errors && errorData.errors.length > 0) {
                        errorData.errors.forEach(err => {
                            const errorElement = document.getElementById(err.field + "Error");
                            if (errorElement) errorElement.innerText = err.defaultMessage;
                        });
                    } else {
                        alert("LỖI: " + (errorData.message || "Can not Update"));
                    }
                }
            } catch (error) {
                alert("Lỗi kết nối đến máy chủ!");
            } finally {
                btnSave.disabled = false;
                btnSave.innerHTML = '<i class="fa-solid fa-floppy-disk me-2"></i> Save Changes';
            }
        });
    }
});