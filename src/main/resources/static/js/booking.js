document.addEventListener("DOMContentLoaded", function () {

    // ====== DATA ======
    let seatPrice = 100000

    // đảm bảo tồn tại
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

    // ⚠️ QUAN TRỌNG: expose ra global cho HTML gọi
    window.changeCombo = changeCombo

    // ====== UPDATE TOTAL ======
    function updateTotal() {

        let comboTotal = 0

        for (let id in comboQty) {
            comboTotal += comboQty[id] * (comboPrice[id] || 0)
        }

        let discount = parseInt(document.getElementById("discount")?.innerText) || 0

        let total = seatPrice + comboTotal
        let final = total - discount

        document.getElementById("totalPrice").innerText = total
        document.getElementById("finalPrice").innerText = final
    }

    // ====== POINT ======
    let pointInput = document.getElementById("pointInput")

    if (pointInput) {
        pointInput.addEventListener("input", function () {

            let point = parseInt(this.value) || 0
            let discount = point * 1000

            document.getElementById("discountMoney").innerText = discount
            document.getElementById("discount").innerText = discount

            updateTotal()
        })
    }

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