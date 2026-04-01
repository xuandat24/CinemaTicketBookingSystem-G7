const API = "/api/showtimes";

const urlParams = new URLSearchParams(window.location.search);
const id = urlParams.get("id");

document.getElementById("price")
    .addEventListener("input", formatPriceInput);

/* ================= LOAD DETAIL ================= */

async function loadShowtime() {
    try {
        const response = await fetch(`${API}/${id}`);
        const data = await response.json();

        document.getElementById("movieTitle").value = data.movieTitle;
        document.getElementById("roomName").value = data.theaterRoomName;

        document.getElementById("movieId").value = data.movieId;
        document.getElementById("roomId").value = data.theaterRoomId;
        document.getElementById("startTime").value = data.startTime.substring(0, 16);
        document.getElementById("price").value =
            new Intl.NumberFormat("vi-VN").format(data.price);
        document.getElementById("format").value = data.format;
        document.getElementById("status").value = data.status;

    } catch (error) {
        console.error("Cannot load showtime:", error);
    }
}

/* ================= UPDATE ================= */

document
    .getElementById("editShowtimeForm")
    .addEventListener("submit", async function (e) {

        e.preventDefault();

        const rawPrice = document.getElementById("price").value;

        const priceNumber = Number(rawPrice.replace(/\./g, ""));

        const message = document.getElementById("message");

        if (!priceNumber || priceNumber < 10000) {
            message.innerHTML = `
                <div class="alert alert-danger">
                    Price must be greater than 10,000
                </div>
            `;
            return;
        }

        const data = {
            movieId: document.getElementById("movieId").value,
            theaterRoomId: document.getElementById("roomId").value,
            startTime: document.getElementById("startTime").value,
            price: priceNumber,
            format: document.getElementById("format").value,
            status: document.getElementById("status").value
        };

        try {
            const response = await fetch(`${API}/${id}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(data)
            });

            if (response.ok) {

                sessionStorage.setItem("successMessage", "Showtime updated successfully");
                window.location.href = "/admin/showtimes";

            } else {

                let errorMessage = "Update failed";

                try {
                    const error = await response.json();
                    errorMessage = error.message || errorMessage;
                } catch (e) {}

                message.innerHTML = `
                <div class="alert alert-danger">
                    ${errorMessage}
                </div>
            `;
            }

        } catch (err) {
            console.error(err);
        }
    });

function formatPriceInput(e) {
    let value = e.target.value;
    value = value.replace(/\D/g, "");
    value = new Intl.NumberFormat("vi-VN").format(value);
    e.target.value = value;
}

document.getElementById("price").addEventListener("input", function (e) {
    let value = e.target.value.replace(/\D/g, ""); // bỏ hết ký tự không phải số

    if (value) {
        value = Number(value).toLocaleString("vi-VN"); // format 100.000
    }

    e.target.value = value;
});

/* ================= INIT ================= */

window.onload = async function () {
    await loadShowtime();
};