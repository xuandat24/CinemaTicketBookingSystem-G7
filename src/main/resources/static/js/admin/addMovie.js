const OMDB_API_KEY = '9b0b8e47';
let selectedCategoryIds = [];
let existingMovieTitles = [];

let typingTimer;
const doneTypingInterval = 1300;
const randomKeywords = ['Animation', 'Comedy', 'Batman', 'Marvel', 'Star Wars', 'Matrix', 'Action', 'War', 'City', 'Space', 'Ocean', 'King', 'Magic'];

document.addEventListener('DOMContentLoaded', async () => {
    loadCategoryDropdown();
    await fetchExistingMovies();

    fetchSuggestedMovies();

    const omdbInput = document.getElementById('omdbSearchInput');
    if (omdbInput) {
        omdbInput.addEventListener('input', function () {
            clearTimeout(typingTimer);
            const keyword = omdbInput.value.trim();

            if (keyword !== '') {
                typingTimer = setTimeout(() => {
                    searchOMDb(keyword);
                }, doneTypingInterval);
            } else {
                fetchSuggestedMovies();
            }
        });

        omdbInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') e.preventDefault();
        });
    }
});

async function fetchSuggestedMovies() {
    const randomKeyword = randomKeywords[Math.floor(Math.random() * randomKeywords.length)];

    document.getElementById('omdbLoading').classList.remove('d-none');
    document.getElementById('omdbResults').innerHTML = '';

    try {
        const res = await fetch(`https://www.omdbapi.com/?apikey=${OMDB_API_KEY}&s=${encodeURIComponent(randomKeyword)}&type=movie`);
        const data = await res.json();

        if (data.Response === "True") {
            renderOmdbResults(data.Search);
        }
    } catch (error) {
        console.error("Error when connect with OMDb API!");
    } finally {
        document.getElementById('omdbLoading').classList.add('d-none');
    }
}

