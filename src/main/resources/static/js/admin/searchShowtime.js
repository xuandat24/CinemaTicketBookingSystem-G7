const API = "/api/showtimes";
const ROOM_API = "/api/theater-rooms";

let debounceTimer;

function debounceFilter() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => { applyFilter(); }, 400);
}

document.addEventListener("DOMContentLoaded", () => {
    checkSuccessToast();
    loadAll();
    loadRooms();

    const idEle = document.getElementById("filterId");
    const roomEle = document.getElementById("filterRoom");
    const dateEle = document.getElementById("filterDate");
    const keyEle = document.getElementById("filterKeyword");

    if(idEle) idEle.addEventListener("input", debounceFilter);
    if(roomEle) roomEle.addEventListener("change", applyFilter);
    if(dateEle) dateEle.addEventListener("change", applyFilter);
    if(keyEle) keyEle.addEventListener("input", debounceFilter);
});

function resetFilter() {
    document.getElementById("filterId").value = "";
    document.getElementById("filterKeyword").value = "";
    document.getElementById("filterRoom").value = "";
    document.getElementById("filterDate").value = "";
    applyFilter();
}

async function loadAll() {
    try {
        const token = sessionStorage.getItem("jwtToken");
        const response = await fetch(API, { headers: { 'Authorization': 'Bearer ' + token } });
        if(!response.ok) throw new Error("Lỗi API");
        const data = await response.json();
        renderTable(data);
    } catch (error) {
        console.error("Lỗi lấy dữ liệu:", error);
    }
}

async function loadRooms() {
    try {
        const token = sessionStorage.getItem("jwtToken");
        const response = await fetch(ROOM_API, { headers: { 'Authorization': 'Bearer ' + token } });
        if(!response.ok) return;
        const data = await response.json();
        const select = document.getElementById("filterRoom");
        data.forEach(r => {
            select.innerHTML += `<option value="${r.roomId}">${r.roomName}</option>`;
        });
    } catch (error) {}
}

async function applyFilter() {
    const id = document.getElementById("filterId").value.trim();
    const room = document.getElementById("filterRoom").value;
    const date = document.getElementById("filterDate").value;
    const keyword = document.getElementById("filterKeyword").value.trim();

    let url = `${API}/search?`;
    const params = new URLSearchParams();

    if(id) params.append("movieId", id);
    if(room) params.append("roomId", room);
    if(date) params.append("date", date);
    if(keyword) params.append("keyword", keyword);

    try {
        const token = sessionStorage.getItem("jwtToken");
        const response = await fetch(url + params.toString(), { headers: { 'Authorization': 'Bearer ' + token } });
        if(!response.ok) throw new Error("Lỗi filter");
        const data = await response.json();
        renderTable(data);
    } catch (error) {}
}

// ==========================================
// THUẬT TOÁN: TỰ ĐỘNG PHÁT HIỆN TRÙNG LỊCH
// ==========================================
function findConflicts(data) {
    const conflicts = new Set();
    const roomGroups = {};

    // Phân loại phim theo từng phòng
    data.forEach(st => {
        if (st.status !== 'ACTIVE') return;
        if (!roomGroups[st.theaterRoomId]) roomGroups[st.theaterRoomId] = [];
        roomGroups[st.theaterRoomId].push(st);
    });

    const CLEANING_TIME = 15 * 60000; // Cộng thêm 15 phút dọn dẹp

    Object.values(roomGroups).forEach(group => {
        // Sắp xếp các suất chiếu trong cùng 1 phòng theo thời gian
        group.sort((a, b) => new Date(a.startTime) - new Date(b.startTime));

        for (let i = 0; i < group.length - 1; i++) {
            const current = group[i];
            const next = group[i+1];

            // Giờ kết thúc của phim trước + 15p dọn dẹp
            const currentEndMs = new Date(current.endTime).getTime() + CLEANING_TIME;
            const nextStartMs = new Date(next.startTime).getTime();

            // Nếu chưa dọn xong mà phim sau đã chiếu -> Đánh dấu Xung đột
            if (currentEndMs > nextStartMs) {
                conflicts.add(current.showtimeId);
                conflicts.add(next.showtimeId);
            }
        }
    });

    return conflicts;
}

