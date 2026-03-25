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
        const token = localStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/movies/${movieId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error("Movie not found");

        const movie = await response.json();

        // Load data cũ
        document.getElementById('editMovieId').value = movie.movieId;
        document.getElementById('movieTitle').value = movie.title;
        document.getElementById('movieDesc').value = movie.description;
        document.getElementById('movieDuration').value = movie.duration;
        document.getElementById('movieReleaseDate').value = movie.releaseDate;
        document.getElementById('movieStatus').value = movie.status;

        // Load data mới
        document.getElementById('movieDirector').value = movie.director || '';
        document.getElementById('movieActors').value = movie.actors  || '';
        document.getElementById('movieRating').value = movie.rating || 0.0;
        document.getElementById('movieLanguage').value = movie.language || 'Unknown';

        if (movie.categoryIds) {
            selectedCategoryIds = movie.categoryIds.map(String);
            renderCategoryTags();
        }

        if (movie.bannerPath) {
            document.getElementById('currentBannerContainer').classList.remove('d-none');
            document.getElementById('currentBannerImg').src = movie.bannerPath;
            document.getElementById('currentBannerPathText').innerText = "Path: " + movie.bannerPath;
        }

        if (movie.trailerPath) {
            document.getElementById('currentTrailerContainer').classList.remove('d-none');
            const videoElement = document.getElementById('currentTrailerVideo');
            videoElement.src = movie.trailerPath;
            videoElement.load();
            document.getElementById('currentTrailerPathText').innerText = "Path: " + movie.trailerPath;
        }

    } catch (error) {
        alert("Failed to load movie data!");
        window.location.href = '/admin/movies/search';
    }
}

async function loadCategoryDropdown() {
    const token = localStorage.getItem('jwtToken');
    const response = await fetch('/api/admin/categories', {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    const categories = await response.json();
    const select = document.getElementById('movieCategorySelect');
    select.innerHTML = '<option value="">-- Select category to add --</option>';
    categories.forEach(c => select.innerHTML += `<option value="${c.categoryId}">${c.name}</option>`);
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

document.getElementById('editMovieForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (selectedCategoryIds.length === 0) return alert("Please add at least one category tag!");

    const btnSubmit = document.getElementById('btnUpdate');
    const originalText = btnSubmit.innerHTML;
    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Updating...';
    btnSubmit.disabled = true;

    const formData = new FormData();
    formData.append('title', document.getElementById('movieTitle').value);
    formData.append('description', document.getElementById('movieDesc').value);
    formData.append('duration', document.getElementById('movieDuration').value);
    formData.append('releaseDate', document.getElementById('movieReleaseDate').value);
    formData.append('status', document.getElementById('movieStatus').value);

    // Gửi data mới xuống BE
    formData.append('director', document.getElementById('movieDirector').value);
    formData.append('actors', document.getElementById('movieActors').value);
    formData.append('rating', document.getElementById('movieRating').value);
    formData.append('language', document.getElementById('movieLanguage').value);
    selectedCategoryIds.forEach(id => formData.append('categoryIds', id));

    const bannerFile = document.getElementById('movieBanner').files[0];
    const trailerFile = document.getElementById('movieTrailer').files[0];

    // Validate định dạng
    if (bannerFile && !bannerFile.type.startsWith('image/')) {
        alert("Error: Banner must be PNG or JPG!");
        btnSubmit.innerHTML = originalText; btnSubmit.disabled = false; return;
    }
    if (trailerFile && trailerFile.type !== 'video/mp4') {
        alert("Lỗi: Trailer must be MP4 file!");
        btnSubmit.innerHTML = originalText; btnSubmit.disabled = false; return;
    }

    if (bannerFile) formData.append('bannerFile', bannerFile);
    if (trailerFile) formData.append('trailerFile', trailerFile);

    try {
        const token = localStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/movies/${movieId}`, {
            method: 'PUT',
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
            alert("Error: " + (data.message || "Failed to update movie."));
        }
    } catch (error) {
        alert("Server connection error!");
    } finally {
        btnSubmit.innerHTML = originalText;
        btnSubmit.disabled = false;
    }
});

document.getElementById('btnDeleteMovie').addEventListener('click', async () => {
    if(!confirm("DANGER: Are you absolutely sure you want to delete this movie? This action will disable the movie.")) return;
    try{
        const token = localStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/movies/${movieId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` } // Thêm token
        });

        const data = await response.json();

        if(response.ok){
            alert("Success: " + data.message);
            window.location.href = '/admin/movies/search';
        } else {
            alert("Error: " + (data.message || "Failed to delete movie"));
        }
    } catch (error) {
        alert("There is some error when trying to delete movie");
        console.error("Delete movie error:", error);
    }
});