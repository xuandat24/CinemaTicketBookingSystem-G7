document.addEventListener("DOMContentLoaded", function () {

    // ====== DATA ======
    let seatPrice = 0

    const totalEl = document.getElementById("totalPrice")

    if (totalEl) {
        seatPrice = parseInt(totalEl.dataset.seatPrice) || 0
    }

    if (typeof comboPrice === "undefined") comboPrice = {}
    if (typeof comboQty === "undefined") comboQty = {}

    // ====== CHANGE COMBO ======
    function changeCombo(id, change) {

        if (!comboQty[id]) comboQty[id] = 0

        comboQty[id] += change
        if (comboQty[id] < 0) comboQty[id] = 0

        let el = document.getElementById("combo" + id)
        if (el) el.innerText = comboQty[id]

        updateTotal()
    }

    window.changeCombo = changeCombo

    // ====== FORMAT VN ======
    function formatVN(number) {
        return number.toLocaleString("vi-VN")
    }

    // ====== PARSE NUMBER ======
    function parseNumber(str) {
        return parseInt(str.replace(/\D/g, "")) || 0
    }

    // ====== UPDATE TOTAL ======
    function updateTotal() {

        let comboTotal = 0

        for (let id in comboQty) {
            comboTotal += comboQty[id] * (comboPrice[id] || 0)
        }

        let discountEl = document.getElementById("discount")
        let discount = discountEl ? parseNumber(discountEl.innerText) : 0

        let total = seatPrice + comboTotal
        let final = total - discount
        if (final < 0) final = 0

        document.getElementById("totalPrice").innerText = formatVN(total)
        document.getElementById("finalPrice").innerText = formatVN(final)
    }

    // ====== POINT INPUT VALIDATION ======
    let pointInput = document.getElementById("pointInput")

    if (pointInput) {

        pointInput.addEventListener("keydown", function (e) {
            if (["e", "E", "+", "-", "."].includes(e.key)) {
                e.preventDefault()
            }
        })

        pointInput.addEventListener("input", function () {
            let value = this.value

            value = value.replace(/[^0-9]/g, "")

            this.value = value
        })
    }

    function applyPoint() {

        let point = parseInt(pointInput.value) || 0
        let currentPoint = parseInt(document.getElementById("currentPoint").innerText) || 0

        if (point < 0) {
            alert("Invalid point!")
            pointInput.value = 0
            return
        }

        if (point > currentPoint) {
            alert("Not enough points!")
            return
        }

        let discount = point * 1000

        document.getElementById("discountMoney").innerText = formatVN(discount)
        document.getElementById("discount").innerText = formatVN(discount)

        updateTotal()
    }

    window.applyPoint = applyPoint

    // ====== TIMER ======
    let time = 600

    setInterval(function () {

        let minutes = Math.floor(time / 60)
        let seconds = time % 60

        let timerEl = document.getElementById("timer")

        if (timerEl) {
            timerEl.innerText =
                minutes + ":" + (seconds < 10 ? "0" : "") + seconds
        }

        time--

    }, 1000)

})