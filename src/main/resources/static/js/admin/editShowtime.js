const API = "/api/showtimes";

const urlParams = new URLSearchParams(window.location.search);
const id = urlParams.get("id");

/* ================= LOAD DETAIL ================= */

async function loadShowtime() {
    try {
        const token = localStorage.getItem('jwtToken'); // Lấy token
        const response = await fetch(`${API}/${id}`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}` // THÊM TOKEN VÀO ĐÂY
            }
        });
        const data = await response.json();

        document.getElementById("movieTitle").value = data.movieTitle;
        document.getElementById("roomName").value = data.theaterRoomName;

        document.getElementById("movieId").value = data.movieId;
        document.getElementById("roomId").value = data.theaterRoomId;
        document.getElementById("startTime").value = data.startTime.substring(0, 16);
        document.getElementById("price").value = data.price;
        document.getElementById("format").value = data.format;

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
            format: document.getElementById("format").value
        };

        const message = document.getElementById("message");

        try {
            const token = localStorage.getItem('jwtToken'); // Lấy token
            const response = await fetch(`${API}/${id}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}` // THÊM TOKEN VÀO ĐÂY
                },
                body: JSON.stringify(data)
            });

            if (response.ok) {

                sessionStorage.setItem("successMessage", "Showtime updated successfully");
                window.location.href = "/admin/showtimes";

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

/* ================= DELETE ================= */

async function deleteShowtime() {

    if (!confirm("Delete this showtime?")) return;

    const token = localStorage.getItem('jwtToken'); // Lấy token
    await fetch(`${API}/${id}`, {
        method: "DELETE",
        headers: {
            "Authorization": `Bearer ${token}` // THÊM TOKEN VÀO ĐÂY
        }
    });

    window.location.href = "/admin/showtimes";
}

/* ================= INIT ================= */

window.onload = async function () {
    await loadShowtime();
};