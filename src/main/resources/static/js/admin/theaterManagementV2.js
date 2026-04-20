(function () {
    const ROOM_API = "/api/theater-rooms";
    const SHOWTIME_API = "/api/showtimes";

    const roomsById = new Map();
    let activeSeatLayout = null;

    const seatLayoutProfiles = {
        1: { shiftX: (_rowIndex, colIndex) => { if (colIndex <= 1) return -40; if (colIndex >= 10) return 40; return 0; } },
        2: { shiftX: (rowIndex, colIndex) => { const dir = colIndex <= 5 ? -1 : 1; return (30 * dir) + (Math.max(0, Math.abs(colIndex - 5.5) - 2) * 5 * dir) + ((rowIndex / 11) * 6 * dir); } },
        3: { shiftX: (_rowIndex, colIndex) => [-36, -36, -12, -12, -12, -12, 12, 12, 12, 12, 36, 36][colIndex] || 0 }
    };

    function setSafeText(id, text) {
        const el = document.getElementById(id);
        if (el) el.textContent = text;
    }

    function setSafeHTML(id, html) {
        const el = document.getElementById(id);
        if (el) el.innerHTML = html;
    }

    document.addEventListener("DOMContentLoaded", init);

    async function init() {
        const roomSelectEl = document.getElementById("roomSelect");
        if (roomSelectEl) {
            roomSelectEl.addEventListener("change", onRoomChange);
        }
        await loadRooms();
    }

    async function loadRooms() {
        const roomSelectEl = document.getElementById("roomSelect");
        if (!roomSelectEl) return;

        roomSelectEl.disabled = true;
        setSafeHTML("roomHelpText", `<i class="fa-solid fa-circle-info me-1 text-primary"></i> Connecting to database...`);

        try {
            const token = sessionStorage.getItem("jwtToken");
            const response = await fetch(ROOM_API, { method: 'GET', headers: { 'Authorization': 'Bearer ' + token } });
            if (!response.ok) throw new Error("Cannot load theater rooms");

            const rooms = await response.json();
            roomSelectEl.innerHTML = '<option value="">-- Choose Theater Room --</option>';
            rooms.forEach((room) => {
                const option = document.createElement("option");
                option.value = String(room.roomId);
                option.textContent = room.roomName || ("Room " + room.roomId);
                roomSelectEl.appendChild(option);
                roomsById.set(String(room.roomId), room);
            });
            setSafeHTML("roomHelpText", `<i class="fa-solid fa-circle-info me-1 text-primary"></i> Select a room to load available showtimes.`);
        } catch (error) {
            setSafeHTML("roomHelpText", `<i class="fa-solid fa-circle-info me-1 text-danger"></i> Connection failed. Please check network.`);
            if(typeof showToast === 'function') showToast("Không thể tải danh sách Phòng chiếu!", "error");
        } finally {
            roomSelectEl.disabled = false;
        }
    }

    async function onRoomChange() {
        const roomSelectEl = document.getElementById("roomSelect");
        const roomId = roomSelectEl ? roomSelectEl.value : null;

        hideSeatSection();
        resetOccupancyText();

        if (!roomId) {
            setSafeHTML("selectedRoomBadge", '<i class="fa-solid fa-desktop me-1"></i> Room --');
            renderShowtimeMessage('<i class="fa-solid fa-magnifying-glass me-2"></i> Waiting for room selection...');
            setSafeHTML("roomHelpText", `<i class="fa-solid fa-circle-info me-1 text-primary"></i> Select a room to load available showtimes.`);
            return;
        }

        const selectedRoom = roomsById.get(roomId);
        setSafeHTML("selectedRoomBadge", '<i class="fa-solid fa-desktop me-1"></i> ' + (selectedRoom.roomName || ("Room " + roomId)));
        await loadShowtimesByRoom(roomId);
    }

    async function loadShowtimesByRoom(roomId) {
        renderShowtimeMessage('<i class="fa-solid fa-spinner fa-spin me-2 text-primary"></i> Fetching active showtimes...');
        setSafeHTML("roomHelpText", `<i class="fa-solid fa-circle-info me-1 text-primary"></i> Fetching schedule from server...`);

        try {
            const token = sessionStorage.getItem("jwtToken");

            // FIX LỖI BACKEND: Đổi từ API "/room/{id}" bị lỗi sang API "/search?roomId={id}" để luôn nhận một MẢNG kết quả
            const url = SHOWTIME_API + "/search?roomId=" + encodeURIComponent(roomId);
            const response = await fetch(url, {
                method: 'GET', headers: { 'Authorization': 'Bearer ' + token }
            });

            if (!response.ok) throw new Error("Cannot load showtimes");

            const showtimes = await response.json();

            // FIX LỖI ẨN DATA FRONTEND: Hủy bỏ bộ lọc filter(isAvailableShowtime) để hiển thị TOÀN BỘ dữ liệu (kể cả phim đã chiếu xong)
            const availableShowtimes = (Array.isArray(showtimes) ? showtimes : []).sort((a, b) => {
                const tA = parseDateTime(a.startTime); const tB = parseDateTime(b.startTime);
                return (tA ? tA.getTime() : 0) - (tB ? tB.getTime() : 0);
            });

            if (availableShowtimes.length === 0) {
                renderShowtimeMessage('<i class="fa-solid fa-box-open fs-4 d-block mb-2 text-muted"></i> No showtimes assigned to this room.');
                setSafeHTML("roomHelpText", `<i class="fa-solid fa-circle-info me-1 text-primary"></i> This room is currently empty.`);
                return;
            }

            renderShowtimes(availableShowtimes);
            setSafeHTML("roomHelpText", `<i class="fa-solid fa-circle-info me-1 text-primary"></i> Click on a showtime card to view live seating map.`);
        } catch (error) {
            renderShowtimeMessage('<i class="fa-solid fa-triangle-exclamation me-2 text-danger"></i> Failed to retrieve schedule. Backend Error!');
            if(typeof showToast === 'function') showToast("Lỗi lấy dữ liệu từ Server!", "error");
        }
    }

    function renderShowtimes(showtimes) {
        const showtimeListEl = document.getElementById("showtimeList");
        if (!showtimeListEl) return;

        showtimeListEl.innerHTML = "";
        const fragment = document.createDocumentFragment();

        showtimes.forEach((showtime, index) => {
            const item = document.createElement("div");
            item.className = "showtime-item" + (index === 0 ? " active" : "");

            item.innerHTML = `
                <div class="showtime-time"><i class="fa-regular fa-clock me-1"></i> ${formatShowtimeRange(showtime)}</div>
                <div class="showtime-movie">${showtime.movieTitle || "Untitled Movie"}</div>
                <div class="showtime-meta"><i class="fa-solid fa-tags me-1"></i> ${formatShowtimeMeta(showtime)}</div>
            `;

            item.addEventListener("click", async function () {
                showtimeListEl.querySelectorAll(".showtime-item").forEach((el) => el.classList.remove("active"));
                item.classList.add("active");
                await loadSeatLayout(showtime);
            });
            fragment.appendChild(item);
        });

        showtimeListEl.appendChild(fragment);
        if (showtimes.length > 0) void loadSeatLayout(showtimes[0]);
    }

    async function loadSeatLayout(showtime) {
        showSeatSection();
        setSafeHTML("selectedShowtimeMeta", '<i class="fa-regular fa-clock me-1"></i> ' + formatShowtimeRange(showtime));
        setSafeHTML("selectedMovieInfo", '<i class="fa-solid fa-spinner fa-spin me-2 text-primary"></i> Fetching seat statuses...');
        renderSeatGridMessage('<i class="fa-solid fa-spinner fa-spin me-2 text-primary"></i> Synchronizing seat map...');

        try {
            const token = sessionStorage.getItem("jwtToken");
            const response = await fetch(SHOWTIME_API + "/" + encodeURIComponent(showtime.showtimeId) + "/seats", {
                method: 'GET', headers: { 'Authorization': 'Bearer ' + token }
            });

            if (!response.ok) throw new Error("Cannot load seat layout");

            const payload = await response.json();
            const seats = Array.isArray(payload.seats) ? payload.seats : [];

            let roomNameFallback = "Theater Room --";
            const selectedRoomBadgeEl = document.getElementById("selectedRoomBadge");
            if(selectedRoomBadgeEl) roomNameFallback = selectedRoomBadgeEl.textContent.replace('Room ', '').trim();
            const roomName = payload.roomName || showtime.theaterRoomName || roomNameFallback;

            setSafeText("theater-room-label", roomName);
            setSafeHTML("selectedMovieInfo", `<div class="fw-bold text-dark fs-5 mb-1">${payload.movieTitle || showtime.movieTitle || "Movie --"}</div><span class="badge bg-success">${formatShowtimeMeta(showtime)}</span>`);

            const roomId = payload.roomId != null ? payload.roomId : showtime.theaterRoomId;
            applySeatLayoutClass(resolveLayoutRoomId(roomId));
            renderSeatGrid(seats);
            updateOccupancyStats(seats);
        } catch (error) {
            renderSeatGridMessage('<i class="fa-solid fa-triangle-exclamation text-danger fs-3 d-block mb-2"></i> Map Sync Failed.');
            setSafeHTML("selectedMovieInfo", '<i class="fa-solid fa-circle-xmark text-danger me-2"></i> Data unavailable.');
            resetOccupancyCounters();
            if(typeof showToast === 'function') showToast("Lỗi đồng bộ Sơ đồ ghế!", "error");
        }
    }

    function renderSeatGrid(seats) {
        const seatGridEl = document.getElementById("seat-grid");
        if (!seatGridEl) return;

        seatGridEl.innerHTML = "";
        if (!seats.length) { renderSeatGridMessage("No seats configured for this room."); return; }

        const orderedSeats = [...seats].sort((a, b) => {
            const sA = parseSeatCode(a.seatCode); const sB = parseSeatCode(b.seatCode);
            if (!sA || !sB) return String(a.seatCode || "").localeCompare(String(b.seatCode || ""));
            return sA.rowIndex !== sB.rowIndex ? sA.rowIndex - sB.rowIndex : sA.column - sB.column;
        });

        const fragment = document.createDocumentFragment();
        orderedSeats.forEach((seat) => {
            const info = parseSeatCode(seat.seatCode);
            if (!info) return;

            const type = normalizeSeatType(seat.seatType, info.rowLabel);
            const seatEl = document.createElement("div");
            seatEl.className = "seat " + type.toLowerCase() + (seat.booked ? " booked" : "");
            seatEl.innerHTML = seat.booked ? '<i class="fa-solid fa-user"></i>' : info.code;
            seatEl.style.setProperty("--seat-shift-x", getSeatShiftX(info.rowIndex, info.column - 1) + "px");
            seatEl.title = info.code + " - " + type + (seat.booked ? " (Booked)" : " (Available)");
            fragment.appendChild(seatEl);
        });

        if (!fragment.childNodes.length) { renderSeatGridMessage("Invalid seat configuration."); return; }
        seatGridEl.appendChild(fragment);
    }

    function updateOccupancyStats(seats) {
        const stats = { NORMAL: { t: 0, b: 0 }, VIP: { t: 0, b: 0 }, PAIR: { t: 0, b: 0 } };
        let totalSeats = 0, bookedSeats = 0;

        seats.forEach((seat) => {
            const info = parseSeatCode(seat.seatCode);
            const type = normalizeSeatType(seat.seatType, info ? info.rowLabel : null);
            const bucket = stats[type] || stats.NORMAL;
            bucket.t += 1; totalSeats += 1;
            if (seat.booked) { bucket.b += 1; bookedSeats += 1; }
        });

        setSafeText("bookedValue", bookedSeats);
        setSafeText("capacityValue", totalSeats);

        let percentage = totalSeats === 0 ? 0 : Math.round((bookedSeats / totalSeats) * 100);
        setSafeText("occupancyPercentage", percentage + "%");

        const progressBar = document.getElementById("occupancyProgressBar");
        if (progressBar) progressBar.style.width = percentage + "%";

        setSafeText("normalOverTotal", stats.NORMAL.b + " / " + stats.NORMAL.t);
        setSafeText("vipOverTotal", stats.VIP.b + " / " + stats.VIP.t);
        setSafeText("pairOverTotal", stats.PAIR.b + " / " + stats.PAIR.t);
    }

    function resetOccupancyText() {
        resetOccupancyCounters();
        setSafeHTML("selectedShowtimeMeta", "Showtime --");
        setSafeText("theater-room-label", "Theater Room --");
        setSafeHTML("selectedMovieInfo", '<i class="fa-solid fa-film me-2"></i> Select a showtime to view info.');
    }

    function resetOccupancyCounters() {
        setSafeText("bookedValue", "0");
        setSafeText("capacityValue", "0");
        setSafeText("occupancyPercentage", "0%");

        const progressBar = document.getElementById("occupancyProgressBar");
        if (progressBar) progressBar.style.width = "0%";

        setSafeText("normalOverTotal", "0 / 0");
        setSafeText("vipOverTotal", "0 / 0");
        setSafeText("pairOverTotal", "0 / 0");
    }

    function renderShowtimeMessage(msg) { setSafeHTML("showtimeList", `<div class="placeholder-note">${msg}</div>`); }

    function renderSeatGridMessage(msg) {
        const seatGridEl = document.getElementById("seat-grid");
        if(seatGridEl) {
            seatGridEl.innerHTML = `<div class="seat-grid-error">${msg}</div>`;
            seatGridEl.className = "seat-grid";
        }
    }

    function showSeatSection() {
        const el = document.getElementById("seatLayoutSection");
        if(el) el.classList.remove("d-none");
    }

    function hideSeatSection() {
        const el = document.getElementById("seatLayoutSection");
        if(el) el.classList.add("d-none");
        setSafeHTML("seat-grid", "");
    }

    function resolveLayoutRoomId(val) { const p = Number.parseInt(val, 10); return [1, 2, 3].includes(p) ? p : 1; }

    function applySeatLayoutClass(roomId) {
        activeSeatLayout = seatLayoutProfiles[roomId] || seatLayoutProfiles[1];
        const seatGridEl = document.getElementById("seat-grid");
        if(seatGridEl) seatGridEl.className = "seat-grid layout-room-" + roomId;
    }

    function getSeatShiftX(r, c) { return activeSeatLayout && activeSeatLayout.shiftX ? activeSeatLayout.shiftX(r, c) : 0; }

    function parseSeatCode(code) {
        if (!code || typeof code !== "string") return null;
        const match = code.trim().toUpperCase().match(/^([A-Z])(\d{1,2})$/);
        if (!match) return null;
        const col = Number.parseInt(match[2], 10);
        return isNaN(col) ? null : { code: match[0], rowLabel: match[1], rowIndex: match[1].charCodeAt(0) - 65, column: col };
    }

    function normalizeSeatType(type, label) {
        const n = (type || "").toString().trim().toUpperCase();
        if (n.includes("PAIR")) return "PAIR"; if (n.includes("VIP")) return "VIP"; if (n.includes("NORMAL")) return "NORMAL";
        if (label === "L") return "PAIR"; if (label && "DEFGHIJ".includes(label)) return "VIP"; return "NORMAL";
    }

    function parseDateTime(val) { if (!val) return null; const d = new Date(val); return isNaN(d.getTime()) ? null : d; }
    function formatShowtimeRange(st) {
        const start = parseDateTime(st ? st.startTime : null); const end = parseDateTime(st ? st.endTime : null);
        if (!start) return "Showtime --";
        return formatDate(start) + " | " + formatTime(start) + " - " + (end ? formatTime(end) : "--:--");
    }
    function formatShowtimeMeta(st) { return formatShowtimeFormat(st ? st.format : null) + " | " + formatCurrency(st ? Number(st.price) : NaN); }
    function formatShowtimeFormat(f) { const n = String(f || "").toUpperCase(); return n === "TWO_D" ? "2D" : n === "THREE_D" ? "3D" : n === "IMAX" ? "IMAX" : "N/A"; }
    function formatCurrency(v) { return Number.isFinite(v) ? v.toLocaleString("vi-VN") + " VND" : "0 VND"; }
    function formatDate(d) { return String(d.getDate()).padStart(2, "0") + "/" + String(d.getMonth() + 1).padStart(2, "0") + "/" + d.getFullYear(); }
    function formatTime(d) { return String(d.getHours()).padStart(2, "0") + ":" + String(d.getMinutes()).padStart(2, "0"); }
})();