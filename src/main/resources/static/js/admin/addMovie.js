const TMDB_API_KEY = '6e00f020b46f9229dc7ed4f23470ca40';
const TMDB_IMAGE_BASE_URL = 'https://image.tmdb.org/t/p/w500';
const TMDB_BANNER_BASE_URL = 'https://image.tmdb.org/t/p/original';

let selectedCategoryIds = [];
let existingMovieTitles = [];

let typingTimer;
const doneTypingInterval = 1000;

document.addEventListener('DOMContentLoaded', async () => {
    loadCategoryDropdown();
    await fetchExistingMovies();
    fetchSuggestedMovies();

    const searchInput = document.getElementById('apiSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            clearTimeout(typingTimer);
            const keyword = searchInput.value.trim();

            if (keyword !== '') {
                typingTimer = setTimeout(() => {
                    searchTMDB(keyword);
                }, doneTypingInterval);
            } else {
                fetchSuggestedMovies();
            }
        });

        searchInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') e.preventDefault();
        });
    }
});

// Fetch random popular movies
async function fetchSuggestedMovies() {
    const loadingEl = document.getElementById('apiLoading');
    const resultsEl = document.getElementById('apiResults');
    if (!loadingEl || !resultsEl) return;

    loadingEl.classList.remove('d-none');
    resultsEl.innerHTML = '';

    try {
        const randomPage = Math.floor(Math.random() * 5) + 1;
        const res = await fetch(`https://api.themoviedb.org/3/movie/popular?api_key=${TMDB_API_KEY}&language=en-US&page=${randomPage}`);
        const data = await res.json();

        if (data.results && data.results.length > 0) {
            const shuffledMovies = data.results.sort(() => 0.5 - Math.random());
            renderTmdbResults(shuffledMovies.slice(0, 12));
        }
    } catch (error) {
        console.error("TMDB API Error", error);
    } finally {
        loadingEl.classList.add('d-none');
    }
}

async function fetchExistingMovies() {
    try {
        const token = localStorage.getItem('jwtToken');
        const res = await fetch('/api/admin/movies', { headers: { 'Authorization': `Bearer ${token}` } });
        const movies = await res.json();
        existingMovieTitles = movies.map(m => m.title.toLowerCase());
    } catch (e) { console.log("Failed to load existing movies"); }
}

function openBlankForm() {
    document.getElementById('addMovieForm').reset();

    if(document.getElementById('posterUrl')) document.getElementById('posterUrl').value = "";
    if(document.getElementById('bannerUrl')) document.getElementById('bannerUrl').value = "";
    if(document.getElementById('genres')) document.getElementById('genres').value = "";
    if(document.getElementById('movieTrailerUrl')) document.getElementById('movieTrailerUrl').value = "";

    if(document.getElementById('posterPreviewContainer')) document.getElementById('posterPreviewContainer').classList.add('d-none');
    if(document.getElementById('bannerPreviewContainer')) document.getElementById('bannerPreviewContainer').classList.add('d-none');
    if(document.getElementById('trailerPreviewContainer')) {
        document.getElementById('trailerPreviewContainer').classList.add('d-none');
        document.getElementById('trailerPreview').src = "";
    }

    selectedCategoryIds = [];
    renderCategoryTags();

    document.getElementById('apiSection').classList.add('d-none');
    document.getElementById('addMovieForm').classList.remove('d-none');
}

function backToSearch() {
    document.getElementById('apiSection').classList.remove('d-none');
    document.getElementById('addMovieForm').classList.add('d-none');
}

