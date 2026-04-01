(function () {
    const seatGrid = document.getElementById("seat-grid");
    if (!seatGrid) {
        return;
    }

    const dayBar = document.getElementById("day-bar");
    const showtimeBar = document.getElementById("showtime-bar");
    const seatSection = document.getElementById("seat-section");
    let dayButtons = dayBar ? Array.from(dayBar.querySelectorAll(".day-btn")) : [];
    const timeButtons = showtimeBar ? Array.from(showtimeBar.querySelectorAll(".showtime-btn")) : [];

    const selectedDateEl = document.getElementById("selected-date");
    const selectedTimeEl = document.getElementById("selected-time");
    const selectedSeatsSideEl = document.getElementById("selected-seats-side");
    const totalSideEl = document.getElementById("total-price-side");
    const totalBottomEl = document.getElementById("total-price-bottom");
    const timerEl = document.getElementById("countdown-timer");
    const continueBtn = document.getElementById("continue-btn");
    const theaterRoomLabelEl = document.getElementById("theater-room-label");
    const movieTitleEl = document.getElementById("movie-title");
    const moviePosterEl = document.getElementById("movie-poster");
    const movieGenreEl = document.getElementById("movie-genre");
    const movieDurationEl = document.getElementById("movie-duration");
    const movieCinemaEl = document.getElementById("movie-cinema");

    const queryParams = new URLSearchParams(window.location.search);
    const showtimeId = queryParams.get("showtimeId");
    const roomIdParam = queryParams.get("roomId");
    let roomName = (queryParams.get("roomName") || "").trim();
    const username = (queryParams.get("username") || localStorage.getItem("username") || "").trim();

    let basePrice = 100000;
    let showtimeStartTime = null;
    let seatRecords = [];
    let bookedSeats = new Set();
    let seatIdByCode = new Map();
    let seatPriceFactorByCode = new Map();
    let isSeatDataLoaded = false;

    const selected = new Set();

    if (showtimeBar) {
        showtimeBar.classList.add("is-hidden");
        showtimeBar.classList.remove("is-visible");
        showtimeBar.setAttribute("aria-hidden", "true");
    }

    const seatLayoutProfiles = {
        1: {
            name: "Classic Twin Aisles",
            shiftX: function (_rowIndex, colIndex) {
                if (colIndex <= 1) return -40;
                if (colIndex >= 10) return 40;
                return 0;
            }
        },
        2: {
            name: "Grand Center Aisle",
            shiftX: function (rowIndex, colIndex) {
                const direction = colIndex <= 5 ? -1 : 1;
                const centerOffset = 30 * direction;
                const edgeBoost = Math.max(0, Math.abs(colIndex - 5.5) - 2) * 5 * direction;
                const rowDepthBoost = (rowIndex / 11) * 6 * direction;
                return centerOffset + edgeBoost + rowDepthBoost;
            }
        },
        3: {
            name: "Practical Triple Aisle",
            shiftX: function (_rowIndex, colIndex) {
                const practicalOffsets = [-36, -36, -12, -12, -12, -12, 12, 12, 12, 12, 36, 36];
                return practicalOffsets[colIndex];
            }
        }
    };

    let activeRoomId = resolveLayoutRoomId(roomIdParam);
    let activeSeatLayout = seatLayoutProfiles[activeRoomId] || seatLayoutProfiles[1];

    function resolveLayoutRoomId(value) {
        const parsed = Number.parseInt(value, 10);
        if ([1, 2, 3].includes(parsed)) {
            return parsed;
        }
        return 1;
    }

    function parseSeatCode(seatCode) {
        if (!seatCode || typeof seatCode !== "string") return null;
        const normalized = seatCode.trim().toUpperCase();
        const match = normalized.match(/^([A-Z])(\d{1,2})$/);
        if (!match) return null;

        const rowLabel = match[1];
        const column = Number.parseInt(match[2], 10);
        if (Number.isNaN(column)) return null;

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
        if ("DEFGHIJ".includes(rowLabel)) return "VIP";
        return "NORMAL";
    }

    function applySeatLayoutClass() {
        seatGrid.classList.remove("layout-room-1", "layout-room-2", "layout-room-3");
        seatGrid.classList.add("layout-room-" + activeRoomId);
    }

    function getSeatShiftX(rowIndex, colIndex) {
        return activeSeatLayout.shiftX(rowIndex, colIndex);
    }

    function setActiveButton(buttons, activeButton) {
        buttons.forEach((btn) => btn.classList.remove("active"));
        if (activeButton) {
            activeButton.classList.add("active");
        }
    }

    function showShowtimes() {
        if (showtimeBar) {
            showtimeBar.classList.remove("is-hidden");
            showtimeBar.classList.add("is-visible");
            showtimeBar.setAttribute("aria-hidden", "false");
        }
    }

    function showSeats() {
        if (seatSection) {
            seatSection.classList.remove("is-hidden");
            seatSection.classList.add("is-visible");
            seatSection.setAttribute("aria-hidden", "false");
        }
    }

    function resetSelection() {
        selected.clear();
        seatGrid.querySelectorAll(".seat.selected").forEach((el) => el.classList.remove("selected"));
        updateSummary();
    }

    function formatDateLabel(date) {
        const dd = String(date.getDate()).padStart(2, "0");
        const mm = String(date.getMonth() + 1).padStart(2, "0");
        return dd + "/" + mm;
    }

    function formatTimeLabel(date) {
        const hh = String(date.getHours()).padStart(2, "0");
        const mm = String(date.getMinutes()).padStart(2, "0");
        return hh + ":" + mm;
    }

    function buildDayButtons() {
        if (!dayBar) return;
        const existing = dayBar.querySelectorAll(".day-btn");
        existing.forEach((el) => el.remove());
        const today = new Date();
        for (let i = 0; i < 5; i++) {
            const d = new Date(today);
            d.setDate(today.getDate() + i);
            const btn = document.createElement("button");
            btn.className = "showtime-btn day-btn";
            btn.textContent = formatDateLabel(d);
            btn.dataset.date = d.toISOString().slice(0, 10);
            dayBar.appendChild(btn);
        }
        dayButtons = Array.from(dayBar.querySelectorAll(".day-btn"));
    }

    function formatPrice(value) {
        const safeValue = Number.isFinite(value) ? value : 0;
        return safeValue.toLocaleString("en-US") + " VND";
    }

    function resolveBannerUrl(path) {
        const fallback = "/img/11.jpg";
        if (!path || typeof path !== "string") return fallback;

        let trimmed = path.trim();
        if (!trimmed) return fallback;

        trimmed = trimmed.replace(/\\/g, "/");

        // Some records may store "/uploads/banners/..." while WebConfig serves "/banners/**".
        if (trimmed.startsWith("/uploads/banners/")) {
            trimmed = trimmed.replace("/uploads/banners/", "/banners/");
        } else if (trimmed.startsWith("uploads/banners/")) {
            trimmed = trimmed.replace("uploads/banners/", "/banners/");
        }

        if (/^https?:\/\//i.test(trimmed)) return trimmed;
        if (trimmed.startsWith("/")) return trimmed;
        return "/" + trimmed;
    }

    function updateMovieInfo(payload) {
        const title = (payload && payload.movieTitle ? String(payload.movieTitle).trim() : "") || "Movie";
        const genre = (payload && payload.genre ? String(payload.genre).trim() : "") || "--";
        const duration = Number(payload && payload.duration);
        const cinemaName = (roomName && roomName.trim()) ? roomName.trim() : "Room --";

        if (movieTitleEl) {
            movieTitleEl.textContent = title;
        }
        if (movieGenreEl) {
            movieGenreEl.textContent = genre;
        }
        if (movieDurationEl) {
            movieDurationEl.textContent = Number.isFinite(duration) && duration > 0 ? (duration + " minutes") : "--";
        }
        if (movieCinemaEl) {
            movieCinemaEl.textContent = cinemaName;
        }
        if (moviePosterEl) {
            moviePosterEl.src = resolveBannerUrl(payload ? payload.bannerPath : null);
            moviePosterEl.alt = title + " poster";
            moviePosterEl.onerror = function () {
                moviePosterEl.onerror = null;
                moviePosterEl.src = "/img/11.jpg";
            };
        }
    }

    function updateTheaterRoomLabel() {
        if (!theaterRoomLabelEl) return;
        if (!roomName) {
            theaterRoomLabelEl.textContent = "Theater Room " + activeRoomId;
            return;
        }

        if (/theater room/i.test(roomName)) {
            theaterRoomLabelEl.textContent = roomName;
            return;
        }

        if (/^room\b/i.test(roomName)) {
            theaterRoomLabelEl.textContent = "Theater " + roomName;
            return;
        }

        theaterRoomLabelEl.textContent = "Theater Room " + roomName;
    }

    function setContinueDisabled(disabled) {
        if (!continueBtn) return;
        continueBtn.classList.toggle("disabled", disabled);
        continueBtn.setAttribute("aria-disabled", disabled ? "true" : "false");
        continueBtn.tabIndex = disabled ? -1 : 0;
    }

    function updateContinueLink() {
        if (!continueBtn) return;
        if (!isSeatDataLoaded || !showtimeId) {
            continueBtn.href = "javascript:void(0)";
            setContinueDisabled(true);
            return;
        }

        const selectedSeatCodes = Array.from(selected).sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
        const selectedSeatIds = selectedSeatCodes
            .map((code) => seatIdByCode.get(code))
            .filter((id) => id !== null && id !== undefined);

        if (selectedSeatIds.length === 0) {
            continueBtn.href = "javascript:void(0)";
            setContinueDisabled(true);
            return;
        }

        continueBtn.href =
            "/booking?showtimeId=" + encodeURIComponent(showtimeId) +
            "&seatIds=" + encodeURIComponent(selectedSeatIds.join(",")) +
            "&seatCodes=" + encodeURIComponent(selectedSeatCodes.join(",")) +
            (username ? ("&username=" + encodeURIComponent(username)) : "");

        setContinueDisabled(false);
    }

    function showGridError(message) {
        seatGrid.innerHTML = "";
        seatGrid.classList.remove("layout-room-1", "layout-room-2", "layout-room-3");
        const errorEl = document.createElement("div");
        errorEl.className = "seat-grid-error";
        errorEl.textContent = message;
        seatGrid.appendChild(errorEl);
        isSeatDataLoaded = false;
        setContinueDisabled(true);
        updateContinueLink();
    }

    function updateSummary() {
        const seats = Array.from(selected).sort((a, b) => {
            const parsedA = parseSeatCode(a);
            const parsedB = parseSeatCode(b);
            if (!parsedA || !parsedB) return a.localeCompare(b);
            if (parsedA.rowIndex !== parsedB.rowIndex) return parsedA.rowIndex - parsedB.rowIndex;
            return parsedA.column - parsedB.column;
        });

        const seatText = seats.length ? seats.join(", ") : "Not selected";
        if (selectedSeatsSideEl) {
            selectedSeatsSideEl.textContent = seatText;
        }

        const total = seats.reduce((sum, code) => {
            const factor = seatPriceFactorByCode.get(code) || 1;
            return sum + (basePrice * factor);
        }, 0);

        const totalText = formatPrice(total);
        if (totalSideEl) totalSideEl.textContent = totalText;
        if (totalBottomEl) totalBottomEl.textContent = totalText;
        updateContinueLink();
    }

    function makeSeatButton(code, type, isBooked, rowIndex, colIndex) {
        const btn = document.createElement("div");
        btn.className = "seat " + type.toLowerCase();
        btn.textContent = code;
        btn.dataset.seat = code;
        btn.style.setProperty("--seat-shift-x", getSeatShiftX(rowIndex, colIndex) + "px");

        if (isBooked) {
            btn.classList.add("booked");
            btn.setAttribute("aria-disabled", "true");
            return btn;
        }

        btn.addEventListener("click", function () {
            const seatInfo = parseSeatCode(code);
            if (seatInfo && seatInfo.rowLabel === "L") {
                togglePairSelection(code);
            } else {
                toggleSingleSelection(code);
            }
            updateSummary();
        });
        return btn;
    }

    function toggleSingleSelection(code) {
        const el = seatGrid.querySelector("[data-seat='" + code + "']");
        if (!el) return;
        if (selected.has(code)) {
            selected.delete(code);
            el.classList.remove("selected");
        } else {
            selected.add(code);
            el.classList.add("selected");
        }
    }

    function togglePairSelection(code) {
        const seatInfo = parseSeatCode(code);
        if (!seatInfo) return;

        const pairNum = seatInfo.column % 2 === 1 ? seatInfo.column + 1 : seatInfo.column - 1;
        const pairCode = "L" + pairNum;
        if (bookedSeats.has(code) || bookedSeats.has(pairCode)) {
            return;
        }

        const codes = [code, pairCode];
        const allSelected = codes.every((c) => selected.has(c));
        codes.forEach((c) => {
            const el = seatGrid.querySelector("[data-seat='" + c + "']");
            if (!el) return;
            if (allSelected) {
                selected.delete(c);
                el.classList.remove("selected");
            } else {
                selected.add(c);
                el.classList.add("selected");
            }
        });
    }

    function renderSeats() {
        const fragment = document.createDocumentFragment();
        seatGrid.innerHTML = "";

        if (!seatRecords.length) {
            showGridError("No seat data available for this showtime.");
            return;
        }

        seatGrid.setAttribute("data-layout-name", activeSeatLayout.name);
        applySeatLayoutClass();

        const orderedSeats = [...seatRecords].sort((a, b) => {
            const parsedA = parseSeatCode(a.seatCode);
            const parsedB = parseSeatCode(b.seatCode);
            if (!parsedA || !parsedB) return (a.seatCode || "").localeCompare(b.seatCode || "");
            if (parsedA.rowIndex !== parsedB.rowIndex) return parsedA.rowIndex - parsedB.rowIndex;
            return parsedA.column - parsedB.column;
        });

        orderedSeats.forEach((seat) => {
            const seatInfo = parseSeatCode(seat.seatCode);
            if (!seatInfo) return;

            const code = seatInfo.code;
            const type = normalizeSeatType(seat.seatType, seatInfo.rowLabel);
            const isBooked = Boolean(seat.booked) || bookedSeats.has(code);
            fragment.appendChild(makeSeatButton(code, type, isBooked, seatInfo.rowIndex, seatInfo.column - 1));
        });

        if (!fragment.childNodes.length) {
            showGridError("Seat codes in database are invalid for rendering.");
            return;
        }

        seatGrid.appendChild(fragment);
        updateSummary();
    }

    async function hydrateSeatDataFromApi() {
        if (!showtimeId) {
            showGridError("Missing showtimeId in URL. Example: /movie-seats?showtimeId=1");
            return false;
        }

        try {
            const response = await fetch("/api/showtimes/" + encodeURIComponent(showtimeId) + "/seats");
            if (!response.ok) {
                if (response.status === 404) {
                    showGridError("Showtime not found. Please use a valid showtimeId.");
                } else {
                    showGridError("Failed to load seat data from server.");
                }
                return false;
            }

            const payload = await response.json();
            activeRoomId = resolveLayoutRoomId(String(payload.roomId || roomIdParam || ""));
            activeSeatLayout = seatLayoutProfiles[activeRoomId] || seatLayoutProfiles[1];

            if (payload.roomName) {
                roomName = String(payload.roomName).trim();
            }

            const parsedBasePrice = Number(payload.basePrice);
            if (!Number.isNaN(parsedBasePrice) && parsedBasePrice > 0) {
                basePrice = parsedBasePrice;
            }

            if (payload.startTime) {
                const parsedStart = new Date(payload.startTime);
                if (!Number.isNaN(parsedStart.getTime())) {
                    showtimeStartTime = parsedStart;
                }
            }

            seatRecords = Array.isArray(payload.seats) ? payload.seats : [];
            bookedSeats = new Set();
            seatIdByCode = new Map();
            seatPriceFactorByCode = new Map();

            seatRecords.forEach((seat) => {
                const seatInfo = parseSeatCode(seat.seatCode);
                if (!seatInfo) return;

                seatIdByCode.set(seatInfo.code, seat.seatId);
                const parsedFactor = Number(seat.priceFactor);
                seatPriceFactorByCode.set(seatInfo.code, Number.isNaN(parsedFactor) ? 1 : parsedFactor);
                if (seat.booked) {
                    bookedSeats.add(seatInfo.code);
                }
            });

            updateMovieInfo(payload);
            isSeatDataLoaded = true;
            return true;
        } catch (error) {
            console.warn("Unable to load seats from API:", error);
            showGridError("Could not load seats. Please refresh and try again.");
            return false;
        }
    }

    function updateSelectedDate(label) {
        if (selectedDateEl) {
            selectedDateEl.textContent = label || "--/--";
        }
    }

    function updateSelectedTime(label) {
        if (selectedTimeEl) {
            selectedTimeEl.textContent = label || "--:--";
        }
    }

    function applyShowtimeDateTime() {
        if (!showtimeStartTime) {
            updateSelectedDate("--/--");
            updateSelectedTime("--:--");
            return;
        }

        updateSelectedDate(formatDateLabel(showtimeStartTime));
        updateSelectedTime(formatTimeLabel(showtimeStartTime));
    }

    function startCountdown(initialSeconds) {
        if (!timerEl) return;
        let remaining = initialSeconds;
        function tick() {
            const min = String(Math.floor(remaining / 60)).padStart(2, "0");
            const sec = String(remaining % 60).padStart(2, "0");
            timerEl.textContent = min + ":" + sec;
            if (remaining > 0) {
                remaining -= 1;
            }
        }
        tick();
        setInterval(tick, 1000);
    }

    function bindTimeControls() {
        dayButtons.forEach((btn) => {
            btn.addEventListener("click", function () {
                setActiveButton(dayButtons, btn);
                showShowtimes();
                setActiveButton(timeButtons, null);
                if (seatSection) {
                    seatSection.classList.add("is-hidden");
                    seatSection.classList.remove("is-visible");
                    seatSection.setAttribute("aria-hidden", "true");
                }
                resetSelection();
                updateSelectedDate(btn.textContent);
                updateSelectedTime("--:--");
            });
        });

        timeButtons.forEach((btn) => {
            btn.addEventListener("click", function () {
                setActiveButton(timeButtons, btn);
                showSeats();
                updateSelectedTime(btn.textContent);
            });
        });
    }

    async function initSeatPage() {
        buildDayButtons();
        await hydrateSeatDataFromApi();
        updateTheaterRoomLabel();
        applyShowtimeDateTime();
        startCountdown(5 * 60 + 11);
        bindTimeControls();

        if (isSeatDataLoaded) {
            renderSeats();
            setContinueDisabled(selected.size === 0);
        } else {
            updateSummary();
        }
    }

    initSeatPage();
})();

