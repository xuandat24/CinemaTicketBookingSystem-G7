document.addEventListener("DOMContentLoaded", function () {
    const movieInput = document.getElementById("filterMovie");
    const dateInput = document.getElementById("filterDate");
    const sortSelect = document.getElementById("sortTickets");
    const resetButton = document.getElementById("resetFiltersBtn");

    if (!movieInput || !dateInput || !sortSelect) {
        return;
    }

    const listContainer = document.getElementById("ticketListContainer")
        || document.getElementById("historyListContainer");

    if (!listContainer) {
        return;
    }

    const getItems = () =>
        Array.from(listContainer.querySelectorAll("[data-movie][data-date-iso][data-timestamp]"));

    const applyFilters = () => {
        const movieKeyword = (movieInput.value || "").trim().toLowerCase();
        const selectedDate = dateInput.value || "";

        getItems().forEach((item) => {
            const itemMovie = (item.dataset.movie || "").toLowerCase();
            const itemDate = item.dataset.dateIso || "";

            const movieMatched = !movieKeyword || itemMovie.includes(movieKeyword);
            const dateMatched = !selectedDate || itemDate === selectedDate;

            item.style.display = movieMatched && dateMatched ? "" : "none";
        });
    };

    const applySort = () => {
        const items = getItems();
        const order = sortSelect.value === "oldest" ? "oldest" : "newest";

        items.sort((a, b) => {
            const tsA = parseInt(a.dataset.timestamp || "0", 10);
            const tsB = parseInt(b.dataset.timestamp || "0", 10);
            return order === "oldest" ? tsA - tsB : tsB - tsA;
        });

        items.forEach((item) => listContainer.appendChild(item));
        applyFilters();
    };

    movieInput.addEventListener("input", applyFilters);
    dateInput.addEventListener("change", applyFilters);
    sortSelect.addEventListener("change", applySort);

    if (resetButton) {
        resetButton.addEventListener("click", () => {
            movieInput.value = "";
            dateInput.value = "";
            sortSelect.value = "newest";
            applySort();
        });
    }

    applySort();
});
