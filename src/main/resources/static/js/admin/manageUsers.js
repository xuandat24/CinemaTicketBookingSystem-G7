let globalUsers = [];
let userModal;

document.addEventListener("DOMContentLoaded", () => {
    userModal = new bootstrap.Modal(document.getElementById('userModal'));
    fetchUsers();
});

async function fetchUsers() {
    const token = sessionStorage.getItem("jwtToken");
    const tbody = document.getElementById("userTableBody");
    tbody.innerHTML = `<tr><td colspan="5" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <div class="text-muted mt-2 fw-bold">Fetching user data...</div>
    </td></tr>`;

    try {
        const response = await fetch('/api/admin/users?t=' + new Date().getTime(), {
            headers: { 'Authorization': 'Bearer ' + token, 'Cache-Control': 'no-cache' }
        });
        if (response.ok) {
            globalUsers = await response.json();
            renderTable(globalUsers);
            updateDashboardStats(globalUsers);
            const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function (tooltipTriggerEl) { return new bootstrap.Tooltip(tooltipTriggerEl) });
        }
    } catch (error) {
        Swal.fire({ icon: 'error', title: 'Connection Error', text: 'Failed to fetch users from server!' });
    }
}

function updateDashboardStats(users) {
    animateValue("statTotal", 0, users.length, 1000);
    animateValue("statAdmin", 0, users.filter(u => u.role && u.role.roleName.toUpperCase().includes('ADMIN')).length, 1000);
    animateValue("statLocal", 0, users.filter(u => u.provider === 'LOCAL').length, 1000);
    animateValue("statGoogle", 0, users.filter(u => u.provider === 'GOOGLE').length, 1000);
}

function animateValue(id, start, end, duration) {
    if (start === end) { document.getElementById(id).innerText = end; return; }
    let range = end - start;
    let current = start;
    let increment = end > start ? 1 : -1;
    let stepTime = Math.abs(Math.floor(duration / range));
    let obj = document.getElementById(id);
    let timer = setInterval(function() {
        current += increment;
        obj.innerText = current;
        if (current == end) clearInterval(timer);
    }, stepTime);
}

const colors = ['#4e73df', '#1cc88a', '#36b9cc', '#f6c23e', '#e74a3b', '#858796', '#5a5c69'];

function renderTable(users) {
    const tbody = document.getElementById("userTableBody");
    tbody.innerHTML = "";

    if(users.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-5 text-muted fw-bold"><i class="fa-solid fa-folder-open fa-2x mb-2 d-block"></i> No users found.</td></tr>`;
        return;
    }

    users.forEach((user, index) => {
        const roleName = user.role ? user.role.roleName : "N/A";
        const isAdmin = roleName.toUpperCase().includes("ADMIN");
        const roleBadge = isAdmin ? "badge-soft-danger" : "badge-soft-primary";
        const roleIcon = isAdmin ? '<i class="fa-solid fa-shield-halved me-1"></i>' : '<i class="fa-solid fa-user me-1"></i>';

        const toggleRoleBtn = isAdmin
            ? `<button class="btn-action role" onclick="toggleRole(${user.userId}, 'demote')" data-bs-toggle="tooltip" title="Demote to User"><i class="fa-solid fa-arrow-down"></i></button>`
            : `<button class="btn-action role" onclick="toggleRole(${user.userId}, 'promote')" data-bs-toggle="tooltip" title="Promote to Admin"><i class="fa-solid fa-arrow-up"></i></button>`;

        const initial = user.firstName ? user.firstName.charAt(0).toUpperCase() : "U";
        const bgColor = colors[user.userId % colors.length];
        const animationDelay = index * 0.05;

        tbody.innerHTML += `
            <tr class="animate__animated animate__fadeInUp" style="animation-delay: ${animationDelay}s;">
                <td>
                    <div class="d-flex align-items-center">
                        <div class="avatar-circle shadow-sm" style="background-color: ${bgColor};">${initial}</div>
                        <div>
                            <span class="d-block fw-bold text-dark">${user.firstName} ${user.lastName}</span>
                            <span class="text-muted small">ID: #${user.userId}</span>
                        </div>
                    </div>
                </td>
                <td class="fw-bold text-secondary">@${user.userName}</td>
                <td>
                    <div class="text-dark"><i class="fa-solid fa-envelope text-muted small me-1"></i> ${user.email}</div>
                    ${user.phone ? `<div class="text-muted small mt-1"><i class="fa-solid fa-phone me-1"></i> ${user.phone}</div>` : ''}
                </td>
                <td><span class="${roleBadge}">${roleIcon} ${roleName}</span></td>
                <td class="text-end pe-4">
                    <div class="d-flex gap-2 justify-content-end">
                        ${toggleRoleBtn}
                        <button class="btn-action edit" onclick="openEditModal(${user.userId})" data-bs-toggle="tooltip" title="View & Edit"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-action delete" onclick="deleteUser(${user.userId})" data-bs-toggle="tooltip" title="Delete Account"><i class="fa-solid fa-trash"></i></button>
                    </div>
                </td>
            </tr>
        `;
    });
}