async function searchTMDB(keyword) {
    if (!keyword) return;
    document.getElementById('apiLoading').classList.remove('d-none');
    document.getElementById('apiResults').innerHTML = '';

    try {
        const res = await fetch(`https://api.themoviedb.org/3/search/movie?api_key=${TMDB_API_KEY}&query=${encodeURIComponent(keyword)}&language=en-US`);
        const data = await res.json();

        if (data.results && data.results.length > 0) {
            renderTmdbResults(data.results);
        } else {
            document.getElementById('apiResults').innerHTML = `<p class="text-center text-danger fw-bold w-100">No movies found.</p>`;
        }
    } catch (error) {
        alert("Error connecting to TMDB API!");
    } finally {
        document.getElementById('apiLoading').classList.add('d-none');
    }
}

function renderTmdbResults(movies) {
    const container = document.getElementById('apiResults');
    const defaultPoster = 'https://placehold.co/300x450/eeeeee/999999?text=No+Poster';
    container.innerHTML = '';

    movies.forEach(movie => {
        const isAdded = existingMovieTitles.includes(movie.title.toLowerCase());
        const btnHtml = isAdded
            ? `<button class="btn btn-secondary btn-sm w-100" disabled><i class="fa-solid fa-check"></i> Added</button>`
            : `<button class="btn btn-primary btn-sm w-100 fw-bold" id="btn-add-${movie.id}" onclick="openPrefilledForm(${movie.id})"><i class="fa-solid fa-download"></i> Select</button>`;

        const poster = movie.poster_path ? `${TMDB_IMAGE_BASE_URL}${movie.poster_path}` : defaultPoster;
        const year = movie.release_date ? movie.release_date.substring(0, 4) : 'N/A';

        container.innerHTML += `
            <div class="col-md-3 col-sm-6 mb-4">
                <div class="card h-100 shadow-sm border-0 bg-white">
                    <img src="${poster}" onerror="this.onerror=null; this.src='${defaultPoster}';" class="card-img-top" alt="poster" style="height: 300px; object-fit: cover;">
                    <div class="card-body p-3 text-center d-flex flex-column justify-content-between">
                        <div>
                            <h6 class="card-title text-truncate mb-1 fw-bold" title="${movie.title}">${movie.title}</h6>
                            <small class="text-muted d-block mb-3">${year}</small>
                        </div>
                        ${btnHtml}
                    </div>
                </div>
            </div>`;
    });
}

