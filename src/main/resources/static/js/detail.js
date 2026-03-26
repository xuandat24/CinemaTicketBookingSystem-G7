document.addEventListener('DOMContentLoaded', async () => {
    const urlParams = new URLSearchParams(window.location.search);
    const movieId = urlParams.get('id');

    if (!movieId) {
        alert("Movie not found!");
        window.location.href = '/';
        return;
    }

    await loadMovieDetails(movieId);

    // Xử lý Nút Play Tùy Chỉnh cho Video file cứng
    const customPlayBtn = document.getElementById('customPlayBtn');
    const inlineVideo = document.getElementById('inlineVideo');

    if (customPlayBtn && inlineVideo) {
        customPlayBtn.addEventListener('click', () => {
            inlineVideo.play();
            customPlayBtn.classList.add('d-none');
            inlineVideo.setAttribute('controls', 'true');
        });

        inlineVideo.addEventListener('pause', () => {
            customPlayBtn.classList.remove('d-none');
            inlineVideo.removeAttribute('controls');
        });
    }
});

async function loadMovieDetails(id) {
    try {
        const response = await fetch(`/api/public/movies/${id}`);
        if (!response.ok) throw new Error("Movie not found");

        const movie = await response.json();

        // 1. Gắn dữ liệu văn bản (English)
        document.getElementById('movieTitle').innerText = movie.title || 'Untitled';
        const categories = movie.categoryNames && movie.categoryNames.length > 0
                           ? movie.categoryNames.join(', ')
                           : 'General';
        document.getElementById('movieMeta').innerText = `${categories} | ${movie.duration || 0} Mins`;
        document.getElementById('movieDesc').innerText = movie.description || 'No description available for this movie.';

        // Cập nhật thông tin nhanh
        document.getElementById('detailLanguage').innerText = movie.language || 'N/A';
        document.getElementById('movieDirector').innerText = movie.director || 'Unknown';
        document.getElementById('movieActors').innerText = movie.actors || 'Unknown';
        document.getElementById('movieRating').innerText = movie.rating ? movie.rating.toFixed(1) : 'N/A';
        document.getElementById('movieRelease').innerText = movie.releaseDate || 'N/A';

        // 2. Gắn Poster
        const posterImg = document.getElementById('moviePoster');
        if (posterImg) {
            posterImg.src = movie.posterPath || '/img/placeholder.jpg';
        }

        // 3. --- CHỐNG HIỆN BANNER KHI CÓ TRAILER ---
        const videoContainer = document.getElementById('videoContainer');
        const bannerContainer = document.getElementById('bannerContainer');
        const bannerImg = document.getElementById('movieBanner');
        const path = movie.trailerPath;

        // Luôn gán Banner làm hình chờ hoặc fallback
        if (bannerImg) {
            bannerImg.src = movie.bannerPath || movie.posterPath || '/img/placeholder.jpg';
        }

        // Kiểm tra xem phim có trailer hay không
        if (path && path.trim() !== "") {
            console.log("Trailer found, switching to video mode.");

            // LOGIC CỐT LÕI: Ẩn Banner, Hiện Video
            if(bannerContainer) bannerContainer.classList.add('d-none');
            if(videoContainer) videoContainer.classList.remove('d-none');

            // Thiết lập loại Player tương ứng
            if (path.includes('youtube.com') || path.includes('youtu.be')) {
                setupYoutubePlayer(path);
            } else {
                setupLocalPlayer(path);
            }
        } else {
            console.log("No trailer, keeping banner mode.");
            // Giữ mặc định: Hiện Banner, Ẩn Video
            if(bannerContainer) bannerContainer.classList.remove('d-none');
            if(videoContainer) videoContainer.classList.add('d-none');
        }

    } catch (e) {
        console.error("Detail load error:", e);
        document.getElementById('movieTitle').innerText = "Movie Not Found";
        document.getElementById('movieDesc').innerText = "Sorry, we couldn't load the details for this movie. Please return to the home page.";
    }
}

