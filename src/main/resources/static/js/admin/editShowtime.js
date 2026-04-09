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

                const error = await response.json();

                let html = "";

                // if there are multiple validation errors
                if (error.errors && error.errors.length > 0) {
                    html = error.errors.map(e => `
                        <div class="alert alert-danger">
                            ${e.message || e.defaultMessage}
                        </div>
                    `).join("");
                } else {
                    // fallback if no error list
                    html = `
                        <div class="alert alert-danger">
                            ${error.message || "Update failed"}
                        </div>
                    `;
                }

                message.innerHTML = html;
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