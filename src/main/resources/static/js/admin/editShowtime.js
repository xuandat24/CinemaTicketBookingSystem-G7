const API = "/api/showtimes";
const urlParams = new URLSearchParams(window.location.search);
const id = urlParams.get("id");

document.addEventListener("DOMContentLoaded", async function() {
    if (!id) {
        if(typeof showToast === 'function') showToast("Showtime ID not found!", "error");
        setTimeout(() => { window.location.href = "/admin/showtimes"; }, 1500);
        return;
    }

    const priceInput = document.getElementById("price");
    priceInput.addEventListener("focus", function (e) { e.target.value = e.target.value.replace(/\D/g, ""); });
    priceInput.addEventListener("input", function (e) { e.target.value = e.target.value.replace(/\D/g, ""); });
    priceInput.addEventListener("blur", function (e) {
        let value = e.target.value.replace(/\D/g, "");
        if (value) e.target.value = Number(value).toLocaleString("vi-VN");
    });

    await loadShowtime();
});

let currentMovieDuration = 120; // Default initialization as fallback

async function loadShowtime() {
    try {
        const token = sessionStorage.getItem("jwtToken");
        const response = await fetch(`${API}/${id}`, {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!response.ok) throw new Error("Cannot load data");

        const data = await response.json();

        document.getElementById("movieTitle").value = data.movieTitle || '';
        document.getElementById("roomName").value = data.theaterRoomName || '';
        document.getElementById("movieId").value = data.movieId || '';
        document.getElementById("roomId").value = data.theaterRoomId || '';

        if (data.startTime) document.getElementById("startTime").value = data.startTime.substring(0, 16);
        if (data.price) document.getElementById("price").value = Number(data.price).toLocaleString("vi-VN");

        document.getElementById("format").value = data.format || '';
        document.getElementById("status").value = data.status || '';

        // Load additional movie info to get duration for cleaning time calculation
        if (data.movieId) {
            const mRes = await fetch(`/api/admin/movies/${data.movieId}`, { headers: { 'Authorization': 'Bearer ' + token }});
            if (mRes.ok) {
                const mData = await mRes.json();
                if (mData.duration) currentMovieDuration = mData.duration;
            }
        }

    } catch (error) {
        if(typeof showToast === 'function') showToast("Cannot load showtime information!", "error");
    }
}

document.getElementById("editShowtimeForm").addEventListener("submit", async function (e) {
    e.preventDefault();

    let errorEle = document.getElementById("formValidationError");
    if (!errorEle) errorEle = document.getElementById("message");

    if (errorEle) {
        errorEle.style.display = "none";
        errorEle.innerHTML = "";
    }

    const btnSubmit = e.target.querySelector('button[type="submit"]');
    const originalText = btnSubmit.innerHTML;
    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Checking schedule...';
    btnSubmit.disabled = true;

    const showError = (msg) => {
        if(typeof showToast === 'function') {
            showToast(msg, "error");
        } else {
            alert(msg);
        }
        btnSubmit.innerHTML = originalText;
        btnSubmit.disabled = false;
    }

    const rawPrice = document.getElementById("price").value;
    const priceNumber = Number(rawPrice.replace(/\D/g, ""));
    if (!priceNumber || priceNumber < 10000) return showError("Minimum ticket price must be at least 10,000 VND!");

    // VALIDATE PAST TIME
    const startTimeStr = document.getElementById("startTime").value;
    const startTimeDate = new Date(startTimeStr);
    if (startTimeDate <= new Date()) return showError("Showtime must start in the future!");

    // CHECK SCHEDULE CONFLICT WHEN UPDATING
    const roomId = document.getElementById("roomId").value;
    const CLEANING_TIME = 15;
    const newStartMs = startTimeDate.getTime();
    const newEndMs = newStartMs + (currentMovieDuration + CLEANING_TIME) * 60000;

    try {
        const token = sessionStorage.getItem("jwtToken");
        const stRes = await fetch(`/api/showtimes/search?roomId=${roomId}`, { headers: { 'Authorization': 'Bearer ' + token }});
        if (stRes.ok) {
            const existingShowtimes = await stRes.json();
            for (let st of existingShowtimes) {
                // Skip the current showtime being edited
                if (st.status !== 'ACTIVE' || String(st.showtimeId) === String(id)) continue;

                const exStart = new Date(st.startTime).getTime();
                const exEnd = new Date(st.endTime).getTime() + (CLEANING_TIME * 60000);

                if (newStartMs < exEnd && newEndMs > exStart) {
                    const exStartStr = new Date(st.startTime).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'});
                    const exEndStr = new Date(st.endTime).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'});
                    return showError(`Schedule conflict! This room already has the movie "${st.movieTitle}" from ${exStartStr} to ${exEndStr}. Requires +15 minutes for cleaning!`);
                }
            }
        }
    } catch(e){}

    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Updating...';

    const data = {
        movieId: document.getElementById("movieId").value,
        theaterRoomId: roomId,
        startTime: startTimeStr,
        price: priceNumber,
        format: document.getElementById("format").value,
        status: document.getElementById("status").value
    };

    try {
        const token = sessionStorage.getItem("jwtToken");
        const response = await fetch(`${API}/${id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            sessionStorage.setItem("successMessage", "Showtime updated successfully");
            if(typeof showToast === 'function') showToast("Showtime updated successfully!", "success");
            setTimeout(() => { window.location.href = "/admin/showtimes"; }, 1000);
        } else {
            const error = await response.json();
            showError(error.message || "Update failed");
        }
    } catch (err) {
        showError("Server connection error!");
    }
});