// Thiết lập Youtube Iframe
function setupYoutubePlayer(path) {
    const inlineVideo = document.getElementById('inlineVideo');
    const youtubePlayer = document.getElementById('youtubePlayer');
    const customPlayBtn = document.getElementById('customPlayBtn');

    if (!youtubePlayer) return;

    youtubePlayer.classList.remove('d-none');
    if (inlineVideo) inlineVideo.classList.add('d-none');
    if (customPlayBtn) customPlayBtn.classList.add('d-none');

    let videoId = "";
    if (path.includes('v=')) {
        videoId = path.split('v=')[1].split('&')[0];
    } else if (path.includes('embed/')) {
        videoId = path.split('embed/')[1].split('?')[0];
    } else {
        videoId = path.split('/').pop();
    }

    youtubePlayer.innerHTML = `
        <iframe width="100%" height="100%"
                src="https://www.youtube.com/embed/${videoId}"
                frameborder="0"
                allow="encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen>
        </iframe>`;
}

// Thiết lập Player cho file MP4 nội bộ
function setupLocalPlayer(path) {
    const inlineVideo = document.getElementById('inlineVideo');
    const youtubePlayer = document.getElementById('youtubePlayer');
    const customPlayBtn = document.getElementById('customPlayBtn');

    if (!inlineVideo) return;

    inlineVideo.classList.remove('d-none');
    if (youtubePlayer) {
        youtubePlayer.classList.add('d-none');
        youtubePlayer.innerHTML = '';
    }
    if (customPlayBtn) customPlayBtn.classList.remove('d-none');

    inlineVideo.src = path;
    inlineVideo.load();
}

function goBack() {
    window.history.back();
}

// ==========================================
// BOOKING LOGIC (TIẾP TỤC DÙNG LOGIC CŨ CỦA BẠN)
// ==========================================

let bookingModal;
let currentMovieId = null;
let currentBooking = {
    showtimeId: null,
    theaterName: '',
    startTime: '',
    pricePerSeat: 0,
    selectedSeats: [],
    selectedSeatNames: [],
    totalPrice: 0
};

function openBookingModal() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        alert("Please login before booking a ticket!");
        window.location.href = '/login';
        return;
    }

    const urlParams = new URLSearchParams(window.location.search);
    currentMovieId = urlParams.get('id');

    if (!currentMovieId) return alert("Movie not found!");

    document.getElementById('bookingMovieTitle').innerText = document.getElementById('movieTitle').innerText;

    renderDateSelector();
    resetBooking();

    if (!bookingModal) {
        bookingModal = new bootstrap.Modal(document.getElementById('bookingModal'));
    }
    bookingModal.show();
}

function renderDateSelector() {
    const container = document.getElementById('dateSelector');
    container.innerHTML = '';
    const today = new Date();
    for (let i = 0; i < 7; i++) {
        let d = new Date(today);
        d.setDate(today.getDate() + i);
        const dateString = d.toISOString().split('T')[0];
        const displayDate = d.toLocaleDateString('en-US', { day: '2-digit', month: 'short' });
        const displayDay = i === 0 ? "Today" : d.toLocaleDateString('en-US', { weekday: 'short' });
        const btn = document.createElement('button');
        btn.className = `btn btn-outline-primary flex-shrink-0 text-center py-2 px-3 ${i === 0 ? 'active' : ''}`;
        btn.innerHTML = `<small class="d-block fw-bold">${displayDay}</small><span>${displayDate}</span>`;
        btn.onclick = () => {
            document.querySelectorAll('#dateSelector button').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            fetchShowtimes(dateString);
        };
        container.appendChild(btn);
        if (i === 0) fetchShowtimes(dateString);
    }
}

