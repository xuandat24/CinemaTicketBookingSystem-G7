let topMoviesChartInstance = null;
let combosChartInstance = null;
let peakHoursChartInstance = null;
let typingTimer;

// ÉP CHART.JS SANG CHẾ ĐỘ LIGHT MODE NHƯNG VẪN TECH
Chart.defaults.color = '#64748b'; // Màu chữ xám nhạt dịu mắt
Chart.defaults.borderColor = 'rgba(0, 0, 0, 0.05)'; // Lưới mờ
Chart.defaults.font.family = "'Poppins', sans-serif";

document.getElementById('occupancyMovieSearch').addEventListener('input', () => {
    clearTimeout(typingTimer);
    typingTimer = setTimeout(loadOccupancy, 500);
});

document.addEventListener("DOMContentLoaded", () => {
    updateDashboard();
    loadOccupancy();
});

// HIỆU ỨNG ĐẾM SỐ (COUNT UP)
function animateValue(id, start, end, duration) {
    if (start === end) return;
    const obj = document.getElementById(id);
    if (!obj) return;

    let startTimestamp = null;
    const step = (timestamp) => {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        let currentVal = Math.floor(progress * (end - start) + start);
        obj.innerHTML = currentVal.toLocaleString('vi-VN');
        if (progress < 1) window.requestAnimationFrame(step);
    };
    window.requestAnimationFrame(step);
}

function parseNumberFromText(text) {
    if (!text) return 0;
    if (typeof text === 'number') return text;
    return parseInt(text.toString().replace(/[^0-9]/g, ''), 10) || 0;
}

