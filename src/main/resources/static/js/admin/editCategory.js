const urlParams = new URLSearchParams(window.location.search);
const categoryId = urlParams.get('id');

document.addEventListener('DOMContentLoaded', () => {
    if (categoryId) {
        loadCategoryData();
    } else {
        alert("Invalid Category ID!");
        window.location.href = '/admin/categories';
    }
});

async function loadCategoryData() {
    try {
        const response = await fetch(`/api/admin/categories/${categoryId}`);
        if (!response.ok) throw new Error("Category not found");

        const category = await response.json();
        document.getElementById('editCategoryName').value = category.name;
    } catch (error) {
        alert("Failed to load category data!");
        window.location.href = '/admin/categories';
    }
}

document.getElementById('editCategoryForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const categoryData = {
        name: document.getElementById('editCategoryName').value.trim()
    };

    try {
        const response = await fetch(`/api/admin/categories/${categoryId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(categoryData)
        });

        const data = await response.json();
        if(response.ok) {
            alert("Success: " + data.message);
            window.location.href = '/admin/categories';
        } else {
            alert("Error: " + data.message);
        }
    } catch (error) {
        alert("Server connection error!");
    }
});

document.getElementById('btnDeleteCategory').addEventListener('click', async () => {
    if(!confirm("WARNING: Are you sure you want to delete this category?\nIf any movies are using this category, deletion might fail.")) return;

    try {
        const response = await fetch(`/api/admin/categories/${categoryId}`, { method: 'DELETE' });
        const data = await response.json();

        if(response.ok) {
            alert("Success: " + data.message);
            window.location.href = '/admin/categories';
        } else {
            alert("Error: " + data.message);
        }
    } catch (error) {
        alert("Server connection error!");
    }
});