function clearErrors() {
    document.querySelectorAll('.error-text').forEach(el => el.innerText = "");
    document.querySelectorAll('.form-control, .form-select').forEach(el => el.classList.remove('is-invalid'));
}

// Hàm in lỗi trực tiếp dưới ô nhập liệu
function showError(fieldId, message) {
    const inputEl = document.getElementById(fieldId);
    const errorEl = document.getElementById('err-' + fieldId);
    if (inputEl) inputEl.classList.add('is-invalid');
    if (errorEl) {
        errorEl.innerText = message;
        errorEl.classList.add('animate__animated', 'animate__headShake');
        setTimeout(() => errorEl.classList.remove('animate__animated', 'animate__headShake'), 500);
    }
}

function openAddModal() {
    document.getElementById('userForm').reset();
    document.getElementById('userId').value = '';
    document.getElementById('userName').disabled = false;
    document.getElementById('email').disabled = false;
    document.getElementById('createdAtGroup').style.display = 'none';
    document.getElementById('passwordGroup').style.display = 'block';
    document.getElementById('rePasswordGroup').style.display = 'block';

    document.getElementById('modalTitle').innerHTML = '<i class="fa-solid fa-user-plus text-primary me-2"></i> Add New User';
    clearErrors();
    userModal.show();
}

function openEditModal(id) {
    const user = globalUsers.find(u => u.userId === id);
    if(!user) return;

    document.getElementById('userId').value = user.userId;
    document.getElementById('userName').value = user.userName;
    document.getElementById('userName').disabled = true;
    document.getElementById('email').value = user.email;
    document.getElementById('email').disabled = true;
    document.getElementById('firstName').value = user.firstName;
    document.getElementById('lastName').value = user.lastName;
    document.getElementById('phone').value = user.phone;
    document.getElementById('dob').value = user.dob;

    let g = user.gender;
    if (g && g.trim() !== "") {
        g = g.charAt(0).toUpperCase() + g.slice(1).toLowerCase();
        document.getElementById('gender').value = (g === "Male" || g === "Female") ? g : "Other";
    } else { document.getElementById('gender').value = "Male"; }

    document.getElementById('createdAtGroup').style.display = 'block';
    document.getElementById('createdAt').value = user.createdAt ? user.createdAt : "N/A";
    document.getElementById('passwordGroup').style.display = 'none';
    document.getElementById('rePasswordGroup').style.display = 'none';

    document.getElementById('modalTitle').innerHTML = '<i class="fa-solid fa-user-pen text-primary me-2"></i> View & Edit User Profile';
    clearErrors();
    userModal.show();
}