// LOGIC GỌI API ĐƯỢC BẢO TOÀN 100%
async function updateDashboard() {
    const timeFilter = document.querySelector('input[name="timeFilter"]:checked').value;
    const token = sessionStorage.getItem("jwtToken");

    try {
        const response = await fetch(`/api/admin/dashboard?filter=${timeFilter}`, {
            method: 'GET',
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!response.ok) throw new Error("Error loading dashboard data");

        const dashboardData = await response.json();
        renderDashboard(dashboardData);

    } catch (error) {
        console.error("Dashboard error:", error);
    }
}

function renderDashboard(data) {
    const revNum = parseNumberFromText(data.totalRevenue);
    const tickNum = parseNumberFromText(data.totalTickets);
    const comboNum = parseNumberFromText(data.totalCombos);

    animateValue('totalRevenue', 0, revNum, 1000);
    animateValue('totalTickets', 0, tickNum, 1000);
    animateValue('totalCombos', 0, comboNum, 1000);

    document.getElementById('topMovieName').innerText = data.topMovieName || "NO DATA";

    // BẢNG MÀU NEON LÊN NỀN SÁNG
    const colorOrange = '#d96c2c';
    const colorBlue = '#0ea5e9';
    const colorPurple = '#a855f7';
    const colorGlass = 'rgba(0, 0, 0, 0.05)'; // Cột mờ xám nhạt

    // BIỂU ĐỒ CỘT (DOANH THU PHIM)
    const ctxTopMovies = document.getElementById('topMoviesChart').getContext('2d');
    if (topMoviesChartInstance) topMoviesChartInstance.destroy();

    let barColors = data.topMovies.labels.map((_, i) => i === 0 ? colorOrange : colorGlass);

    topMoviesChartInstance = new Chart(ctxTopMovies, {
        type: 'bar',
        data: {
            labels: data.topMovies.labels,
            datasets: [{
                label: 'Revenue (VND)',
                data: data.topMovies.data,
                backgroundColor: barColors,
                borderRadius: 6,
                barPercentage: 0.6
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { x: { grid: { display: false } }, y: { beginAtZero: true } }
        }
    });

    // BIỂU ĐỒ DOUGHNUT (PHÂN BỔ COMBO)
    const ctxCombos = document.getElementById('combosChart').getContext('2d');
    if (combosChartInstance) combosChartInstance.destroy();
    combosChartInstance = new Chart(ctxCombos, {
        type: 'doughnut',
        data: {
            labels: data.combos.labels,
            datasets: [{
                data: data.combos.data,
                backgroundColor: [colorOrange, colorBlue, colorPurple, '#eab308', '#f43f5e'],
                borderWidth: 2,
                borderColor: '#ffffff', // Trùng màu nền để cắt khối đẹp
                hoverOffset: 15
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false, cutout: '80%',
            plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, padding: 20 } } }
        }
    });

    // BIỂU ĐỒ ĐƯỜNG LƯỚI NEON (PEAK HOURS)
    const ctxPeak = document.getElementById('peakHoursChart').getContext('2d');
    if (peakHoursChartInstance) peakHoursChartInstance.destroy();
    peakHoursChartInstance = new Chart(ctxPeak, {
        type: 'line',
        data: {
            labels: data.peakHours.labels,
            datasets: [{
                label: 'Tickets',
                data: data.peakHours.data,
                fill: true,
                backgroundColor: 'rgba(14, 165, 233, 0.1)', // Đổ bóng nền xanh dương mờ
                borderColor: colorBlue,
                borderWidth: 3,
                tension: 0.4,
                pointBackgroundColor: '#fff',
                pointBorderColor: colorBlue,
                pointBorderWidth: 2,
                pointRadius: 4, pointHoverRadius: 8
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { x: { grid: { display: false } }, y: { beginAtZero: true, suggestedMax: 5 } }
        }
    });
}

document.querySelectorAll('input[name="timeFilter"]').forEach(radio => {
    radio.addEventListener('change', () => { loadOccupancy(); });
});

// LOGIC BẢNG LẤP ĐẦY ĐƯỢC BẢO TOÀN 100%
async function loadOccupancy() {
    const dateFilter = document.getElementById('occupancyDate') ? document.getElementById('occupancyDate').value : '';
    const searchQuery = document.getElementById('occupancyMovieSearch').value.trim();
    const token = sessionStorage.getItem("jwtToken");

    try {
        const response = await fetch(`/api/admin/dashboard/occupancy?date=${dateFilter}&search=${searchQuery}`, {
            method: 'GET',
            headers: { 'Authorization': 'Bearer ' + token }
        });

        if (!response.ok) throw new Error("Error loading occupancy data");

        const data = await response.json();
        const tbody = document.getElementById('occupancyTableBody');
        tbody.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5" style="color: #64748b;"><i class="fa-solid fa-satellite fs-2 mb-3 d-block"></i> No signals detected in sector</td></tr>';
            return;
        }

        data.forEach(item => {
            let colorClass = '';
            if (item.occupancyRate >= 80) colorClass = 'occupancy-high';
            else if (item.occupancyRate >= 60) colorClass = 'occupancy-medium';
            else colorClass = 'occupancy-low';

            tbody.innerHTML += `
            <tr>
                <td>
                    <div class="d-flex align-items-center">
                        <div class="rounded p-2 me-3" style="background: rgba(0,0,0,0.05); color: #0ea5e9;"><i class="fa-solid fa-film"></i></div>
                        <span class="fw-bold" style="color: #1e293b;">${item.movieName}</span>
                    </div>
                </td>
                <td><span class="badge px-3 py-2 text-dark" style="background: rgba(0,0,0,0.05); border: 1px solid rgba(0,0,0,0.1);"><i class="fa-regular fa-clock me-1 text-warning"></i> ${item.time}</span></td>
                <td><span class="fw-semibold" style="color: #64748b;">${item.roomName}</span></td>
                <td class="text-center fw-bold" style="color: #1e293b;">${item.bookedSeats} <span class="fw-normal" style="color: #64748b;">/ ${item.totalSeats}</span></td>
                <td>
                    <div class="d-flex align-items-center">
                        <span class="me-3 fw-bold" style="width: 45px; color: #1e293b;">${item.occupancyRate}%</span>
                        <div class="progress progress-tech flex-grow-1">
                            <div class="progress-bar ${colorClass}" role="progressbar" style="width: ${item.occupancyRate}%;"></div>
                        </div>
                    </div>
                </td>
            </tr>
        `;
        });
    } catch (e) {
        console.error("Error loading occupancy data", e);
    }
}