async function fetchShowtimes(dateStr) {
    const container = document.getElementById('showtimeContainer');
    container.innerHTML = '<div class="text-center"><div class="spinner-border text-primary"></div></div>';
    try {
        const res = await fetch(`/api/showtimes/available?movieId=${currentMovieId}&date=${dateStr}`);
        if (!res.ok) throw new Error("API Error");
        const showtimes = await res.json();
        if (showtimes.length === 0) {
            container.innerHTML = '<p class="text-danger text-center fw-bold my-3">No showtimes available on this date.</p>';
            return;
        }
        const grouped = {};
        showtimes.forEach(st => {
            if (!grouped[st.theaterRoomName]) grouped[st.theaterRoomName] = [];
            grouped[st.theaterRoomName].push(st);
        });
        container.innerHTML = '';
        for (const [room, times] of Object.entries(grouped)) {
            let html = `<h6 class="fw-bold mt-3 border-bottom pb-2">${room}</h6><div class="d-flex flex-wrap gap-2 mb-3">`;
            times.forEach(st => {
                const timeStr = new Date(st.startTime).toLocaleTimeString('en-US', {hour: '2-digit', minute:'2-digit'});
                html += `<button class="btn btn-outline-success fw-bold" onclick="selectShowtime(${st.showtimeId}, '${room}', '${timeStr}', ${st.price})">${timeStr}</button>`;
            });
            html += `</div>`;
            container.innerHTML += html;
        }
    } catch (e) { container.innerHTML = '<p class="text-danger text-center my-3">Error loading data. Please try again.</p>'; }
}

function selectShowtime(id, roomName, timeStr, price) {
    currentBooking.showtimeId = id;
    currentBooking.theaterName = roomName;
    currentBooking.startTime = timeStr;
    currentBooking.pricePerSeat = price;
    currentBooking.selectedSeats = [];
    currentBooking.selectedSeatNames = [];
    currentBooking.totalPrice = 0;
    document.getElementById('btnNextToCombo').disabled = true;
    document.getElementById('step2SeatCount').innerText = "0";
    fetchSeats(id);
}

async function fetchSeats(showtimeId) {
    goToStep(2);
    const container = document.getElementById('seatGrid');
    container.innerHTML = '<div class="spinner-border text-primary my-4"></div>';
    try {
        const res = await fetch(`/api/showtimes/${showtimeId}/seats`);
        if (!res.ok) throw new Error("API Error");
        const data = await res.json();
        if (data.basePrice) currentBooking.pricePerSeat = data.basePrice;
        const realSeats = data.seats.map(s => {
            let type = 'Normal';
            const sType = s.seatType ? s.seatType.toUpperCase() : '';
            if (sType.includes('VIP')) type = 'VIP';
            else if (sType.includes('ĐÔI') || sType.includes('PAIR')) type = 'Pair';
            const factor = s.priceFactor != null ? s.priceFactor : 1.0;
            return { seatId: s.seatId, seatName: s.seatCode, type: type, price: currentBooking.pricePerSeat * factor, isBooked: s.booked };
        });
        renderSeatGrid(realSeats);
    } catch (e) { container.innerHTML = '<p class="text-danger my-4">Error loading seats. Please try again.</p>'; }
}

function renderSeatGrid(seats) {
    const container = document.getElementById('seatGrid');
    container.innerHTML = '';
    const grouped = {};
    seats.forEach(s => {
        const row = s.seatName.charAt(0);
        if(!grouped[row]) grouped[row] = [];
        grouped[row].push(s);
    });
    for(const [row, rowSeats] of Object.entries(grouped)) {
        const rowDiv = document.createElement('div');
        rowDiv.className = 'd-flex justify-content-center gap-2 mb-2 align-items-center';
        rowDiv.insertAdjacentHTML('beforeend', `<div class="fw-bold text-muted" style="width: 25px;">${row}</div>`);
        rowSeats.forEach(s => {
            const btn = document.createElement('button');
             btn.innerText = s.seatName;
             btn.disabled = s.isBooked;
             let baseClass = 'btn btn-sm seat-btn fw-bold transition-all ';
             if (s.isBooked) baseClass += 'btn-secondary opacity-50 border-0';
             else {
                 if (s.type === 'VIP') baseClass += 'btn-outline-warning text-dark';
                 else if (s.type === 'Pair') baseClass += 'btn-outline-info text-dark';
                 else baseClass += 'btn-outline-secondary';
             }
             btn.className = baseClass;
             btn.style.width = s.type === 'Pair' ? '70px' : '40px';
             if(!s.isBooked) btn.onclick = () => handleSeatSelection(btn, s);
             rowDiv.appendChild(btn);
        });
        rowDiv.insertAdjacentHTML('beforeend', `<div class="fw-bold text-muted" style="width: 25px;">${row}</div>`);
        container.appendChild(rowDiv);
    }
}

