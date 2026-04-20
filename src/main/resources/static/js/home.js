// Function to handle left/right scrolling for sliders
function scrollSlider(btn, direction) {
    const wrapper = btn.closest('.movie-slider-wrapper');
    const slider = wrapper.querySelector('.movie-slider');
    const scrollAmount = slider.clientWidth * 0.8;

    if (direction === 'left') {
        slider.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
    } else {
        slider.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }
}

// BẮT BIẾN NGÔN NGỮ TỪ HTML (Có fallback an toàn tiếng Anh để chống lỗi)
const txtDetail = typeof transDetail !== 'undefined' ? transDetail : 'Detail';
const txtBook = typeof transBookTicket !== 'undefined' ? transBookTicket : 'Book Ticket';
const txtMore = typeof transMoreInfo !== 'undefined' ? transMoreInfo : 'More Info';
const txtSoon = typeof transComingSoon !== 'undefined' ? transComingSoon : 'Coming Soon';
const txtNoShow = typeof transNoShowing !== 'undefined' ? transNoShowing : 'No movies are currently showing.';
const txtNoSoon = typeof transNoUpcoming !== 'undefined' ? transNoUpcoming : 'No upcoming movies at the moment.';

// GỘP CHUNG THÀNH 1 SỰ KIỆN DUY NHẤT KHI TẢI TRANG
document.addEventListener('DOMContentLoaded', async () => {
    try {
        console.log("Fetching movies data from server...");

        // 1. Fetch ALL public movies ONCE
        const response = await fetch('/api/public/movies');
        if (!response.ok) throw new Error("Failed to fetch movies from API");
        const allMovies = await response.json();

        // 2. Filter movies on the client-side
        const nowPlayingMovies = allMovies.filter(m => m.status === 'Now Playing');
        const comingSoonMovies = allMovies.filter(m => m.status === 'Coming Soon');

        console.log(`Found: ${nowPlayingMovies.length} Now Playing, ${comingSoonMovies.length} Coming Soon.`);

        // 3. SMART BANNER LOGIC (Fallback mechanism)
        let bannerMovies = nowPlayingMovies.slice(0, 5);
        if (bannerMovies.length === 0) {
            bannerMovies = comingSoonMovies.slice(0, 5);
        }
        if (bannerMovies.length === 0) {
            bannerMovies = allMovies.filter(m => m.status !== 'Disabled').slice(0, 5);
        }

        // 4. Render to UI
        renderBanners(bannerMovies);
        renderNowShowing(nowPlayingMovies);
        renderComingSoon(comingSoonMovies);

    } catch (error) {
        console.error("Error loading Home Page data:", error);
    }
});

// --- RENDER FUNCTIONS ---

function renderBanners(movies) {
    const indicators = document.getElementById('bannerIndicators');
    const inner = document.getElementById('bannerInner');
    if (!indicators || !inner) return;

    if (movies.length === 0) {
        // EMPTY STATE FOR BANNER
        inner.innerHTML = `
            <div class="carousel-item active">
                <img src="https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=1920" class="d-block w-100" style="height: 85vh; object-fit: cover;">
                <div class="carousel-caption"><h1>Welcome to CTBS Cinema</h1></div>
            </div>`;
        return;
    }

    let indicatorHtml = '';
    let innerHtml = '';

    movies.forEach((movie, index) => {
        const isActive = index === 0 ? 'active' : '';
        const bgImage = movie.bannerPath || movie.posterPath || 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=1920';

        indicatorHtml += `<button type="button" data-bs-target="#carouselExampleCaptions" data-bs-slide-to="${index}" class="${isActive}" aria-current="true"></button>`;

        innerHtml += `
            <div class="carousel-item ${isActive}">
                <a href="/detail?id=${movie.movieId}" class="banner-link-wrapper">
                    <img src="${bgImage}" class="d-block w-100" alt="Banner" loading="lazy" style="height: 85vh; object-fit: cover; background-color: #0b0b0b; filter: brightness(0.8);">
                    <div class="carousel-caption">
                        <h6 class="text-uppercase text-white-50 fw-bold tracking-wider mb-2">
                            <i class="fa fa-star text-warning me-1"></i> Blockbuster Release
                        </h6>
                        <h1 class="mb-3">${movie.title}</h1>
                        <p class="text-truncate d-none d-md-block mb-4" style="max-width: 600px; margin: 0 auto;">${movie.description}</p>
                        <div class="hero-ticket-btn"><i class="fa fa-info-circle fs-5 me-2"></i> ${txtDetail}</div>
                    </div>
                </a>
            </div>
        `;
    });

    indicators.innerHTML = indicatorHtml;
    inner.innerHTML = innerHtml;
}

function renderNowShowing(movies) {
    const container = document.getElementById('nowShowingSlider');
    if (!container) return;

    if (movies.length === 0) {
        container.closest('.movie-slider-wrapper').innerHTML = `
            <div class="text-center py-5 w-100">
                <h5 class="text-muted">${txtNoShow}</h5>
            </div>`;
        return;
    }

    let html = '';
    movies.forEach(movie => {
        const poster = movie.posterPath || 'https://placehold.co/300x450?text=No+Poster';
        html += `
            <div class="movie-slider-item">
                <div class="movie-card mb-0">
                    <div class="movie-poster-wrapper">
                        <img src="${poster}" style="height: 350px; width: 100%; object-fit: cover;">
                        <div class="movie-overlay">
                            <a href="/detail?id=${movie.movieId}" class="btn-book-now mt-3">${txtDetail}</a>
                        </div>
                    </div>
                    <div class="movie-info p-3 text-center text-white">
                        <h5 class="text-truncate">${movie.title}</h5>
                    </div>
                </div>
            </div>`;
    });
    container.innerHTML = html;
}

function renderComingSoon(movies) {
    const container = document.getElementById('comingSoonSlider');

    if (!container) {
        console.error("Target container for Coming Soon not found in HTML!");
        return;
    }

    if (movies.length === 0) {
        container.innerHTML = `
            <div class="text-center py-5 w-100">
                <h5 class="text-white-50">${txtNoSoon}</h5>
            </div>`;
        return;
    }

    let html = '';
    movies.forEach(movie => {
        const banner = movie.bannerPath || movie.posterPath || 'https://placehold.co/600x337?text=No+Image';
        html += `
            <div class="coming-soon-slider-item" style="min-width: 320px; margin-right: 15px;">
                <div class="coming-soon-card position-relative">
                    <a href="/detail?id=${movie.movieId}">
                        <img src="${banner}" style="height: 200px; width: 100%; object-fit: cover; border-radius: 8px; filter: brightness(0.7);">
                    </a>
                    <div class="position-absolute bottom-0 start-0 p-3 w-100">
                        <span class="badge bg-warning text-dark mb-2">${movie.releaseDate || txtSoon}</span>
                        <h4 class="text-white text-truncate">${movie.title}</h4>
                        <a href="/detail?id=${movie.movieId}" class="btn btn-sm btn-outline-light">${txtDetail}</a>
                    </div>
                </div>
            </div>`;
    });
    container.innerHTML = html;
}