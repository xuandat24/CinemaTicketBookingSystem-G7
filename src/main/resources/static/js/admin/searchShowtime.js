const API = "/api/showtimes";

document.addEventListener("DOMContentLoaded", () => {
    showSuccessMessage();
    loadAll();
});

function loadAll() {
    const token = localStorage.getItem('jwtToken');
    fetch(API, {
        headers: { "Authorization": `Bearer ${token}` }
    })
        .then(res => res.json())
        .then(renderTable);
}

function applyFilter() {
    const date = document.getElementById("filterDate").value;

    if (!date) return loadAll();

    // SỬA Ở ĐÂY: Thêm Token vào quá trình gọi API lọc theo ngày
    const token = localStorage.getItem('jwtToken');
    fetch(`${API}/date?date=${date}`, {
        method: "GET",
        headers: {
            "Authorization": `Bearer ${token}`
        }
    })
        .then(res => res.json())
        .then(renderTable);
}

function renderTable(data) {
    const tbody = document.getElementById("showtimeTable");
    tbody.innerHTML = "";

    if (!data || data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center">No showtimes found</td></tr>`;
        return;
    }

    data.forEach(s => {
        let format = s.format; // Đã thêm 'let' để khai báo biến đúng chuẩn
        if(format === "TWO_D") {
            format = "2D";
        } else if(format === "THREE_D") {
            format = "3D";
        } else {
            format = "IMAX";
        }
        tbody.innerHTML += `
            <tr>
                <td>${s.showtimeId}</td>
                <td>${s.movieTitle || "-"}</td>
                <td>${s.theaterRoomName || "-"}</td>
                <td>${formatDate(s.startTime)}</td>
                <td>${formatDate(s.endTime)}</td>
                <td>${s.price}</td>
                <td>${format}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-warning"
                        onclick="edit(${s.showtimeId})">Edit</button>
                    <button class="btn btn-sm btn-danger"
                        onclick="del(${s.showtimeId})">Delete</button>
                </td>
            </tr>
        `;
    });
}

function edit(id) {
    window.location.href = `/admin/showtimes/update?id=${id}`;
}

function del(id) {
    if (!confirm("Delete this showtime?")) return;
    const token = localStorage.getItem('jwtToken');
    fetch(`${API}/${id}`, {
        method: "DELETE",
        headers: { "Authorization": `Bearer ${token}` }
    })
        .then(res => {
            if (res.ok) loadAll();
            else alert("Delete failed");
        });
}

function formatDate(dt) {
    return dt ? dt.replace("T", " ") : "";
}

function showSuccessMessage() {

    const message = sessionStorage.getItem("successMessage");

    if (message) {

        const container = document.getElementById("message");

        container.innerHTML = `
            <div class="alert alert-success">
                ${message}
            </div>
        `;

        // xóa sau khi hiển thị (tránh reload vẫn hiện)
        sessionStorage.removeItem("successMessage");
    }
}