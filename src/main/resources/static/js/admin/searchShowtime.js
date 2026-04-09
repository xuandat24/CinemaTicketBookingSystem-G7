const API = "/api/showtimes";
const MOVIE_API = "/api/admin/movies";
const ROOM_API = "/api/theater-rooms";

let debounceTimer;

function debounceFilter() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        applyFilter();
    }, 400); // delay 400ms
}

document.addEventListener("DOMContentLoaded", () => {
    showSuccessMessage();
    loadAll();
    loadRooms();

    // Realtime filter
    document.getElementById("filterId")
        .addEventListener("input", debounceFilter);

    document.getElementById("filterRoom")
        .addEventListener("change", applyFilter);

    document.getElementById("filterDate")
        .addEventListener("change", applyFilter);

    document.getElementById("filterKeyword")
        .addEventListener("input", debounceFilter);
});

function loadAll() {
    fetch(API)
        .then(res => res.json())
        .then(renderTable);
}

function loadRooms() {
   fetch(ROOM_API)
       .then(res => res.json())
       .then(data => {
           const select = document.getElementById("filterRoom");
           data.forEach(r => {
               select.innerHTML += `<option value="${r.roomId}">${r.roomName}</option>`;
           });
       });
}




function applyFilter() {

    const id = document.getElementById("filterId").value;
    const roomId = document.getElementById("filterRoom").value;
    const date = document.getElementById("filterDate").value;
    const keyword = document.getElementById("filterKeyword").value;

    // ưu tiên ID (search riêng)
    if (id && id.trim() !== "") {

        document.getElementById("filterMovie").value = "";
        document.getElementById("filterRoom").value = "";
        document.getElementById("filterDate").value = "";

        fetch(`${API}/${id}`)
            .then(res => {
                if (!res.ok) throw new Error();
                return res.json();
            })
            .then(data => renderTable([data]))
            .catch(() => renderTable([]));
        return;
    }

    // build query
    let query = [];

    if (roomId) query.push(`roomId=${roomId}`);
    if (date) query.push(`date=${date}`);
    if (keyword) query.push(`keyword=${encodeURIComponent(keyword)}`);

    const url = `${API}/search?${query.join("&")}`;

    fetch(url)
        .then(res => res.json())
        .then(renderTable);
}

function resetFilter() {
    document.getElementById("filterId").value = "";
    document.getElementById("filterKeyword").value = "";
    document.getElementById("filterRoom").value = "";
    document.getElementById("filterDate").value = "";
    loadAll();
}

function renderTable(data) {
    const tbody = document.getElementById("showtimeTable");
    tbody.innerHTML = "";

    if (!data || data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center">No showtimes found</td></tr>`;
        return;
    }

    data.forEach(s => {
        format = s.format
        if(format == "TWO_D") {
            format = "2D"
        } else if(format == "THREE_D") {
            format = "3D"
        } else {
            format = "IMAX"
        }
        tbody.innerHTML += `
            <tr>
                <td>${s.showtimeId}</td>
                <td>${s.movieTitle || "-"}</td>
                <td>${s.theaterRoomName || "-"}</td>
                <td>${formatDate(s.startTime)}</td>
                <td>${formatDate(s.endTime)}</td>
                <td>${formatPrice(s.price)}</td>
                <td>${format}</td>
                <td>${s.status}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-warning"
                        onclick="edit(${s.showtimeId})">Edit</button>
                </td>
            </tr>
        `;
    });
}

function edit(id) {
    window.location.href = `/admin/showtimes/update?id=${id}`;
}

function formatDate(dt) {
    return dt ? dt.replace("T", " ") : "";
}

function formatPrice(price) {
    return new Intl.NumberFormat("vi-VN").format(price);
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