const API = "/api/showtimes";

const urlParams = new URLSearchParams(window.location.search);
const id = urlParams.get("id");

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
        document.getElementById("price").value = data.price;
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

        const data = {
            movieId: document.getElementById("movieId").value,
            theaterRoomId: document.getElementById("roomId").value,
            startTime: document.getElementById("startTime").value,
            price: document.getElementById("price").value,
            format: document.getElementById("format").value,
            status: document.getElementById("status").value
        };

        const message = document.getElementById("message");

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
                window.location.href = "../../css/admin/showtimes";

            } else {

                const error = await response.text();

                message.innerHTML = `
                <div class="alert alert-danger">
                    ${error}
                </div>
            `;
            }

        } catch (err) {
            console.error(err);
        }
    });

/* ================= INIT ================= */

window.onload = async function () {
    await loadShowtime();
};