async function fetchExistingMovies() {
    try {
        const token = localStorage.getItem('jwtToken');
        const res = await fetch('/api/admin/movies', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const movies = await res.json();
        existingMovieTitles = movies.map(m => m.title.toLowerCase());
    } catch (e) { console.log("Failed to load existing movies"); }
}

function openBlankForm() {
    document.getElementById('addMovieForm').reset();
    document.getElementById('omdbPosterUrl').value = "";
    document.getElementById('omdbGenres').value = "";
    document.getElementById('omdbPosterContainer').classList.add('d-none');

    document.getElementById('omdbSection').classList.add('d-none');
    document.getElementById('addMovieForm').classList.remove('d-none');
}

function backToOMDb() {
    document.getElementById('omdbSection').classList.remove('d-none');
    document.getElementById('addMovieForm').classList.add('d-none');
}

async function searchOMDb(keyword = null) {
    const query = keyword || document.getElementById('omdbSearchInput').value.trim();
    if (!query) return;

    document.getElementById('omdbLoading').classList.remove('d-none');
    document.getElementById('omdbResults').innerHTML = '';

    try {
        const res = await fetch(`https://www.omdbapi.com/?apikey=${OMDB_API_KEY}&s=${encodeURIComponent(query)}&type=movie`);
        const data = await res.json();

        if (data.Response === "True") {
            renderOmdbResults(data.Search);
        } else {
            document.getElementById('omdbResults').innerHTML = `<p class="text-center text-danger fw-bold w-100">${data.Error}</p>`;
        }
    } catch (error) {
        alert("Lỗi kết nối OMDb API!");
    } finally {
        document.getElementById('omdbLoading').classList.add('d-none');
    }
}

function renderOmdbResults(movies) {
    const container = document.getElementById('omdbResults');
    const defaultPoster = 'https://placehold.co/300x450/eeeeee/999999?text=No+Poster';

    movies.forEach(movie => {
        const isAdded = existingMovieTitles.includes(movie.Title.toLowerCase());
        const btnHtml = isAdded
            ? `<button class="btn btn-secondary btn-sm w-100" disabled><i class="fa-solid fa-check"></i> Already Added</button>`
            : `<button class="btn btn-primary btn-sm w-100 fw-bold" id="btn-add-${movie.imdbID}" onclick="openPrefilledForm('${movie.imdbID}')"><i class="fa-solid fa-download"></i> Select</button>`;

        const poster = (movie.Poster && movie.Poster !== "N/A") ? movie.Poster : defaultPoster;

        container.innerHTML += `
            <div class="col-md-3 col-sm-6 mb-4">
                <div class="card h-100 shadow-sm border-0 bg-white">
                    <img src="${poster}" onerror="this.onerror=null; this.src='${defaultPoster}';" class="card-img-top" alt="poster" style="height: 300px; object-fit: cover;">
                    <div class="card-body p-3 text-center d-flex flex-column justify-content-between">
                        <div>
                            <h6 class="card-title text-truncate mb-1 fw-bold" title="${movie.Title}">${movie.Title}</h6>
                            <small class="text-muted d-block mb-3">${movie.Year}</small>
                        </div>
                        ${btnHtml}
                    </div>
                </div>
            </div>`;
    });
}
async function openPrefilledForm(imdbID) {
    const btn = document.getElementById(`btn-add-${imdbID}`);
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Loading...';
    btn.disabled = true;

    try {
        const detailRes = await fetch(`https://www.omdbapi.com/?apikey=${OMDB_API_KEY}&i=${imdbID}`);
        const movieData = await detailRes.json();

        const runtime = parseInt(movieData.Runtime) || 120;
        const rating = parseFloat(movieData.imdbRating) || 0.0;
        let releaseDate = new Date().toISOString().split('T')[0];
        if (movieData.Released !== "N/A" && !isNaN(new Date(movieData.Released))) {
            releaseDate = new Date(movieData.Released).toISOString().split('T')[0];
        }

        document.getElementById('movieTitle').value = movieData.Title;
        document.getElementById('movieDesc').value = movieData.Plot !== "N/A" ? movieData.Plot : "";
        document.getElementById('movieDuration').value = runtime;
        document.getElementById('movieReleaseDate').value = releaseDate;
        document.getElementById('movieDirector').value = movieData.Director !== "N/A" ? movieData.Director : "Unknown";
        document.getElementById('movieActors').value = movieData.Actors !== "N/A" ? movieData.Actors : "Unknown";
        document.getElementById('movieRating').value = rating;

        document.getElementById('omdbGenres').value = movieData.Genre !== "N/A" ? movieData.Genre : "";

        if (movieData.Poster !== "N/A") {
            document.getElementById('omdbPosterUrl').value = movieData.Poster;
            document.getElementById('omdbPosterPreview').src = movieData.Poster;
            document.getElementById('omdbPosterContainer').classList.remove('d-none');
        } else {
            document.getElementById('omdbPosterUrl').value = "";
            document.getElementById('omdbPosterContainer').classList.add('d-none');
        }

        document.getElementById('omdbSection').classList.add('d-none');
        document.getElementById('addMovieForm').classList.remove('d-none');

    } catch (error) {
        alert("Lỗi khi tải chi tiết phim từ OMDb!");
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}

async function loadCategoryDropdown() {
    const token = localStorage.getItem('jwtToken');
    const response = await fetch('/api/admin/categories', {
         headers: { 'Authorization': `Bearer ${token}` }
    });
    const categories = await response.json();
    const select = document.getElementById('movieCategorySelect');
    categories.forEach(c => select.innerHTML += `<option value="${c.categoryId}">${c.name}</option>`);
}

function addCategoryTag() {
    const select = document.getElementById('movieCategorySelect');
    const id = select.value;
    if (id && !selectedCategoryIds.includes(id)) { selectedCategoryIds.push(id); renderCategoryTags(); }
    select.value = "";
}
function removeCategoryTag(id) {
    selectedCategoryIds = selectedCategoryIds.filter(catId => catId !== id.toString()); renderCategoryTags();
}
function renderCategoryTags() {
    const container = document.getElementById('selectedCategoryTags'); container.innerHTML = '';
    const select = document.getElementById('movieCategorySelect');
    selectedCategoryIds.forEach(id => {
        let name = Array.from(select.options).find(opt => opt.value === id)?.text || 'Unknown';
        container.innerHTML += `<span class="badge bg-success text-white border me-2 p-2" onclick="removeCategoryTag('${id}')" style="cursor:pointer">${name} <i class="fa-solid fa-xmark"></i></span>`;
    });
}

document.getElementById('addMovieForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const omdbGenres = document.getElementById('omdbGenres').value;
    const omdbPosterUrl = document.getElementById('omdbPosterUrl').value;

    if (selectedCategoryIds.length === 0 && !omdbGenres) return alert("Chose at least 1 category!");

    const bannerFile = document.getElementById('movieBanner').files[0];
    const trailerFile = document.getElementById('movieTrailer').files[0];

    if (!bannerFile && !omdbPosterUrl) return alert("Chose the banner/poster!");

    const btnSubmit = e.target.querySelector('button[type="submit"]');
    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Saving...';
    btnSubmit.disabled = true;

    const formData = new FormData();
    formData.append('title', document.getElementById('movieTitle').value);
    formData.append('description', document.getElementById('movieDesc').value);
    formData.append('duration', document.getElementById('movieDuration').value);
    formData.append('releaseDate', document.getElementById('movieReleaseDate').value);
    formData.append('status', document.getElementById('movieStatus').value);
    formData.append('director', document.getElementById('movieDirector').value);
    formData.append('actors', document.getElementById('movieActors').value);
    formData.append('rating', document.getElementById('movieRating').value);

    selectedCategoryIds.forEach(id => formData.append('categoryIds', id));
    if (omdbGenres) formData.append('omdbGenres', omdbGenres);
    if (omdbPosterUrl) formData.append('omdbPosterUrl', omdbPosterUrl);

    if (bannerFile) formData.append('bannerFile', bannerFile);
    if (trailerFile) formData.append('trailerFile', trailerFile);

    try {
        const token = localStorage.getItem('jwtToken');
        const response = await fetch('/api/admin/movies', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });
        const data = await response.json();
        if (response.ok) {
            alert("Success: " + data.message);
            window.location.href = '/admin/movies/search';
        } else {
            alert("Error: " + (data.message || "Lỗi lưu phim!"));
            btnSubmit.innerHTML = '<i class="fa-solid fa-floppy-disk me-1"></i> Save Movie';
            btnSubmit.disabled = false;
        }
    } catch (error) {
        alert("Server connection error.");
        btnSubmit.disabled = false;
    }
});