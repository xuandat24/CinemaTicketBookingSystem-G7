async function loadMovies() {

    const movieSelect = document.getElementById("movieId");

    try {
        const token = localStorage.getItem('jwtToken'); // Lấy token
        const response = await fetch("/api/admin/movies", {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}` // THÊM TOKEN VÀO ĐÂY
            }
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
        const token = localStorage.getItem('jwtToken');
        const response = await fetch("/api/admin/rooms", {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const rooms = await response.json();

        rooms.forEach(room => {

            const option = document.createElement("option");

            option.value = room.roomId;

            option.text = room.roomName;

            roomSelect.appendChild(option);

        });
    } catch (error) {
        console.error("Cannot load rooms:", error);
    }
}

document
    .getElementById("createShowtimeForm")
    .addEventListener("submit", async function(e){

        e.preventDefault();

        const data = {

            movieId: document.getElementById("movieId").value,

            theaterRoomId: document.getElementById("roomId").value,

            startTime: document.getElementById("startTime").value,

            format: document.getElementById("format").value,

            price: document.getElementById("price").value

        };

        const message = document.getElementById("message"); // Declare message here

        const token = localStorage.getItem('jwtToken'); // Lấy token
        const response = await fetch("/api/showtimes", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}` // Bổ sung dòng này
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

            message.innerHTML =`
            <div class="alert alert-danger">
                ${error.message}
            </div>`;

        }

    });

window.onload = function(){

    loadMovies();

    loadRooms();

};