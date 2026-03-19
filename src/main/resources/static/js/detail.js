document.addEventListener('DOMContentLoaded', async () => {
    const urlParams = new URLSearchParams(window.location.search);
    const movieId = urlParams.get('id');

    if (!movieId) {
        alert("Movie not found!");
        window.location.href = '/movies.html';
        return;
    }

    await loadMovieDetails(movieId);
});

async function loadMovieDetails(id) {
    try {
        // Gọi API Public (Hãy đảm bảo bạn có API này)
        const response = await fetch(`/api/public/movies/${id}`);
        if (!response.ok) throw new Error("Movie not found");

        const movie = await response.json();

        // 1. Gắn dữ liệu cơ bản
        document.getElementById('movieTitle').innerText = movie.title;
        const catNames = movie.categoryNames ? movie.categoryNames.join(', ') : 'Uncategorized';
        document.getElementById('movieMeta').innerText = `${catNames} / ${movie.duration} Mins`;

        document.getElementById('movieDirector').innerText = movie.director || 'Unknown';
        document.getElementById('movieActors').innerText = movie.actors || 'Unknown';
        document.getElementById('movieRating').innerText = movie.rating ? movie.rating.toFixed(1) : 'N/A';
        document.getElementById('movieRelease').innerText = movie.releaseDate || 'N/A';
        document.getElementById('movieDesc').innerText = movie.description || 'No description available.';

        const banner = movie.bannerPath || '/img/placeholder.jpg';
        document.getElementById('moviePoster').src = banner;

        // 3. XỬ LÝ TRAILER / BANNER (Cột phải)
        const videoContainer = document.getElementById('videoContainer');
        const bannerContainer = document.getElementById('bannerContainer');
        const inlineVideo = document.getElementById('inlineVideo');
        const customPlayBtn = document.getElementById('customPlayBtn');

        if (movie.trailerPath && movie.trailerPath.trim() !== "") {
            // NẾU CÓ VIDEO: Gắn link, hiện Video, ẩn ảnh Banner dự phòng
            inlineVideo.src = movie.trailerPath;
            videoContainer.classList.remove('d-none');
            if (bannerContainer) bannerContainer.classList.add('d-none');

            // Xử lý sự kiện nút Play tròn
            customPlayBtn.addEventListener('click', function() {
                inlineVideo.play();
                inlineVideo.setAttribute('controls', 'controls');
                customPlayBtn.style.display = 'none';
            });

            inlineVideo.addEventListener('pause', function() {
                customPlayBtn.style.display = 'flex';
                inlineVideo.removeAttribute('controls');
            });

            inlineVideo.addEventListener('ended', function() {
                customPlayBtn.style.display = 'flex';
                inlineVideo.removeAttribute('controls');
            });

        } else {
            // NẾU KHÔNG CÓ VIDEO: Ẩn hoàn toàn mọi thứ ở cột phải
            videoContainer.classList.add('d-none');
            if (bannerContainer) bannerContainer.classList.add('d-none');

            // Ẩn luôn thẻ cha (thẻ div chứa nền đen 530px) để không để lại mảng đen trống trên giao diện
            videoContainer.parentElement.classList.add('d-none');
        }

    } catch (error) {
        console.error("Error:", error);
        alert("Failed to load movie details.");
    }
}

// Xử lý nút Back thông minh
function goBack() {
    // Nếu có state tìm kiếm trong bộ nhớ, dùng history.back để mượt mà nhất
    if (sessionStorage.getItem('movieSearchState')) {
        window.history.back();
    } else {
        // Nếu người dùng vào thẳng link Detail không qua trang Search
        window.location.href = '/movies.html';
    }
}