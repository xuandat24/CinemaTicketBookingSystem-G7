document.getElementById('addCategoryForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const nameInputEle = document.getElementById('categoryName');
    const nameInput = nameInputEle.value.trim();
    const errorEle = document.getElementById('nameError');

    // Reset error state before validation
    nameInputEle.classList.remove('is-invalid');
    errorEle.style.display = 'none';

    // VALIDATION 1: Show error below the input if it's empty
    if (!nameInput) {
        nameInputEle.classList.add('is-invalid');
        errorEle.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> Category name cannot be empty!';
        errorEle.style.display = 'block';
        return;
    }

    // VALIDATION 2: Show error if it contains numbers/special characters
    const nameRegex = /^[\p{L}\s]+$/u;
    if (!nameRegex.test(nameInput)) {
        nameInputEle.classList.add('is-invalid');
        errorEle.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> Category name can only contain letters and spaces. Numbers and special characters are forbidden!';
        errorEle.style.display = 'block';
        return;
    }

    const btnSubmit = e.target.querySelector('button[type="submit"]');
    const originalBtnText = btnSubmit.innerHTML;
    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Saving...';
    btnSubmit.disabled = true;

    const categoryData = { name: nameInput };

    try {
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch('/api/admin/categories', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(categoryData)
        });

        const data = await response.json();

        // ONLY USE TOAST POP-UP FOR SERVER RESPONSES
        if (response.ok) {
            showToast("Category added successfully!", "success");
            setTimeout(() => { window.location.href = '/admin/categories'; }, 1500);
        } else {
            showToast(data.message || "Save failed. Please try again!", "error");
        }
    } catch (error) {
        showToast("Error connecting to the server!", "error");
    } finally {
        btnSubmit.innerHTML = originalBtnText;
        btnSubmit.disabled = false;
    }
});