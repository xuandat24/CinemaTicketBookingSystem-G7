/* Mock seat selection behavior */
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
    const queryParams = new URLSearchParams(window.location.search);
    const showtimeId = queryParams.get("showtimeId");
    const username = (queryParams.get("username") || localStorage.getItem("username") || "").trim();

    if (showtimeBar) {
        showtimeBar.classList.add("is-hidden");
        showtimeBar.classList.remove("is-visible");
        showtimeBar.setAttribute("aria-hidden", "true");
    }

    const basePrice = 100000;
    const priceFactor = {
        NORMAL: 1.0,
        VIP: 1.5,
        PAIR: 1.25
    };

    const bookedSeats = new Set([
        "A1", "A2", "A3", "B5", "C7", "D10", "E12", "F9", "G14", "H3", "I16", "J6", "K11", "L5", "L6"
    ]);

    const selected = new Set();

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
        return value.toLocaleString("vi-VN") + "đ";
    }

    function seatCodeToId(code) {
        const row = code.charCodeAt(0) - "A".charCodeAt(0);
        const col = parseInt(code.slice(1), 10);
        if (row < 0 || Number.isNaN(col)) {
            return null;
        }
        return row * 12 + col;
    }

    function updateContinueLink() {
        if (!continueBtn) return;

        const selectedSeatCodes = Array.from(selected).sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
        const selectedSeatIds = selectedSeatCodes
            .map((code) => seatCodeToId(code))
            .filter((id) => id !== null);

        if (!showtimeId || selectedSeatCodes.length === 0) {
            continueBtn.href = username ? ("/booking?username=" + encodeURIComponent(username)) : "/booking";
            return;
        }

        continueBtn.href =
            "/booking?showtimeId=" + encodeURIComponent(showtimeId) +
            "&seatIds=" + encodeURIComponent(selectedSeatIds.join(",")) +
            "&seatCodes=" + encodeURIComponent(selectedSeatCodes.join(",")) +
            (username ? ("&username=" + encodeURIComponent(username)) : "");
    }

    function updateSummary() {
        const seats = Array.from(selected).sort((a, b) => {
            const rowA = a.charCodeAt(0);
            const rowB = b.charCodeAt(0);
            if (rowA !== rowB) return rowA - rowB;
            return parseInt(a.slice(1), 10) - parseInt(b.slice(1), 10);
        });

        const seatText = seats.length ? seats.join(", ") : "Chưa chọn";
        if (selectedSeatsSideEl) {
            selectedSeatsSideEl.textContent = seatText;
        }

        let total = 0;
        seats.forEach((code) => {
            const row = code[0];
            const type = getSeatType(row);
            total += basePrice * priceFactor[type];
        });

        const totalText = formatPrice(total);
        if (totalSideEl) totalSideEl.textContent = totalText;
        if (totalBottomEl) totalBottomEl.textContent = totalText;
        updateContinueLink();
    }

    function getSeatType(row) {
        if (row === "L") {
            return "PAIR";
        }
        if ("DEFGHIJ".includes(row)) {
            return "VIP";
        }
        return "NORMAL";
    }

    function makeSeatButton(code, type, isBooked) {
        const btn = document.createElement("div");
        btn.className = "seat " + type.toLowerCase();
        btn.textContent = code;
        btn.dataset.seat = code;

        if (isBooked) {
            btn.classList.add("booked");
            btn.setAttribute("aria-disabled", "true");
            return btn;
        }

        btn.addEventListener("click", function () {
            const row = code[0];
            if (row === "L") {
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
        const num = parseInt(code.slice(1), 10);
        const pairNum = num % 2 === 1 ? num + 1 : num - 1;
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
        const rows = "ABCDEFGHIJKL".split("");
        const cols = 12;
        const fragment = document.createDocumentFragment();

        rows.forEach((row) => {
            for (let col = 1; col <= cols; col++) {
                const code = row + col;
                const type = getSeatType(row);
                const isBooked = bookedSeats.has(code);
                fragment.appendChild(makeSeatButton(code, type, isBooked));
            }
        });

        seatGrid.appendChild(fragment);
        updateSummary();
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

    buildDayButtons();
    updateSelectedDate("--/--");
    updateSelectedTime("--:--");
    startCountdown(5 * 60 + 11);
    updateContinueLink();

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

    renderSeats();
})();
