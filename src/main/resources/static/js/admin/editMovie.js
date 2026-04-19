const urlParams = new URLSearchParams(window.location.search);
const movieId = urlParams.get('id');

let selectedCategoryIds = [];

document.addEventListener('DOMContentLoaded', async () => {
    if (!movieId) {
        alert("Invalid Movie ID!");
        window.location.href = '/admin/movies/search';
        return;
    }
    await loadCategoryDropdown();
    await loadMovieData();
});

async function loadMovieData() {
    try {
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/movies/${movieId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            throw new Error(`Server returned status ${response.status}`);
        }

        const movie = await response.json();

        const setVal = (id, val) => {
            const el = document.getElementById(id);
            if (el) el.value = val;
        };

        setVal('editMovieId', movie.movieId);
        setVal('movieTitle', movie.title);
        setVal('movieDesc', movie.description);
        setVal('movieDuration', movie.duration);
        setVal('movieReleaseDate', movie.releaseDate);
        setVal('movieStatus', movie.status);
        setVal('movieDirector', movie.director || '');
        setVal('movieActors', movie.actors || '');
        setVal('movieRating', movie.rating || 0.0);
        setVal('movieLanguage', movie.language || 'Unknown');

        if (movie.categoryIds) {
            selectedCategoryIds = movie.categoryIds.map(String);
            renderCategoryTags();
        }

        if (movie.bannerPath && document.getElementById('currentBannerContainer')) {
            document.getElementById('currentBannerContainer').classList.remove('d-none');
            if(document.getElementById('currentBannerImg')) document.getElementById('currentBannerImg').src = movie.bannerPath;
        }

        if (movie.posterPath && document.getElementById('currentPosterContainer')) {
            document.getElementById('currentPosterContainer').classList.remove('d-none');
            if(document.getElementById('currentPosterImg')) document.getElementById('currentPosterImg').src = movie.posterPath;
        }

        if (movie.trailerPath && document.getElementById('currentTrailerContainer')) {
            document.getElementById('currentTrailerContainer').classList.remove('d-none');
            const wrapper = document.getElementById('trailerMediaWrapper');

            if (wrapper) {
                if (movie.trailerPath.includes('youtube.com') || movie.trailerPath.includes('youtu.be')) {
                    wrapper.innerHTML = `<iframe style="width: 100%; height: 120px; border-radius: 5px;" src="${movie.trailerPath}" frameborder="0" allowfullscreen></iframe>`;
                } else {
                    wrapper.innerHTML = `<video controls style="width: 100%; max-height: 120px; border-radius: 5px; background: #000;"><source src="${movie.trailerPath}" type="video/mp4"></video>`;
                }
            }
        }

    } catch (error) {
        console.error("Detail Error:", error);
        alert("Error loading movie data: " + error.message + "\n(Please open F12 -> Console for details)");
    }
}

