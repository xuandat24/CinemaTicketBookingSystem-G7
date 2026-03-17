document.getElementById('addCategoryForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const btnSubmit = e.target.querySelector('button[type="submit"]')
    const originalBtnText = btnSubmit.innerHTML
    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Saving...'
    btnSubmit.disabled = true

    const categoryData = {
        name: document.getElementById('categoryName').value.trim()
    }

    try {
        const token = localStorage.getItem('jwtToken'); // Lấy token
        const response = await fetch('/api/admin/categories', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}` // Bổ sung Token
            },
            body: JSON.stringify(categoryData)
        });

        const data = await response.json()

        if (response.ok) {
            alert("Success: " + data.message)
            window.location.href = '/admin/categories'
        } else {
            alert("Error: " + (data.message || "Failed to add category."))
        }
    } catch (error) {
        console.error("Error:", error)
        alert("Server connection error!")
    } finally {
        btnSubmit.innerHTML = originalBtnText
        btnSubmit.disabled = false
    }
})