async function openPrefilledForm(tmdbID) {
    const btn = document.getElementById(`btn-add-${tmdbID}`);
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Loading...';
    btn.disabled = true;

    try {
        const detailRes = await fetch(`https://api.themoviedb.org/3/movie/${tmdbID}?api_key=${TMDB_API_KEY}&append_to_response=credits,videos&language=en-US`);
        const movieData = await detailRes.json();

        const genresString = movieData.genres ? movieData.genres.map(g => g.name).join(', ') : "";
        const actorsString = (movieData.credits && movieData.credits.cast) ? movieData.credits.cast.slice(0, 5).map(a => a.name).join(', ') : "Unknown";
        const directorObj = (movieData.credits && movieData.credits.crew) ? movieData.credits.crew.find(c => c.job === 'Director') : null;
        const directorString = directorObj ? directorObj.name : "Unknown";

        let languageString = "Unknown";
        if (movieData.spoken_languages && movieData.spoken_languages.length > 0) {
            languageString = movieData.spoken_languages[0].english_name;
        } else if (movieData.original_language) {
            languageString = movieData.original_language.toUpperCase();
        }

        let trailerUrl = "";
        if (movieData.videos && movieData.videos.results && movieData.videos.results.length > 0) {
            const trailerVideo = movieData.videos.results.find(vid => vid.site === "YouTube" && vid.type === "Trailer");
            if (trailerVideo) {
                trailerUrl = `https://www.youtube.com/embed/${trailerVideo.key}`;
            } else {
                const anyYoutubeVideo = movieData.videos.results.find(vid => vid.site === "YouTube");
                if(anyYoutubeVideo) trailerUrl = `https://www.youtube.com/embed/${anyYoutubeVideo.key}`;
            }
        }

        document.getElementById('movieTitle').value = movieData.title || "";
        document.getElementById('movieDesc').value = movieData.overview || "";
        document.getElementById('movieDuration').value = movieData.runtime || 120;
        document.getElementById('movieReleaseDate').value = movieData.release_date || new Date().toISOString().split('T')[0];
        document.getElementById('movieDirector').value = directorString;
        document.getElementById('movieActors').value = actorsString;
        document.getElementById('movieRating').value = movieData.vote_average ? parseFloat(movieData.vote_average.toFixed(1)) : 0.0;
        document.getElementById('movieLanguage').value = languageString;
        if(document.getElementById('genres')) document.getElementById('genres').value = genresString;

        if (movieData.backdrop_path) {
            const fullBannerUrl = `${TMDB_BANNER_BASE_URL}${movieData.backdrop_path}`;
            document.getElementById('bannerUrl').value = fullBannerUrl;
            if (document.getElementById('bannerPreview')) {
                document.getElementById('bannerPreview').src = fullBannerUrl;
                document.getElementById('bannerPreviewContainer').classList.remove('d-none');
            }
        } else if (movieData.poster_path) {
            const backupBannerUrl = `${TMDB_IMAGE_BASE_URL}${movieData.poster_path}`;
            document.getElementById('bannerUrl').value = backupBannerUrl;
            if (document.getElementById('bannerPreview')) {
                document.getElementById('bannerPreview').src = backupBannerUrl;
                document.getElementById('bannerPreviewContainer').classList.remove('d-none');
            }
        } else {
            document.getElementById('bannerUrl').value = "";
            if (document.getElementById('bannerPreviewContainer')) document.getElementById('bannerPreviewContainer').classList.add('d-none');
        }

        if (movieData.poster_path) {
            const fullPosterUrl = `${TMDB_IMAGE_BASE_URL}${movieData.poster_path}`;
            document.getElementById('posterUrl').value = fullPosterUrl;
            if (document.getElementById('posterPreview')) {
                document.getElementById('posterPreview').src = fullPosterUrl;
                document.getElementById('posterPreviewContainer').classList.remove('d-none');
            }
        } else {
            document.getElementById('posterUrl').value = "";
            if (document.getElementById('posterPreviewContainer')) document.getElementById('posterPreviewContainer').classList.add('d-none');
        }

        if (trailerUrl) {
            document.getElementById('movieTrailerUrl').value = trailerUrl;
            if (document.getElementById('trailerPreview')) {
                document.getElementById('trailerPreview').src = trailerUrl;
                document.getElementById('trailerPreviewContainer').classList.remove('d-none');
            }
        } else {
            document.getElementById('movieTrailerUrl').value = "";
            if (document.getElementById('trailerPreviewContainer')) document.getElementById('trailerPreviewContainer').classList.add('d-none');
        }

        document.getElementById('apiSection').classList.add('d-none');
        document.getElementById('addMovieForm').classList.remove('d-none');

    } catch (error) {
        alert("Error loading movie details from TMDB!");
        console.error(error);
    } finally {
        if(btn) { btn.innerHTML = originalText; btn.disabled = false; }
    }
}

async function loadCategoryDropdown() {
    const token = localStorage.getItem('jwtToken');
    const response = await fetch('/api/admin/categories', { headers: { 'Authorization': `Bearer ${token}` } });
    const categories = await response.json();
    const select = document.getElementById('movieCategorySelect');
    if (select) categories.forEach(c => select.innerHTML += `<option value="${c.categoryId}">${c.name}</option>`);
}

function addCategoryTag() {
    const select = document.getElementById('movieCategorySelect');
    const id = select.value;
    if (id && !selectedCategoryIds.includes(id)) { selectedCategoryIds.push(id); renderCategoryTags(); }
    select.value = "";
}

function removeCategoryTag(id) {
    selectedCategoryIds = selectedCategoryIds.filter(catId => catId !== id.toString());
    renderCategoryTags();
}

