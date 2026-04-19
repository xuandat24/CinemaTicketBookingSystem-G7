document.addEventListener("DOMContentLoaded", function () {

    // ====== DATA KHỞI TẠO ======
    let seatPrice = 0;
    const totalEl = document.getElementById("totalPrice");
    if (totalEl) {
        seatPrice = parseInt(totalEl.dataset.seatPrice) || 0;
    }

    if (typeof window.comboPrice === "undefined") window.comboPrice = {};
    if (typeof window.comboQty === "undefined") window.comboQty = {};
    if (typeof window.availableCouponsList === "undefined") window.availableCouponsList = [];

    function initComboDataFromDom() {
        const rows = document.querySelectorAll('tr[data-combo-id][data-combo-price]');
        rows.forEach((row) => {
            const comboId = parseInt(row.getAttribute('data-combo-id'), 10);
            const comboPrice = parseInt(row.getAttribute('data-combo-price'), 10);
            if (!Number.isFinite(comboId)) return;

            window.comboPrice[comboId] = Number.isFinite(comboPrice) ? comboPrice : 0;
            if (typeof window.comboQty[comboId] === "undefined") {
                window.comboQty[comboId] = 0;
            }
        });
    }

    function initCouponDataFromDom() {
        const sourceItems = document.querySelectorAll('#couponDataSource .coupon-source');
        if (!sourceItems.length) return;

        window.availableCouponsList = Array.from(sourceItems).map((item) => ({
            code: (item.getAttribute('data-code') || '').trim(),
            discountType: (item.getAttribute('data-discount-type') || 'FIXED').trim(),
            discountValue: parseFloat(item.getAttribute('data-discount-value')) || 0,
            minOrderAmount: parseFloat(item.getAttribute('data-min-order')) || 0,
            description: item.getAttribute('data-description') || ''
        }));
    }

    initComboDataFromDom();
    initCouponDataFromDom();

    window.appliedCouponCode = null;
    window.currentDiscountValue = 0;

    // ====== HÀM TIỆN ÍCH ======
    function formatVN(number) {
        return number.toLocaleString("vi-VN");
    }

    function parseNumber(str) {
        return parseInt(str.replace(/\D/g, "")) || 0;
    }

    // ====== TĂNG GIẢM COMBO ======
    window.changeCombo = function (id, change) {
        if (!window.comboQty[id]) window.comboQty[id] = 0;

        window.comboQty[id] += change;
        if (window.comboQty[id] < 0) window.comboQty[id] = 0;

        let el = document.getElementById("combo" + id);
        if (el) el.innerText = window.comboQty[id];

        updateTotal();
    }

    // ====== RENDER DANH SÁCH COUPON ======
    function renderCoupons(currentTotal) {
        const container = document.getElementById("coupon-list-container");
        if (!container || !window.availableCouponsList) return;

        container.innerHTML = window.availableCouponsList.map(c => {
            const minOrder = c.minOrderAmount || 0;
            const isEligible = currentTotal >= minOrder;

            // Xử lý Giao diện nút bấm
            const borderClass = isEligible ? 'border-success bg-light' : 'border-secondary bg-white opacity-50';
            const btnClass = isEligible ? 'btn-success' : 'btn-outline-secondary';
            const disableAttr = isEligible ? '' : 'disabled';
            const minOrderText = minOrder > 0
                ? `<small class="d-block mt-1 ${isEligible ? 'text-success' : 'text-danger'}"><i class="fa fa-info-circle"></i> Min order: ${formatVN(minOrder)} VND</small>`
                : '';

            return `
                <div class="p-3 border rounded d-flex justify-content-between align-items-center shadow-sm ${borderClass} transition-all">
                    <div>
                        <strong class="text-dark fs-5">${c.code}</strong>
                        <span class="badge ${c.discountType === 'PERCENTAGE' ? 'bg-warning text-dark' : 'bg-primary'} ms-2">
                            ${c.discountType === 'PERCENTAGE' ? c.discountValue + '%' : formatVN(c.discountValue) + 'đ'} OFF
                        </span>
                        <small class="d-block text-muted mt-1">${c.description || 'Special discount for you'}</small>
                        ${minOrderText}
                    </div>
                    <button type="button" class="btn btn-sm ${btnClass} px-3 fw-bold" ${disableAttr}
                            onclick="document.getElementById('couponInput').value='${c.code}'; window.applyCoupon();">
                        USE
                    </button>
                </div>
            `;
        }).join('');
    }

    // ====== HÀM TÍNH TỔNG ĐỘNG (TRÁI TIM CỦA LOGIC) ======
    window.updateTotal = async function () {
        let comboTotal = 0;
        for (let id in window.comboQty) {
            comboTotal += window.comboQty[id] * (window.comboPrice[id] || 0);
        }

        // 1. Cập nhật Total Gốc
        let total = seatPrice + comboTotal;
        document.getElementById("totalPrice").innerText = formatVN(total);

        // 2. Render lại danh sách mã (Để sáng/tối nút bấm theo giá mới)
        renderCoupons(total);

        // 3. Nếu đang áp mã, phải check ngầm lại với Server xem còn thỏa mãn không (Ví dụ lỡ bớt Combo làm rớt Min Order)
        if (window.appliedCouponCode) {
            try {
                const response = await fetch(`/api/public/coupons/check?code=${window.appliedCouponCode}&orderValue=${total}`);
                const result = await response.json();

                if (result.success) {
                    // Update lại tiền giảm (Phòng trường hợp là mã % thì tiền giảm sẽ thay đổi theo Combo)
                    window.currentDiscountValue = result.discountAmount;
                } else {
                    alert("Coupon is no longer valid for this order amount!");
                    window.clearCoupon();
                    return; // Lệnh clear sẽ tự gọi lại updateTotal nên ta thoát hàm ở đây
                }
            } catch (e) {
                console.error("Coupon re-validation error", e);
            }
        }

        // 4. Cập nhật Final Price
        let final = total - window.currentDiscountValue;
        if (final < 0) final = 0;

        document.getElementById("discount").innerText = "- " + formatVN(window.currentDiscountValue);
        document.getElementById("finalPrice").innerText = formatVN(final);
    }

    // ====== HÀM XỬ LÝ NÚT APPLY ======
    window.applyCoupon = async function () {
        const couponInputEl = document.getElementById("couponInput");
        if (!couponInputEl) return;

        const codeInput = couponInputEl.value.trim().toUpperCase();
        if (!codeInput) {
            alert("Please enter a coupon code!");
            return;
        }

        const totalStr = document.getElementById("totalPrice").innerText;
        const currentOriginalPrice = parseNumber(totalStr);

        try {
            const response = await fetch(`/api/public/coupons/check?code=${codeInput}&orderValue=${currentOriginalPrice}`);
            const result = await response.json();

            if (result.success) {
                window.appliedCouponCode = codeInput;
                window.currentDiscountValue = result.discountAmount;

                document.getElementById("clearCouponBtn").style.display = "block";
                couponInputEl.disabled = true;

                updateTotal();

                const msgEl = document.getElementById('couponMessage');
                if (msgEl) {
                    msgEl.innerText = "Coupon applied successfully!";
                    msgEl.style.color = "green";
                }
            } else {
                const msgEl = document.getElementById('couponMessage');
                if (msgEl) {
                    msgEl.innerText = result.message;
                    msgEl.style.color = "red";
                } else alert(result.message);

                window.appliedCouponCode = null;
                window.currentDiscountValue = 0;
            }
        } catch (error) {
            alert("Error checking coupon!");
        }
    }

    // ====== HÀM XỬ LÝ NÚT CLEAR ======
    window.clearCoupon = function () {
        const couponInputEl = document.getElementById("couponInput");
        window.appliedCouponCode = null;
        window.currentDiscountValue = 0;

        couponInputEl.value = "";
        couponInputEl.disabled = false;

        document.getElementById("clearCouponBtn").style.display = "none";
        document.getElementById("discount").innerText = "0";

        const msgEl = document.getElementById('couponMessage');
        if (msgEl) msgEl.innerText = "";

        updateTotal();
    }

    // ====== KHỞI ĐỘNG CÁC CHỨC NĂNG CƠ BẢN ======

    // Nút mở Modal Thanh Toán
    const btnNext = document.querySelector('.btn-next');
    const termsOverlay = document.getElementById('termsOverlay');
    const termsClose = document.getElementById('termsClose');
    const agreeCheckbox = document.getElementById('agreeCheckbox');
    const payButton = document.getElementById('payButton');

    if (btnNext && termsOverlay) {
        btnNext.addEventListener('click', () => termsOverlay.classList.add('is-open'));
    }
    if (termsClose && termsOverlay) {
        termsClose.addEventListener('click', () => termsOverlay.classList.remove('is-open'));
    }
    if (agreeCheckbox && payButton) {
        agreeCheckbox.addEventListener('change', (e) => payButton.disabled = !e.target.checked);
    }


    // GỬI DATA XUỐNG SERVER
    // GỬI DATA XUỐNG SERVER
    if (payButton) {
        payButton.addEventListener('click', async function () {
            payButton.disabled = true;
            payButton.innerHTML = 'Processing...';

            const urlParams = new URLSearchParams(window.location.search);
            const showtimeId = urlParams.get('showtimeId');
            const seatIdsStr = urlParams.get('seatIds');

            let seatIds = [];
            if (seatIdsStr) {
                seatIds = seatIdsStr.split(',').filter(id => id.trim() !== '').map(Number);
            }

            let combos = [];
            for (let id in window.comboQty) {
                if (window.comboQty[id] > 0) {
                    combos.push({comboId: parseInt(id), quantity: window.comboQty[id]});
                }
            }

            const requestData = {
                showtimeId: parseInt(showtimeId),
                seatIds: seatIds,
                combos: combos,
                couponCode: window.appliedCouponCode || null
            };

            try {
                const token = sessionStorage.getItem('jwtToken');

                const response = await fetch('/api/payment/create-payment', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify(requestData)
                });
                const result = await response.json();

                if (result.success) {
                    // Trả về nguyên bản: Chỉ chuyển hướng đến VNPay, không cấy Cookie hay làm gì khác
                    window.location.href = result.paymentUrl;
                } else {
                    alert("Payment error: " + result.message);
                    payButton.disabled = false;
                    payButton.innerHTML = 'PAY';
                }
            } catch (error) {
                alert("Server error!");
                payButton.disabled = false;
                payButton.innerHTML = 'PAY';
            }


        });
    }
    // TIMER
    let time = 600;
    setInterval(function () {
        let minutes = Math.floor(time / 60);
        let seconds = time % 60;
        let timerEl = document.getElementById("timer");
        if (timerEl) timerEl.innerText = minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        time--;
    }, 1000);

    // Chạy hàm tính tiền lần đầu tiên
    window.updateTotal();
});
