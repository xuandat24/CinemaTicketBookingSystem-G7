// Bắt biến ngôn ngữ từ HTML (Có fallback tiếng Anh an toàn)
const txtUntitled = typeof transUntitled !== 'undefined' ? transUntitled : 'Untitled';
const txtNoDesc = typeof transNoDesc !== 'undefined' ? transNoDesc : 'No description available.';
const txtMins = typeof transMins !== 'undefined' ? transMins : 'mins';

document.addEventListener('DOMContentLoaded', async () => {

    // ==========================================
    // VÁ LỖI 1: BẢO TOÀN ID KHI ĐỔI NGÔN NGỮ
    // ==========================================
    document.querySelectorAll('a[href*="lang="]').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault(); // Chặn hành vi chuyển trang mặc định
            const langParam = new URL(this.href, window.location.origin).searchParams.get('lang');
            if (langParam) {
                const currentUrl = new URL(window.location.href);
                currentUrl.searchParams.set('lang', langParam);
                window.location.href = currentUrl.toString();
            } else {
                window.location.href = this.href;
            }
        });
    });

    // ==========================================
    // KHỞI TẠO DỮ LIỆU PHIM
    // ==========================================
    const urlParams = new URLSearchParams(window.location.search);
    const movieId = urlParams.get('id');

    if (!movieId) {
        alert("Movie not found!");
        window.location.href = '/';
        return;
    }

    await loadMovieDetails(movieId);

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

        document.getElementById('movieTitle').innerText = movie.title || txtUntitled;
        document.getElementById('movieDesc').innerText = movie.description || txtNoDesc;

        const durationEl = document.getElementById('movieDuration');
        if (durationEl) {
            durationEl.innerHTML = `<i class="fa fa-clock-o text-warning me-2"></i> ${movie.duration || 0} ${txtMins}`;
        }

        const categories = movie.categoryNames && movie.categoryNames.length > 0 ? movie.categoryNames.join(', ') : 'N/A';
        const metaEl = document.getElementById('movieMeta');
        if (metaEl) {
            metaEl.innerText = `${categories} | ${movie.duration || 0} ${txtMins}`;
        }

        document.getElementById('detailLanguage').innerText = movie.language || 'N/A';
        document.getElementById('movieDirector').innerText = movie.director || 'Unknown';
        document.getElementById('movieActors').innerText = movie.actors || 'Unknown';
        document.getElementById('movieRating').innerText = movie.rating ? movie.rating.toFixed(1) : 'N/A';
        document.getElementById('movieRelease').innerText = movie.releaseDate || 'N/A';

        const posterImg = document.getElementById('moviePoster');
        if (posterImg) {
            posterImg.src = movie.posterPath || 'https://placehold.co/400x600?text=No+Poster';
        }

        const videoContainer = document.getElementById('videoContainer');
        const bannerContainer = document.getElementById('bannerContainer');
        const bannerImg = document.getElementById('movieBanner');
        const path = movie.trailerPath;

        if (bannerImg) {
            bannerImg.src = movie.bannerPath || movie.posterPath || 'https://placehold.co/1200x500?text=No+Banner';
        }

        if (path && path.trim() !== "") {
            if(bannerContainer) bannerContainer.classList.add('d-none');
            if(videoContainer) videoContainer.classList.remove('d-none');

            if (path.includes('youtube.com') || path.includes('youtu.be')) {
                setupYoutubePlayer(path);
            } else {
                setupLocalPlayer(path);
            }
        } else {
            if(bannerContainer) bannerContainer.classList.remove('d-none');
            if(videoContainer) videoContainer.classList.add('d-none');
        }

    } catch (e) {
        console.error("Detail load error:", e);
        document.getElementById('movieTitle').innerText = txtUntitled;
        document.getElementById('movieDesc').innerText = txtNoDesc;
    }
}

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
// BOOKING LOGIC (ĐÃ DỌN DẸP SẠCH SẼ)
// ==========================================

let bookingModal;
let currentMovieId = null;

function openBookingModal() {
    const token = sessionStorage.getItem('jwtToken');
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

        // 🛠 ĐÃ SỬA LỖI MÚI GIỜ: Lấy chính xác YYYY-MM-DD theo giờ Local (VN)
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const dateString = `${year}-${month}-${day}`;

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

        // CHỐT CHẶN FRONTEND: Lọc bỏ các suất chiếu đã trôi qua so với giờ hiện tại của thiết bị
        const now = new Date();
        const validShowtimes = showtimes.filter(st => st.startTime && new Date(st.startTime) > now);

        // Nếu lọc xong mà không còn suất chiếu nào hợp lệ
        if (validShowtimes.length === 0) {
            container.innerHTML = '<p class="text-danger text-center fw-bold my-3">No showtimes available on this date.</p>';
            return;
        }

        const grouped = {};
        validShowtimes.forEach(st => {
            if (!grouped[st.theaterRoomName]) grouped[st.theaterRoomName] = [];
            grouped[st.theaterRoomName].push(st);
        });

        container.innerHTML = '';
        for (const [room, times] of Object.entries(grouped)) {
            let html = `<h6 class="fw-bold mt-3 border-bottom pb-2">${room}</h6><div class="d-flex flex-wrap gap-2 mb-3">`;
            times.forEach(st => {
                const timeStr = new Date(st.startTime).toLocaleTimeString('en-US', {
                    hour: '2-digit',
                    minute: '2-digit'
                });
                html += `<button class="btn btn-outline-success fw-bold" onclick="selectShowtime(${st.showtimeId}, '${room}', '${timeStr}', ${st.price})">${timeStr}</button>`;
            });
            html += `</div>`;
            container.innerHTML += html;
        }
    } catch (e) {
        container.innerHTML = '<p class="text-danger text-center my-3">Error loading data. Please try again.</p>';
    }
}
// LƯU Ý: Đây là điểm chốt chặn. Khi bấm giờ chiếu, lập tức đẩy sang trang movie-seats.
function selectShowtime(id, roomName, timeStr, price) {
    const username = (sessionStorage.getItem('username') || '').trim();
    const room = (roomName || '').trim();

    let url = `/movie-seats?showtimeId=${encodeURIComponent(id)}`;
    if (room) {
        url += `&roomName=${encodeURIComponent(room)}`;
    }
    if (username) {
        url += `&username=${encodeURIComponent(username)}`;
    }

    // Đóng Modal hiện tại cho lịch sự
    if (bookingModal) bookingModal.hide();

    // Chuyển hướng
    window.location.href = url;
}

// XÓA TOÀN BỘ CÁC HÀM CŨ: fetchSeats, renderSeatGrid, handleSeatSelection, goToStep, submitBooking.
// Các hàm đó đã có trang seat-select.js lo liệu!