async function saveUser() {
    clearErrors();
    let hasError = false;

    const id = document.getElementById('userId').value;
    const userName = document.getElementById('userName').value.trim();
    const email = document.getElementById('email').value.trim();
    const firstName = document.getElementById('firstName').value.trim();
    const lastName = document.getElementById('lastName').value.trim();
    const phone = document.getElementById('phone').value.trim();
    const dob = document.getElementById('dob').value;
    const pass = document.getElementById('password').value;
    const rePass = document.getElementById('rePassword').value;

    // Xác thực tại ô nhập liệu (Inline Validation)
    if (!userName) { showError('userName', 'Username is required.'); hasError = true; }
    if (!email) { showError('email', 'Email is required.'); hasError = true; }
    if (!firstName) { showError('firstName', 'First name is required.'); hasError = true; }
    if (!lastName) { showError('lastName', 'Last name is required.'); hasError = true; }

    if (!/^(03|09)\d{8}$/.test(phone)) { showError('phone', 'Phone must be 10 digits (start with 03/09).'); hasError = true; }
    if (!dob) { showError('dob', 'Date of birth is required.'); hasError = true; }
    else {
        const dobDate = new Date(dob);
        if (dobDate > new Date()) { showError('dob', 'Date of birth cannot be in the future.'); hasError = true; }
        if (dobDate.getFullYear() < 1900) { showError('dob', 'Invalid birth year.'); hasError = true; }
    }

    if (!id) {
        if (!pass) { showError('password', 'Password is required.'); hasError = true; }
        else if (pass.length < 8) { showError('password', 'Password must be at least 8 characters.'); hasError = true; }
        else if (pass !== rePass) { showError('rePassword', 'Passwords do not match.'); hasError = true; }
    }

    if (hasError) return;

    const data = { userName, email, firstName, lastName, phone, dob, gender: document.getElementById('gender').value, password: pass };
    const token = sessionStorage.getItem("jwtToken");

    const btn = document.querySelector("#userModal .btn-primary");
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Saving...';
    btn.disabled = true;

    try {
        const res = await fetch(id ? `/api/admin/users/${id}` : '/api/admin/users', {
            method: id ? 'PUT' : 'POST',
            headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        const result = await res.json();

        if (res.ok) {
            userModal.hide();
            // Pop-up chung khi Thành công
            Swal.fire({ icon: 'success', title: 'Saved Successfully!', text: result.message, timer: 2000, showConfirmButton: false });
            fetchUsers();
        } else {
            // Lỗi riêng cho từng ô từ Server (VD: Trùng username) -> in xuống ô
            if (result.errorField) {
                showError(result.errorField, result.message);
            } else {
                // Lỗi chung (VD: Lỗi Database) -> In ra Pop-up
                Swal.fire({ icon: 'error', title: 'Action Failed', text: result.message });
            }
        }
    } catch (err) {
        Swal.fire({ icon: 'error', title: 'Network Error', text: 'Unable to connect to the server.' });
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}

async function toggleRole(userId, type) {
    const textMsg = type === 'demote'
        ? "If demoted, they will lose admin privileges immediately!"
        : "They will gain full access to the admin dashboard.";

    const { isConfirmed } = await Swal.fire({
        title: 'Are you sure?', text: textMsg, icon: 'warning',
        showCancelButton: true, confirmButtonColor: '#4e73df', cancelButtonColor: '#858796', confirmButtonText: 'Yes, proceed!'
    });

    if (!isConfirmed) return;

    const token = sessionStorage.getItem("jwtToken");
    const res = await fetch(`/api/admin/users/${userId}/toggle-role`, { method: 'PUT', headers: { 'Authorization': 'Bearer ' + token } });
    const result = await res.json();

    if (res.ok) {
        Swal.fire({ icon: 'success', title: 'Role Updated!', text: result.message, timer: 1500, showConfirmButton: false });
        fetchUsers();
    } else {
        Swal.fire({ icon: 'error', title: 'Action Denied', text: result.message });
    }
}

async function deleteUser(id) {
    const { isConfirmed } = await Swal.fire({
        title: 'Delete this account?', text: "This will permanently remove the user and their related data!",
        icon: 'error', showCancelButton: true, confirmButtonColor: '#e74a3b', cancelButtonColor: '#858796', confirmButtonText: 'Yes, delete it!'
    });

    if (!isConfirmed) return;

    const token = sessionStorage.getItem("jwtToken");
    const res = await fetch(`/api/admin/users/${id}`, { method: 'DELETE', headers: { 'Authorization': 'Bearer ' + token }});
    const result = await res.json();

    if (res.ok) {
        Swal.fire({ icon: 'success', title: 'Deleted!', text: 'User has been removed.', timer: 1500, showConfirmButton: false });
        fetchUsers();
    } else {
        Swal.fire({ icon: 'error', title: 'Error', text: result.message });
    }
}