function renderCategoryTags() {
    const container = document.getElementById('selectedCategoryTags');
    if (!container) return;

    container.innerHTML = selectedCategoryIds.length === 0 ? '<span class="text-muted small">No categories selected yet.</span>' : '';
    const select = document.getElementById('movieCategorySelect');
    selectedCategoryIds.forEach(id => {
        let name = Array.from(select.options).find(opt => opt.value === id)?.text || 'Unknown';
        container.innerHTML += `<span class="badge bg-success text-white border me-2 p-2" onclick="removeCategoryTag('${id}')" style="cursor:pointer">${name} <i class="fa-solid fa-xmark ms-1"></i></span>`;
    });
}

const formEl = document.getElementById('addMovieForm');
if (formEl) {
    formEl.addEventListener('submit', async (e) => {
        e.preventDefault();

        const genresEl = document.getElementById('genres');
        const genres = genresEl ? genresEl.value.trim() : "";
        if (selectedCategoryIds.length === 0 && !genres) {
            return alert("Please select at least 1 category for the movie!");
        }

        const bannerFile = document.getElementById('movieBanner') ? document.getElementById('movieBanner').files[0] : null;
        const posterFile = document.getElementById('moviePosterFile') ? document.getElementById('moviePosterFile').files[0] : null;
        const trailerFile = document.getElementById('movieTrailer') ? document.getElementById('movieTrailer').files[0] : null;

        const bannerUrl = document.getElementById('bannerUrl') ? document.getElementById('bannerUrl').value.trim() : "";
        const posterUrl = document.getElementById('posterUrl') ? document.getElementById('posterUrl').value.trim() : "";
        const trailerUrl = document.getElementById('movieTrailerUrl') ? document.getElementById('movieTrailerUrl').value.trim() : "";

        if (!bannerFile && !bannerUrl) return alert("Please upload a Banner image or provide a TMDB Banner link!");
        if (!posterFile && !posterUrl) return alert("Please upload a Poster image or provide a TMDB Poster link!");

        const btnSubmit = e.target.querySelector('button[type="submit"]');
        const originalBtnText = btnSubmit.innerHTML;
        btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Saving...';
        btnSubmit.disabled = true;

        const formData = new FormData();
        formData.append('title', document.getElementById('movieTitle').value.trim());
        formData.append('description', document.getElementById('movieDesc').value.trim());
        formData.append('duration', document.getElementById('movieDuration').value);
        formData.append('releaseDate', document.getElementById('movieReleaseDate').value);
        formData.append('status', document.getElementById('movieStatus').value);
        formData.append('language', document.getElementById('movieLanguage').value.trim());
        formData.append('director', document.getElementById('movieDirector').value.trim());
        formData.append('actors', document.getElementById('movieActors').value.trim());
        formData.append('rating', document.getElementById('movieRating').value);

        selectedCategoryIds.forEach(id => formData.append('categoryIds', id));
        if (genres) formData.append('genres', genres);

        if (bannerFile) formData.append('bannerFile', bannerFile);
        if (posterFile) formData.append('posterFile', posterFile);
        if (trailerFile) formData.append('trailerFile', trailerFile);

        if (bannerUrl) formData.append('bannerUrl', bannerUrl);
        if (posterUrl) formData.append('posterUrl', posterUrl);
        if (trailerUrl) formData.append('trailerPath', trailerUrl);

        try {
            const token = localStorage.getItem('jwtToken');
            const response = await fetch('/api/admin/movies', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });

            const data = await response.json();

            if (response.ok) {
                alert("Success: " + data.message);
                window.location.href = '/admin/movies/search';
            } else {
                alert("Error: " + (data.message || "Failed to save movie! Please try again."));
                btnSubmit.innerHTML = originalBtnText;
                btnSubmit.disabled = false;
            }
        } catch (error) {
            console.error("Connection error:", error);
            alert("Error connecting to the server. Please try again later.");
            btnSubmit.innerHTML = originalBtnText;
            btnSubmit.disabled = false;
        }
    });
}