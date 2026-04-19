document.addEventListener("DOMContentLoaded", function() {
    const priceInput = document.getElementById("price");

    // 1. ON FOCUS: Remove dots to avoid Unikey typing issues
    priceInput.addEventListener("focus", function (e) {
        e.target.value = e.target.value.replace(/\D/g, "");
    });

    // 2. ON INPUT: Allow only numbers
    priceInput.addEventListener("input", function (e) {
        e.target.value = e.target.value.replace(/\D/g, "");
    });

    // 3. ON BLUR: Auto format with thousand separators
    priceInput.addEventListener("blur", function (e) {
        let value = e.target.value.replace(/\D/g, "");
        if (value) {
            e.target.value = Number(value).toLocaleString("vi-VN");
        }
    });

    loadMovies();
    loadRooms();
});

async function loadMovies() {
    const movieSelect = document.getElementById("movieId");
    try {
        const token = sessionStorage.getItem("jwtToken");
        const response = await fetch("/api/admin/movies", {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        const movies = await response.json();
        movies.forEach(movie => {
            const option = document.createElement("option");
            option.value = movie.movieId;
            // Store movie duration in attribute for schedule conflict checking
            option.setAttribute("data-duration", movie.duration || 120);
            option.textContent = movie.title;
            movieSelect.appendChild(option);
        });
    } catch (error) {
        console.error("Cannot load movies:", error);
    }
}

async function loadRooms() {
    const roomSelect = document.getElementById("roomId");
    try {
        const token = sessionStorage.getItem("jwtToken");
        const response = await fetch("/api/theater-rooms", {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!response.ok) throw new Error("Cannot load theater rooms");
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

document.getElementById("createShowtimeForm").addEventListener("submit", async function (e) {
    e.preventDefault();

    let errorEle = document.getElementById("formValidationError");
    if (!errorEle) errorEle = document.getElementById("message");
    if (errorEle) {
        errorEle.style.display = "none";
        errorEle.innerHTML = "";
    }

    const btnSubmit = e.target.querySelector('button[type="submit"]');
    const originalBtnText = btnSubmit.innerHTML;
    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Checking schedule...';
    btnSubmit.disabled = true;

    const showError = (msg) => {
        if(typeof showToast === 'function') {
            showToast(msg, "error");
        } else {
            alert(msg);
        }
        btnSubmit.innerHTML = originalBtnText;
        btnSubmit.disabled = false;
    }

    // 1. VALIDATE TICKET PRICE
    let priceText = document.getElementById("price").value;
    let priceNumber = Number(priceText.replace(/\D/g, ""));
    if (!priceNumber || priceNumber < 10000) {
        return showError("Minimum ticket price must be at least 10,000 VND!");
    }

    // 2. VALIDATE PAST TIME
    const startTimeStr = document.getElementById("startTime").value;
    const startTimeDate = new Date(startTimeStr);
    if (startTimeDate <= new Date()) {
        return showError("Showtime must start in the future!");
    }

    // 3. SCHEDULE CONFLICT DETECTION (including 15 minutes cleaning time)
    const movieId = document.getElementById("movieId").value;
    const roomId = document.getElementById("roomId").value;
    const movieSelect = document.getElementById("movieId");
    const selectedOption = movieSelect.options[movieSelect.selectedIndex];

    // Get movie duration (default 120 mins) + 15 mins cleaning time
    const duration = parseInt(selectedOption.getAttribute("data-duration"), 10) || 120;
    const CLEANING_TIME = 15;

    const newStartMs = startTimeDate.getTime();
    const newEndMs = newStartMs + (duration + CLEANING_TIME) * 60000;

    try {
        const token = sessionStorage.getItem("jwtToken");
        // Fetch showtimes for this room
        const stRes = await fetch(`/api/showtimes/search?roomId=${roomId}`, { headers: { 'Authorization': 'Bearer ' + token }});
        if (stRes.ok) {
            const existingShowtimes = await stRes.json();
            for (let st of existingShowtimes) {
                if (st.status !== 'ACTIVE') continue;

                const exStart = new Date(st.startTime).getTime();
                // Existing showtime end + 15 mins cleaning
                const exEnd = new Date(st.endTime).getTime() + (CLEANING_TIME * 60000);

                // Overlapping detection algorithm
                if (newStartMs < exEnd && newEndMs > exStart) {
                    const exStartStr = new Date(st.startTime).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'});
                    const exEndStr = new Date(st.endTime).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'});
                    return showError(`Schedule conflict! This room already has the movie "${st.movieTitle}" from ${exStartStr} to ${exEndStr}. Please leave at least 15 minutes for cleaning!`);
                }
            }
        }
    } catch(e) {
        console.warn("Skipping schedule conflict check due to connection error.");
    }

    // 4. SEND DATA TO SERVER
    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Creating...';

    const data = {
        movieId: movieId,
        theaterRoomId: roomId,
        startTime: startTimeStr,
        format: document.getElementById("format").value,
        price: priceNumber,
        status: document.getElementById("status").value
    };

    try {
        const token = sessionStorage.getItem("jwtToken");
        const response = await fetch("/api/showtimes", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            sessionStorage.setItem("successMessage", "Showtime created successfully");
            if(typeof showToast === 'function') showToast("Showtime created successfully!", "success");
            setTimeout(() => { window.location.href = "/admin/showtimes"; }, 1000);
        } else {
            const error = await response.json();
            showError(error.message || "Cannot create showtime");
        }
    } catch (err) {
        showError("Server connection error!");
    }
});
