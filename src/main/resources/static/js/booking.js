document.addEventListener("DOMContentLoaded", function () {
    let seatPrice = 0;

    const totalEl = document.getElementById("totalPrice");
    if (totalEl) {
        seatPrice = parseInt(totalEl.dataset.seatPrice, 10) || 0;
    }

    const pageData = window.bookingPageData || {};
    const combos = Array.isArray(pageData.combos) ? pageData.combos : [];
    const showtimeId = pageData.showtimeId || 0;
    const seatIds = Array.isArray(pageData.seatIds) ? pageData.seatIds : [];
    const userId = pageData.userId ?? null;

    const comboPrice = {};
    const comboQty = {};

    combos.forEach((c) => {
        comboPrice[c.id] = c.price || 0;
        comboQty[c.id] = 0;
    });

    function changeCombo(id, change) {
        if (!comboQty[id]) comboQty[id] = 0;

        comboQty[id] += change;
        if (comboQty[id] < 0) comboQty[id] = 0;

        const el = document.getElementById("combo" + id);
        if (el) el.innerText = comboQty[id];

        updateTotal();
    }

    window.changeCombo = changeCombo;

    function formatVN(number) {
        return number.toLocaleString("vi-VN");
    }

    function parseNumber(str) {
        return parseInt(String(str || "").replace(/\D/g, ""), 10) || 0;
    }

    function updateTotal() {
        let comboTotal = 0;

        for (const id in comboQty) {
            comboTotal += comboQty[id] * (comboPrice[id] || 0);
        }

        const discountEl = document.getElementById("discount");
        const discount = discountEl ? parseNumber(discountEl.innerText) : 0;

        const total = seatPrice + comboTotal;
        let final = total - discount;
        if (final < 0) final = 0;

        document.getElementById("totalPrice").innerText = formatVN(total);
        document.getElementById("finalPrice").innerText = formatVN(final);
    }

    const pointInput = document.getElementById("pointInput");

    if (pointInput) {
        pointInput.addEventListener("keydown", function (e) {
            if (["e", "E", "+", "-", "."].includes(e.key)) {
                e.preventDefault();
            }
        });

        pointInput.addEventListener("input", function () {
            this.value = this.value.replace(/[^0-9]/g, "");
        });
    }

    function applyPoint() {
        const point = parseInt(pointInput.value, 10) || 0;
        const currentPoint = parseInt(document.getElementById("currentPoint").innerText, 10) || 0;

        if (point < 0) {
            alert("Invalid point!");
            pointInput.value = 0;
            return;
        }

        if (point > currentPoint) {
            alert("Not enough points!");
            return;
        }

        const discount = point * 1000;

        document.getElementById("discountMoney").innerText = formatVN(discount);
        document.getElementById("discount").innerText = formatVN(discount);

        updateTotal();
    }

    window.applyPoint = applyPoint;

    let time = 600;
    setInterval(function () {
        const minutes = Math.floor(time / 60);
        const seconds = time % 60;

        const timerEl = document.getElementById("timer");
        if (timerEl) {
            timerEl.innerText = minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }

        time--;
    }, 1000);

    const nextBtn = document.querySelector(".btn-next");
    const termsOverlay = document.getElementById("termsOverlay");
    const termsClose = document.getElementById("termsClose");
    const agreeCheckbox = document.getElementById("agreeCheckbox");
    const payButton = document.getElementById("payButton");

    function openTermsModal() {
        agreeCheckbox.checked = false;
        payButton.disabled = true;
        termsOverlay.classList.add("is-open");
        termsOverlay.setAttribute("aria-hidden", "false");
    }

    function closeTermsModal() {
        termsOverlay.classList.remove("is-open");
        termsOverlay.setAttribute("aria-hidden", "true");
    }

    function collectPayload() {
        const selectedCombos = [];

        combos.forEach((c) => {
            const comboElement = document.getElementById("combo" + c.id);
            if (!comboElement) return;

            const qty = parseInt(comboElement.innerText, 10);
            if (qty > 0) {
                selectedCombos.push({ comboId: c.id, quantity: qty });
            }
        });

        const finalPriceText = document.getElementById("finalPrice").innerText;
        const finalPrice = parseFloat(finalPriceText.replace(/\./g, ""));

        return {
            showtimeId: showtimeId,
            seatIds: seatIds,
            userId: userId,
            finalPrice: finalPrice,
            combos: selectedCombos
        };
    }

    if (nextBtn) {
        nextBtn.addEventListener("click", function () {
            openTermsModal();
        });
    }

    if (termsClose) {
        termsClose.addEventListener("click", function () {
            closeTermsModal();
        });
    }

    if (termsOverlay) {
        termsOverlay.addEventListener("click", function (event) {
            if (event.target === termsOverlay) {
                closeTermsModal();
            }
        });
    }

    if (agreeCheckbox) {
        agreeCheckbox.addEventListener("change", function () {
            payButton.disabled = !agreeCheckbox.checked;
        });
    }

    if (payButton) {
        payButton.addEventListener("click", function () {
            if (!agreeCheckbox.checked) {
                return;
            }

            const payload = collectPayload();
            payButton.disabled = true;

            const token = localStorage.getItem("jwtToken");
            const headers = {
                "Content-Type": "application/json",
                "Accept": "application/json"
            };
            if (token) {
                headers["Authorization"] = "Bearer " + token;
            }

            fetch("/api/booking/confirm", {
                method: "POST",
                credentials: "include",
                headers: headers,
                body: JSON.stringify(payload)
            })
                .then(async (res) => {
                    if (res.redirected && res.url.includes("/login")) {
                        throw new Error("Session expired. Please login again.");
                    }

                    if (!res.ok) {
                        const errorText = await res.text();
                        throw new Error("HTTP " + res.status + ": " + (errorText || res.statusText));
                    }

                    const contentType = res.headers.get("content-type") || "";
                    if (!contentType.includes("application/json")) {
                        const raw = await res.text();
                        throw new Error("Unexpected response: " + raw);
                    }

                    return res.json();
                })
                .then((data) => {
                    if (data.success) {
                        alert("Payment and booking completed successfully!");
                        window.location.href = "/booking/history" + (localStorage.getItem("username") ? ("?username=" + encodeURIComponent(localStorage.getItem("username"))) : "");
                    } else {
                        alert("Booking failed: " + (data.message || "Unknown error"));
                        payButton.disabled = false;
                    }
                })
                .catch((err) => {
                    console.error(err);
                    alert("Booking failed: " + err.message);
                    payButton.disabled = false;
                });
        });
    }

    function goBack() {
        if (document.referrer) {
            history.back();
        } else {
            window.location.href = "/";
        }
    }

    window.goBack = goBack;
});