function handleSeatSelection(btn, seatData) {
    const isSelected = currentBooking.selectedSeats.includes(seatData.seatId);
    if (isSelected) {
        currentBooking.selectedSeats = currentBooking.selectedSeats.filter(id => id !== seatData.seatId);
        currentBooking.selectedSeatNames = currentBooking.selectedSeatNames.filter(name => name !== seatData.seatName);
        currentBooking.totalPrice -= seatData.price;
        btn.classList.remove('btn-success', 'text-white');
        if(seatData.type === 'VIP') btn.classList.add('btn-outline-warning', 'text-dark');
        else if(seatData.type === 'Pair') btn.classList.add('btn-outline-info', 'text-dark');
        else btn.classList.add('btn-outline-secondary');
    } else {
        currentBooking.selectedSeats.push(seatData.seatId);
        currentBooking.selectedSeatNames.push(seatData.seatName);
        currentBooking.totalPrice += seatData.price;
        btn.className = 'btn btn-sm seat-btn btn-success text-white fw-bold transition-all';
        btn.style.width = seatData.type === 'Pair' ? '70px' : '40px';
    }
    const count = currentBooking.selectedSeats.length;
    document.getElementById('step2SeatCount').innerText = count;
    document.getElementById('btnNextToCombo').disabled = count === 0;
}

function goToStep(stepNumber) {
    document.querySelectorAll('.booking-step').forEach(el => el.classList.add('d-none'));
    document.getElementById(`step-${stepNumber}`).classList.remove('d-none');
    if (stepNumber === 4) {
        document.getElementById('summaryMovieName').innerText = document.getElementById('movieTitle').innerText;
        document.getElementById('summaryTheater').innerText = currentBooking.theaterName;
        document.getElementById('summaryTime').innerText = currentBooking.startTime;
        document.getElementById('summarySeats').innerText = currentBooking.selectedSeatNames.join(', ') || 'None selected';
        document.getElementById('summaryTotal').innerText = currentBooking.totalPrice.toLocaleString();
    }
}

async function submitBooking() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        alert("Please login before booking a ticket!");
        window.location.href = '/login';
        return;
    }
    const payload = { showtimeId: currentBooking.showtimeId, seatIds: currentBooking.selectedSeats, finalPrice: currentBooking.totalPrice, combos: [] };
    try {
        const btn = document.getElementById('btnConfirmBooking');
        btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Processing...';
        btn.disabled = true;
        const res = await fetch('/api/booking/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify(payload)
        });
        const result = await res.json();
        if (result.success) {
            alert("🎉 Booking successful!");
            bookingModal.hide();
            window.location.href = '/booking/history';
        } else {
            alert("Booking error: " + (result.message || "Please try again."));
            btn.innerHTML = 'Confirm & Pay';
            btn.disabled = false;
        }
    } catch (e) {
        alert("Server connection error.");
        document.getElementById('btnConfirmBooking').disabled = false;
    }
}

function resetBooking() {
    currentBooking = { showtimeId: null, theaterName: '', startTime: '', pricePerSeat: 0, selectedSeats: [], selectedSeatNames: [], totalPrice: 0 };
    document.getElementById('btnNextToCombo').disabled = true;
    document.querySelectorAll('.seat-btn').forEach(b => {
        b.classList.remove('btn-success');
        b.classList.add('btn-outline-secondary');
    });
    goToStep(1);
}