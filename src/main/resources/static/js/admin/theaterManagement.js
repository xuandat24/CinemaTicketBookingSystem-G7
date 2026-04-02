(function () {
    const ROOM_API = "/api/theater-rooms";
    const SHOWTIME_API = "/api/showtimes";

    const roomSelectEl = document.getElementById("roomSelect");
    const roomHelpTextEl = document.getElementById("roomHelpText");
    const selectedRoomBadgeEl = document.getElementById("selectedRoomBadge");
    const showtimeListEl = document.getElementById("showtimeList");
    const seatLayoutSectionEl = document.getElementById("seatLayoutSection");
    const selectedShowtimeMetaEl = document.getElementById("selectedShowtimeMeta");
    const theaterRoomLabelEl = document.getElementById("theater-room-label");
    const seatGridEl = document.getElementById("seat-grid");
    const bookedOverTotalEl = document.getElementById("bookedOverTotal");
    const normalOverTotalEl = document.getElementById("normalOverTotal");
    const vipOverTotalEl = document.getElementById("vipOverTotal");
    const pairOverTotalEl = document.getElementById("pairOverTotal");
    const selectedMovieInfoEl = document.getElementById("selectedMovieInfo");

    if (!roomSelectEl || !showtimeListEl || !seatGridEl) {
        return;
    }

    const roomsById = new Map();
    let activeSeatLayout = null;

    const seatLayoutProfiles = {
        1: {
            shiftX: function (_rowIndex, colIndex) {
                if (colIndex <= 1) return -40;
                if (colIndex >= 10) return 40;
                return 0;
            }
        },
        2: {
            shiftX: function (rowIndex, colIndex) {
                const direction = colIndex <= 5 ? -1 : 1;
                const centerOffset = 30 * direction;
                const edgeBoost = Math.max(0, Math.abs(colIndex - 5.5) - 2) * 5 * direction;
                const rowDepthBoost = (rowIndex / 11) * 6 * direction;
                return centerOffset + edgeBoost + rowDepthBoost;
            }
        },
        3: {
            shiftX: function (_rowIndex, colIndex) {
                const practicalOffsets = [-36, -36, -12, -12, -12, -12, 12, 12, 12, 12, 36, 36];
                return practicalOffsets[colIndex] || 0;
            }
        }
    };

    document.addEventListener("DOMContentLoaded", init);

    async function init() {
        roomSelectEl.addEventListener("change", onRoomChange);
        await loadRooms();
    }

    async function loadRooms() {
        roomSelectEl.disabled = true;
        setRoomHelpText("Loading theater rooms...");

        try {
            const response = await fetch(ROOM_API);
            if (!response.ok) {
                throw new Error("Cannot load theater rooms");
            }

            const rooms = await response.json();
            roomSelectEl.innerHTML = '<option value="">-- Choose Theater Room --</option>';
            rooms.forEach((room) => {
                const option = document.createElement("option");
                option.value = String(room.roomId);
                option.textContent = room.roomName || ("Room " + room.roomId);
                roomSelectEl.appendChild(option);
                roomsById.set(String(room.roomId), room);
            });

            setRoomHelpText("Select a room to load available showtimes.");
        } catch (error) {
            console.error(error);
            setRoomHelpText("Failed to load theater rooms. Please refresh.");
        } finally {
            roomSelectEl.disabled = false;
        }
    }

    async function onRoomChange() {
        const roomId = roomSelectEl.value;
        hideSeatSection();
        resetOccupancyText();

        if (!roomId) {
            selectedRoomBadgeEl.textContent = "Room --";
            renderShowtimeMessage("Please choose a theater room to continue.");
            setRoomHelpText("Select a room to load available showtimes.");
            return;
        }

        const selectedRoom = roomsById.get(roomId);
        selectedRoomBadgeEl.textContent = selectedRoom && selectedRoom.roomName ? selectedRoom.roomName : ("Room " + roomId);
        await loadShowtimesByRoom(roomId);
    }

    async function loadShowtimesByRoom(roomId) {
        renderShowtimeMessage("Loading available showtimes...");
        setRoomHelpText("Fetching showtimes for selected room...");

        try {
            const response = await fetch(SHOWTIME_API + "/room/" + encodeURIComponent(roomId));
            if (!response.ok) {
                throw new Error("Cannot load showtimes");
            }

            const showtimes = await response.json();
            const availableShowtimes = (Array.isArray(showtimes) ? showtimes : [])
                .filter(isAvailableShowtime)
                .sort((a, b) => {
                    const timeA = parseDateTime(a.startTime);
                    const timeB = parseDateTime(b.startTime);
                    return (timeA ? timeA.getTime() : 0) - (timeB ? timeB.getTime() : 0);
                });

            if (availableShowtimes.length === 0) {
                renderShowtimeMessage("No available showtimes found for this theater room.");
                setRoomHelpText("This room currently has no available showtimes.");
                return;
            }

            renderShowtimes(availableShowtimes);
            setRoomHelpText("Choose a showtime to view seat layout and occupancy.");
        } catch (error) {
            console.error(error);
            renderShowtimeMessage("Unable to load showtimes. Please try again.");
            setRoomHelpText("Unable to load showtimes.");
        }
    }

    function isAvailableShowtime(showtime) {
        const status = String(showtime && showtime.status ? showtime.status : "").toUpperCase();
        if (status && status !== "ACTIVE") {
            return false;
        }

        const endTime = parseDateTime(showtime ? showtime.endTime : null);
        if (!endTime) {
            return true;
        }
        return endTime.getTime() >= Date.now();
    }

    function renderShowtimes(showtimes) {
        showtimeListEl.innerHTML = "";
        const fragment = document.createDocumentFragment();

        showtimes.forEach((showtime, index) => {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "showtime-item" + (index === 0 ? " active" : "");

            const timeEl = document.createElement("div");
            timeEl.className = "showtime-time";
            timeEl.textContent = formatShowtimeRange(showtime);

            const movieEl = document.createElement("div");
            movieEl.className = "showtime-movie";
            movieEl.textContent = showtime.movieTitle || "Untitled Movie";

            const metaEl = document.createElement("div");
            metaEl.className = "showtime-meta";
            metaEl.textContent = formatShowtimeMeta(showtime);

            item.appendChild(timeEl);
            item.appendChild(movieEl);
            item.appendChild(metaEl);

            item.addEventListener("click", async function () {
                showtimeListEl.querySelectorAll(".showtime-item").forEach((el) => el.classList.remove("active"));
                item.classList.add("active");
                await loadSeatLayout(showtime);
            });

            fragment.appendChild(item);
        });

        showtimeListEl.appendChild(fragment);

        if (showtimes.length > 0) {
            void loadSeatLayout(showtimes[0]);
        }
    }

    async function loadSeatLayout(showtime) {
        showSeatSection();
        selectedShowtimeMetaEl.textContent = formatShowtimeRange(showtime);
        selectedMovieInfoEl.textContent = "Loading seat data...";
        renderSeatGridMessage("Loading seat layout...");

        try {
            const response = await fetch(
                SHOWTIME_API + "/" + encodeURIComponent(showtime.showtimeId) + "/seats"
            );
            if (!response.ok) {
                throw new Error("Cannot load seat layout");
            }

            const payload = await response.json();
            const seats = Array.isArray(payload.seats) ? payload.seats : [];
            const roomName = payload.roomName || showtime.theaterRoomName || selectedRoomBadgeEl.textContent;

            theaterRoomLabelEl.textContent = roomName || "Theater Room --";
            selectedMovieInfoEl.textContent =
                (payload.movieTitle || showtime.movieTitle || "Movie --") +
                " | " +
                formatShowtimeMeta(showtime);

            const roomId = payload.roomId != null ? payload.roomId : showtime.theaterRoomId;
            applySeatLayoutClass(resolveLayoutRoomId(roomId));
            renderSeatGrid(seats);
            updateOccupancyStats(seats);
        } catch (error) {
            console.error(error);
            renderSeatGridMessage("Unable to load seat layout for this showtime.");
            selectedMovieInfoEl.textContent = "Unable to load movie details.";
            resetOccupancyCounters();
        }
    }

    function renderSeatGrid(seats) {
        seatGridEl.innerHTML = "";

        if (!seats.length) {
            renderSeatGridMessage("No seat data available for this showtime.");
            return;
        }

        const orderedSeats = [...seats].sort((a, b) => {
            const seatA = parseSeatCode(a.seatCode);
            const seatB = parseSeatCode(b.seatCode);
            if (!seatA || !seatB) {
                return String(a.seatCode || "").localeCompare(String(b.seatCode || ""));
            }
            if (seatA.rowIndex !== seatB.rowIndex) {
                return seatA.rowIndex - seatB.rowIndex;
            }
            return seatA.column - seatB.column;
        });

        const fragment = document.createDocumentFragment();
        orderedSeats.forEach((seat) => {
            const seatInfo = parseSeatCode(seat.seatCode);
            if (!seatInfo) {
                return;
            }

            const type = normalizeSeatType(seat.seatType, seatInfo.rowLabel);
            const seatEl = document.createElement("div");
            seatEl.className = "seat " + type.toLowerCase() + (seat.booked ? " booked" : "");
            seatEl.textContent = seatInfo.code;
            seatEl.style.setProperty("--seat-shift-x", getSeatShiftX(seatInfo.rowIndex, seatInfo.column - 1) + "px");
            seatEl.title = seatInfo.code + " - " + type + (seat.booked ? " (Booked)" : " (Available)");
            fragment.appendChild(seatEl);
        });

        if (!fragment.childNodes.length) {
            renderSeatGridMessage("Seat codes are invalid and cannot be rendered.");
            return;
        }

        seatGridEl.appendChild(fragment);
    }

    function updateOccupancyStats(seats) {
        const stats = {
            NORMAL: { total: 0, booked: 0 },
            VIP: { total: 0, booked: 0 },
            PAIR: { total: 0, booked: 0 }
        };

        let totalSeats = 0;
        let bookedSeats = 0;

        seats.forEach((seat) => {
            const seatInfo = parseSeatCode(seat.seatCode);
            const type = normalizeSeatType(seat.seatType, seatInfo ? seatInfo.rowLabel : null);
            const bucket = stats[type] || stats.NORMAL;

            bucket.total += 1;
            totalSeats += 1;

            if (seat.booked) {
                bucket.booked += 1;
                bookedSeats += 1;
            }
        });

        bookedOverTotalEl.textContent = bookedSeats + " / " + totalSeats;
        normalOverTotalEl.textContent = stats.NORMAL.booked + " / " + stats.NORMAL.total;
        vipOverTotalEl.textContent = stats.VIP.booked + " / " + stats.VIP.total;
        pairOverTotalEl.textContent = stats.PAIR.booked + " / " + stats.PAIR.total;
    }

    function resetOccupancyText() {
        resetOccupancyCounters();
        selectedShowtimeMetaEl.textContent = "Showtime --";
        theaterRoomLabelEl.textContent = "Theater Room --";
        selectedMovieInfoEl.textContent = "Choose a showtime to view movie and seat details.";
    }

    function resetOccupancyCounters() {
        bookedOverTotalEl.textContent = "0 / 0";
        normalOverTotalEl.textContent = "0 / 0";
        vipOverTotalEl.textContent = "0 / 0";
        pairOverTotalEl.textContent = "0 / 0";
    }

    function renderShowtimeMessage(message) {
        showtimeListEl.innerHTML = "";
        const messageEl = document.createElement("div");
        messageEl.className = "placeholder-note";
        messageEl.textContent = message;
        showtimeListEl.appendChild(messageEl);
    }

    function renderSeatGridMessage(message) {
        seatGridEl.innerHTML = "";
        seatGridEl.classList.remove("layout-room-1", "layout-room-2", "layout-room-3");
        const errorEl = document.createElement("div");
        errorEl.className = "seat-grid-error";
        errorEl.textContent = message;
        seatGridEl.appendChild(errorEl);
    }

    function showSeatSection() {
        seatLayoutSectionEl.classList.remove("d-none");
    }

    function hideSeatSection() {
        seatLayoutSectionEl.classList.add("d-none");
        seatGridEl.innerHTML = "";
    }

    function setRoomHelpText(text) {
        if (roomHelpTextEl) {
            roomHelpTextEl.textContent = text;
        }
    }

    function resolveLayoutRoomId(value) {
        const parsed = Number.parseInt(value, 10);
        if ([1, 2, 3].includes(parsed)) {
            return parsed;
        }
        return 1;
    }

    function applySeatLayoutClass(roomId) {
        activeSeatLayout = seatLayoutProfiles[roomId] || seatLayoutProfiles[1];
        seatGridEl.classList.remove("layout-room-1", "layout-room-2", "layout-room-3");
        seatGridEl.classList.add("layout-room-" + roomId);
    }

    function getSeatShiftX(rowIndex, colIndex) {
        if (!activeSeatLayout || typeof activeSeatLayout.shiftX !== "function") {
            return 0;
        }
        return activeSeatLayout.shiftX(rowIndex, colIndex);
    }

    function parseSeatCode(seatCode) {
        if (!seatCode || typeof seatCode !== "string") {
            return null;
        }
        const normalized = seatCode.trim().toUpperCase();
        const match = normalized.match(/^([A-Z])(\d{1,2})$/);
        if (!match) {
            return null;
        }

        const rowLabel = match[1];
        const column = Number.parseInt(match[2], 10);
        if (Number.isNaN(column)) {
            return null;
        }

        return {
            code: normalized,
            rowLabel: rowLabel,
            rowIndex: rowLabel.charCodeAt(0) - "A".charCodeAt(0),
            column: column
        };
    }

    function normalizeSeatType(seatType, rowLabel) {
        const normalized = (seatType || "").toString().trim().toUpperCase();
        if (normalized.includes("PAIR")) return "PAIR";
        if (normalized.includes("VIP")) return "VIP";
        if (normalized.includes("NORMAL")) return "NORMAL";

        if (rowLabel === "L") return "PAIR";
        if (rowLabel && "DEFGHIJ".includes(rowLabel)) return "VIP";
        return "NORMAL";
    }

    function parseDateTime(value) {
        if (!value) {
            return null;
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return null;
        }
        return date;
    }

    function formatShowtimeRange(showtime) {
        const startTime = parseDateTime(showtime ? showtime.startTime : null);
        const endTime = parseDateTime(showtime ? showtime.endTime : null);
        if (!startTime) {
            return "Showtime --";
        }

        const dateText = formatDate(startTime);
        const startText = formatTime(startTime);
        const endText = endTime ? formatTime(endTime) : "--:--";
        return dateText + " | " + startText + " - " + endText;
    }

    function formatShowtimeMeta(showtime) {
        const format = formatShowtimeFormat(showtime ? showtime.format : null);
        const price = formatCurrency(showtime ? Number(showtime.price) : NaN);
        return format + " | " + price;
    }

    function formatShowtimeFormat(format) {
        const normalized = String(format || "").toUpperCase();
        if (normalized === "TWO_D") return "2D";
        if (normalized === "THREE_D") return "3D";
        if (normalized === "IMAX") return "IMAX";
        return "N/A";
    }

    function formatCurrency(value) {
        if (!Number.isFinite(value)) {
            return "0 VND";
        }
        return value.toLocaleString("en-US") + " VND";
    }

    function formatDate(date) {
        const day = String(date.getDate()).padStart(2, "0");
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const year = date.getFullYear();
        return day + "/" + month + "/" + year;
    }

    function formatTime(date) {
        const hour = String(date.getHours()).padStart(2, "0");
        const minute = String(date.getMinutes()).padStart(2, "0");
        return hour + ":" + minute;
    }
})();