async function loadCategoryDropdown() {
    const token = sessionStorage.getItem('jwtToken');
    const response = await fetch('/api/admin/categories', {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    const categories = await response.json();
    const select = document.getElementById('movieCategorySelect');
    if(select) {
        select.innerHTML = '<option value="">-- Select category to add --</option>';
        categories.forEach(c => select.innerHTML += `<option value="${c.categoryId}">${c.name}</option>`);
    }
}

function addCategoryTag() {
    const select = document.getElementById('movieCategorySelect');
    const id = select.value;
    if (id && !selectedCategoryIds.includes(id)) {
        selectedCategoryIds.push(id);
        renderCategoryTags();
    }
    select.value = "";
}

function removeCategoryTag(id) {
    selectedCategoryIds = selectedCategoryIds.filter(catId => catId !== id.toString());
    renderCategoryTags();
}

function renderCategoryTags() {
    const container = document.getElementById('selectedCategoryTags');
    if(!container) return;

    container.innerHTML = '';
    const select = document.getElementById('movieCategorySelect');

    selectedCategoryIds.forEach(id => {
        let name = Array.from(select.options).find(opt => opt.value === id)?.text || 'Unknown';
        container.innerHTML += `
            <span class="badge bg-primary text-white border me-2 mb-2 p-2" style="cursor:pointer;" onclick="removeCategoryTag('${id}')" title="Click to remove">
                ${name} <i class="fa-solid fa-xmark ms-1"></i>
            </span>
        `;
    });
}

const editForm = document.getElementById('editMovieForm');
if (editForm) {
    const editForm = document.getElementById('editMovieForm');
    if (editForm) {
        editForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const errorEle = document.getElementById('formValidationError');
            errorEle.style.display = 'none'; // Reset error

            const title = document.getElementById('movieTitle').value.trim();
            const desc = document.getElementById('movieDesc').value.trim();
            const duration = document.getElementById('movieDuration').value;
            const releaseDate = document.getElementById('movieReleaseDate').value;
            const status = document.getElementById('movieStatus').value;
            const language = document.getElementById('movieLanguage').value.trim();
            const director = document.getElementById('movieDirector').value.trim();
            const actors = document.getElementById('movieActors').value.trim();
            const rating = document.getElementById('movieRating').value;

            // INLINE ERROR
            if (!title || !desc || !duration || !releaseDate || !status || !language || !director || !actors || rating === "") {
                errorEle.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> ERROR: Please fill in all required fields!';
                errorEle.style.display = 'block';
                return;
            }

            if (selectedCategoryIds.length === 0) {
                errorEle.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> ERROR: Please add at least one category for the movie!';
                errorEle.style.display = 'block';
                return;
            }

            const btnSubmit = document.getElementById('btnUpdate');
            const originalText = btnSubmit.innerHTML;
            btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Updating...';
            btnSubmit.disabled = true;

            const formData = new FormData();
            formData.append('movieId', document.getElementById('editMovieId').value);
            formData.append('title', title);
            formData.append('description', desc);
            formData.append('duration', duration);
            formData.append('releaseDate', releaseDate);
            formData.append('status', status);
            formData.append('director', director);
            formData.append('actors', actors);
            formData.append('rating', rating);
            formData.append('language', language);

            selectedCategoryIds.forEach(id => formData.append('categoryIds', id));

            const bannerFile = document.getElementById('movieBanner') ? document.getElementById('movieBanner').files[0] : null;
            const posterFile = document.getElementById('moviePosterFile') ? document.getElementById('moviePosterFile').files[0] : null;
            const trailerFile = document.getElementById('movieTrailer') ? document.getElementById('movieTrailer').files[0] : null;

            const bannerUrl = document.getElementById('bannerUrl') ? document.getElementById('bannerUrl').value.trim() : "";
            const posterUrl = document.getElementById('posterUrl') ? document.getElementById('posterUrl').value.trim() : "";
            const trailerUrl = document.getElementById('movieTrailerUrl') ? document.getElementById('movieTrailerUrl').value.trim() : "";

            if (bannerFile) formData.append('bannerFile', bannerFile);
            if (posterFile) formData.append('posterFile', posterFile);
            if (trailerFile) formData.append('trailerFile', trailerFile);

            if (bannerUrl) formData.append('bannerUrl', bannerUrl);
            if (posterUrl) formData.append('posterUrl', posterUrl);
            if (trailerUrl) formData.append('trailerPath', trailerUrl);

            try {
                const token = sessionStorage.getItem('jwtToken');
                const response = await fetch(`/api/admin/movies/${movieId}`, {
                    method: 'PUT',
                    headers: { 'Authorization': `Bearer ${token}` },
                    body: formData
                });
                const data = await response.json();

                // TOAST FOR SERVER RESPONSE
                if (response.ok) {
                    showToast("Movie updated successfully!", "success");
                    setTimeout(() => { window.location.href = '/admin/movies/search'; }, 1500);
                } else {
                    showToast(data.message || "Update failed!", "error");
                }
            } catch (error) {
                showToast("Server connection error!", "error");
            } finally {
                btnSubmit.innerHTML = originalText;
                btnSubmit.disabled = false;
            }
        });
    }

    const btnDelete = document.getElementById('btnDeleteMovie');
    if (btnDelete) {
        btnDelete.addEventListener('click', async () => {
            if(!confirm("WARNING: Are you sure you want to disable this movie?")) return;
            try {
                const token = sessionStorage.getItem('jwtToken');
                const response = await fetch(`/api/admin/movies/${movieId}`, {
                    method: 'DELETE',
                    headers: { 'Authorization': `Bearer ${token}` }
                });

                const data = await response.json();

                if(response.ok) {
                    showToast("Movie has been disabled!", "success");
                    setTimeout(() => { window.location.href = '/admin/movies/search'; }, 1500);
                } else {
                    showToast(data.message || "Cannot disable movie!", "error");
                }
            } catch (error) {
                showToast("Server connection error!", "error");
            }
        });
    }
}