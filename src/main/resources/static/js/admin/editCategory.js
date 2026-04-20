const urlParams = new URLSearchParams(window.location.search);
const categoryId = urlParams.get('id');

document.addEventListener('DOMContentLoaded', async () => {
    if (!categoryId) {
        showToast("Category ID not found!", "error");
        setTimeout(() => { window.location.href = '/admin/categories'; }, 1500);
        return;
    }
    await loadCategoryData();
});

async function loadCategoryData() {
    try {
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/categories/${categoryId}`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error("Category not found");

        const category = await response.json();
        document.getElementById('editCategoryName').value = category.name;
    } catch (error) {
        showToast("Cannot load category data!", "error");
        setTimeout(() => { window.location.href = '/admin/categories'; }, 1500);
    }
}

document.getElementById('editCategoryForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const nameInputEle = document.getElementById('editCategoryName');
    const nameInput = nameInputEle.value.trim();
    const errorEle = document.getElementById('nameError');

    // Reset error state
    nameInputEle.classList.remove('is-invalid');
    errorEle.style.display = 'none';

    // INLINE ERROR BELOW INPUT
    if (!nameInput) {
        nameInputEle.classList.add('is-invalid');
        errorEle.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> Category name must not be empty!';
        errorEle.style.display = 'block';
        return;
    }

    const nameRegex = /^[\p{L}\s]+$/u;
    if (!nameRegex.test(nameInput)) {
        nameInputEle.classList.add('is-invalid');
        errorEle.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> FORBIDDEN: Numbers and special characters are not allowed in category name!';
        errorEle.style.display = 'block';
        return;
    }

    const btnSubmit = e.target.querySelector('button[type="submit"]');
    const originalText = btnSubmit.innerHTML;
    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Updating...';
    btnSubmit.disabled = true;

    try {
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/categories/${categoryId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ name: nameInput })
        });

        const data = await response.json();

        // USE TOAST ONLY FOR SERVER RESPONSES
        if(response.ok) {
            showToast("Updated successfully!", "success");
            setTimeout(() => { window.location.href = '/admin/categories'; }, 1500);
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

document.getElementById('btnDeleteCategory').addEventListener('click', async () => {
    if(!confirm("WARNING: Deleting this category may affect movies. Are you sure?")) return;

    const btnDelete = document.getElementById('btnDeleteCategory');
    const originalText = btnDelete.innerHTML;
    btnDelete.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Deleting...';
    btnDelete.disabled = true;

    try {
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/categories/${categoryId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const data = await response.json();

        if(response.ok) {
            showToast("Category deleted successfully!", "success");
            setTimeout(() => { window.location.href = '/admin/categories'; }, 1500);
        } else {
            showToast(data.message || "Delete failed!", "error");
        }
    } catch (error) {
        showToast("Server connection error!", "error");
    } finally {
        btnDelete.innerHTML = originalText;
        btnDelete.disabled = false;
    }
});