function renderTable(data) {
    const tbody = document.getElementById("showtimeTable");
    if (!tbody) return;
    tbody.innerHTML = "";

    if (!data || data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted py-5"><i class="fa-solid fa-box-open fs-2 mb-2"></i><br>No showtimes found matching your criteria.</td></tr>`;
        return;
    }

    // Sắp xếp Tăng dần theo ID
    data.sort((a, b) => a.showtimeId - b.showtimeId);

    // Kích hoạt máy quét trùng lịch
    const conflicts = findConflicts(data);

    data.forEach(s => {
        let format = s.format;
        let formatBadge = '';
        if(format === "TWO_D") { format = "2D"; formatBadge = "bg-secondary"; }
        else if(format === "THREE_D") { format = "3D"; formatBadge = "bg-primary"; }
        else { format = "IMAX"; formatBadge = "bg-dark text-warning"; }

        let statusBadge = s.status === 'ACTIVE' ? 'bg-success' : 'bg-danger';

        // Giao diện cảnh báo đỏ nều phát hiện trùng lịch
        const isConflict = conflicts.has(s.showtimeId);
        const rowClass = isConflict ? "table-danger" : "";
        const conflictWarning = isConflict ? `<div class="text-danger mt-1" style="font-size: 0.75rem; font-weight: bold;"><i class="fa-solid fa-triangle-exclamation"></i> CẢNH BÁO: Trùng lịch / Thiếu giờ dọn!</div>` : "";

        tbody.innerHTML += `
            <tr class="${rowClass}">
                <td class="align-middle">
                    <span class="badge bg-light text-secondary border shadow-sm px-2 py-1">ST-ID: #${s.showtimeId}</span>
                </td>
                <td class="align-middle">
                    <span class="badge bg-info-subtle text-info border border-info px-2 py-1 mb-1">Movie ID: ${s.movieId || 'N/A'}</span><br>
                    <span class="fw-bold ${isConflict ? 'text-danger' : 'text-dark'}">${s.movieTitle || "-"}</span>
                    ${conflictWarning}
                </td>
                <td class="fw-semibold text-primary align-middle"><i class="fa-solid fa-door-open me-1"></i>${s.theaterRoomName || "-"}</td>
                <td class="text-muted align-middle"><i class="fa-regular fa-clock me-1"></i>${formatDate(s.startTime)}</td>
                <td class="text-muted align-middle"><i class="fa-solid fa-flag-checkered me-1"></i>${formatDate(s.endTime)}</td>
                <td class="fw-bold text-success align-middle">${formatPrice(s.price)} ₫</td>
                <td class="align-middle"><span class="badge ${formatBadge} px-2 py-1">${format}</span></td>
                <td class="align-middle"><span class="badge ${statusBadge} px-2 py-1">${s.status}</span></td>
                <td class="text-end pe-4 align-middle">
                    <button class="btn tech-btn tech-btn-outline px-3 py-1 shadow-sm" style="font-size: 0.75rem;" onclick="edit(${s.showtimeId})">
                        <i class="fa-solid fa-pen-to-square"></i> Edit
                    </button>
                </td>
            </tr>
        `;
    });
}

function edit(id) {
    window.location.href = `/admin/showtimes/update?id=${id}`;
}

function formatDate(dt) {
    if (!dt) return "";
    let d = new Date(dt);
    return d.toLocaleString("vi-VN", { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function formatPrice(price) {
    return new Intl.NumberFormat("vi-VN").format(price);
}

function checkSuccessToast() {
    const message = sessionStorage.getItem("successMessage");
    if (message) {
        if(typeof showToast === 'function') {
            showToast(message, "success");
        } else {
            alert(message);
        }
        sessionStorage.removeItem("successMessage");
    }
}