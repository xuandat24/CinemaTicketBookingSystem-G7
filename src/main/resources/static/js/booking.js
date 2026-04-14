document.addEventListener("DOMContentLoaded", function () {
    const pageData = window.bookingPageData || {};
    const combos = Array.isArray(pageData.combos) ? pageData.combos : [];
    const showtimeId = pageData.showtimeId || 0;
    const seatIds = Array.isArray(pageData.seatIds) ? pageData.seatIds : [];
    const userId = pageData.userId ?? null;

    let seatPrice = 0;
    const totalEl = document.getElementById("totalPrice");
    if (totalEl) {
        seatPrice = parseInt(totalEl.dataset.seatPrice, 10) || 0;
    }

    const comboPrice = {};
    const comboQty = {};
    combos.forEach((c) => {
        comboPrice[c.id] = c.price || 0;
        comboQty[c.id] = 0;
    });

    function formatVN(number) {
        return Number(number || 0).toLocaleString("vi-VN");
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

        const totalPriceEl = document.getElementById("totalPrice");
        const finalPriceEl = document.getElementById("finalPrice");
        if (totalPriceEl) totalPriceEl.innerText = formatVN(total);
        if (finalPriceEl) finalPriceEl.innerText = formatVN(final);
    }

    window.changeCombo = function changeCombo(id, change) {
        if (!comboQty[id]) comboQty[id] = 0;

        comboQty[id] += change;
        if (comboQty[id] < 0) comboQty[id] = 0;

        const el = document.getElementById("combo" + id);
        if (el) el.innerText = comboQty[id];

        updateTotal();
    };

    window.applyCoupon = function applyCoupon() {
        const couponInput = document.getElementById("couponInput");
        const couponCode = couponInput ? couponInput.value.trim() : "";
        const originalPriceText = document.getElementById("totalPrice")?.innerText || "0";
        const originalPrice = parseFloat(originalPriceText.replace(/\./g, "")) || 0;

        if (!couponCode) {
            alert("Please enter a coupon code.");
            return;
        }

        fetch(`/api/booking/check-coupon?code=${encodeURIComponent(couponCode)}&amount=${originalPrice}`)
            .then((res) => res.json())
            .then((data) => {
                const msgElement = document.getElementById("couponMessage");
                if (!msgElement) return;

                if (data.success) {
                    msgElement.innerText = "Coupon applied successfully!";
                    msgElement.style.color = "green";

                    const discountAmount = data.discountAmount || 0;
                    const finalPrice = Math.max(0, originalPrice - discountAmount);

                    document.getElementById("discount").innerText = formatVN(discountAmount);
                    document.getElementById("finalPrice").innerText = formatVN(finalPrice);

                    window.appliedCouponCode = couponCode;

                    if (couponInput) couponInput.readOnly = true;
                    const clearBtn = document.getElementById("clearCouponBtn");
                    if (clearBtn) clearBtn.style.display = "block";
                } else {
                    msgElement.innerText = data.message || "Invalid coupon.";
                    msgElement.style.color = "red";
                }
            })
            .catch((err) => {
                console.error(err);
                alert("Cannot validate coupon right now.");
            });
    };

    window.clearCoupon = function clearCoupon() {
        const couponInput = document.getElementById("couponInput");
        const originalPriceText = document.getElementById("totalPrice")?.innerText || "0";

        if (couponInput) {
            couponInput.value = "";
            couponInput.readOnly = false;
        }

        const msgElement = document.getElementById("couponMessage");
        if (msgElement) msgElement.innerText = "";

        const discountEl = document.getElementById("discount");
        if (discountEl) discountEl.innerText = "0";

        const finalEl = document.getElementById("finalPrice");
        if (finalEl) finalEl.innerText = originalPriceText;

        window.appliedCouponCode = null;

        const clearBtn = document.getElementById("clearCouponBtn");
        if (clearBtn) clearBtn.style.display = "none";
    };

    window.goBack = function goBack() {
        if (document.referrer) {
            history.back();
        } else {
            window.location.href = "/";
        }
    };

    let time = 600;
    const timerId = setInterval(function () {
        const minutes = Math.floor(time / 60);
        const seconds = time % 60;

        const timerEl = document.getElementById("timer");
        if (timerEl) {
            timerEl.innerText = minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }

        if (time <= 0) {
            clearInterval(timerId);
            window.location.href = "/";
            return;
        }

        time--;
    }, 1000);

    const nextBtn = document.querySelector(".btn-next");
    const termsOverlay = document.getElementById("termsOverlay");
    const termsClose = document.getElementById("termsClose");
    const agreeCheckbox = document.getElementById("agreeCheckbox");
    const payButton = document.getElementById("payButton");

    function openTermsModal() {
        if (!termsOverlay || !agreeCheckbox || !payButton) return;
        agreeCheckbox.checked = false;
        payButton.disabled = true;
        termsOverlay.classList.add("is-open");
        termsOverlay.setAttribute("aria-hidden", "false");
    }

    function closeTermsModal() {
        if (!termsOverlay) return;
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

        const finalPriceText = document.getElementById("finalPrice")?.innerText || "0";
        const finalPrice = parseFloat(finalPriceText.replace(/\./g, "")) || 0;

        return {
            showtimeId: showtimeId,
            seatIds: seatIds,
            userId: userId,
            finalPrice: finalPrice,
            combos: selectedCombos,
            couponCode: window.appliedCouponCode || null
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

    if (agreeCheckbox && payButton) {
        agreeCheckbox.addEventListener("change", function () {
            payButton.disabled = !agreeCheckbox.checked;
        });

        payButton.addEventListener("click", function () {
            if (!agreeCheckbox.checked) return;

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

            sessionStorage.setItem("bookingData", JSON.stringify(payload));

            fetch("/api/payment/create-payment", {
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
                    if (data.success && data.paymentUrl) {
                        window.location.href = data.paymentUrl;
                    } else {
                        alert("Payment creation failed: " + (data.message || "Unknown error"));
                        payButton.disabled = false;
                    }
                })
                .catch((err) => {
                    console.error(err);
                    alert("Payment creation failed: " + err.message);
                    payButton.disabled = false;
                });
        });
    }
});