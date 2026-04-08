async function loadMovies() {

    const movieSelect = document.getElementById("movieId");

    try {

        const response = await fetch("/api/admin/movies", {
            credentials: "include"
        });

        const movies = await response.json();

        movies.forEach(movie => {

            const option = document.createElement("option");

            option.value = movie.movieId;

            option.textContent = movie.title;

            movieSelect.appendChild(option);

        });

    } catch (error) {

        console.error("Cannot load movies:", error);

    }
}

async function loadRooms(){
    const roomSelect = document.getElementById("roomId");

    try {
        const response = await fetch("/api/theater-rooms", {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Cannot load theater rooms");
        }

        const rooms = await response.json();

        rooms.forEach(room => {
            const option = document.createElement("option");
            option.value = room.roomId;
            option.text = room.roomName;
            roomSelect.appendChild(option);
        });
    } catch (error) {
        console.error("Cannot load theater rooms:", error);
    }

}

document.addEventListener("DOMContentLoaded", function(){

    document.getElementById("price")
        .addEventListener("input", formatPriceInput);

    loadMovies();
    loadRooms();
});

function formatPriceInput(e) {
    let value = e.target.value;

    // chỉ giữ số
    value = value.replace(/\D/g, "");

    // format dấu chấm
    value = new Intl.NumberFormat("vi-VN").format(value);

    e.target.value = value;
}

document
    .getElementById("createShowtimeForm")
    .addEventListener("submit", async function(e){

    e.preventDefault();
    const message = document.getElementById("message");

    const rawPrice = document.getElementById("price").value;

    const priceNumber = Number(rawPrice.replace(/\./g, ""));

    const data = {

        movieId: document.getElementById("movieId").value,

        theaterRoomId: document.getElementById("roomId").value,

        startTime: document.getElementById("startTime").value,

        format: document.getElementById("format").value,

        price: priceNumber,

        status: document.getElementById("status").value

    };

    const response = await fetch("/api/showtimes", {

        method: "POST",

        headers: {
            "Content-Type": "application/json"
        },

        body: JSON.stringify(data)

    });

    if(response.ok){
        // save message to sessionStorage
        sessionStorage.setItem("successMessage", "Showtime created successfully");

        // redirect
        window.location.href = "/admin/showtimes";

    }else{
        const error = await response.json();

        let html = "";

        // If there are multiple validation errors
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
                    ${error.message || "Cannot create showtime"}
                </div>
            `;
        }

        message.innerHTML = html;

    }

});

document.getElementById("price").addEventListener("input", function (e) {
    let value = e.target.value.replace(/\D/g, ""); // bỏ hết ký tự không phải số

    if (value) {
        value = Number(value).toLocaleString("vi-VN"); // format 100.000
    }

    e.target.value = value;
});