async function loadMovies() {

    const movieSelect = document.getElementById("movieId");

    try {

        const response = await fetch("/api/admin/movies");

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

   const response = await fetch("/api/rooms");

   const rooms = await response.json();

   const roomSelect = document.getElementById("roomId");

   rooms.forEach(room => {

       const option = document.createElement("option");

       option.value = room.id;

       option.text = room.name;

       roomSelect.appendChild(option);

   });

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

            price: document.getElementById("price").value,

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