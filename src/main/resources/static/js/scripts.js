// src/main/resources/static/js/scripts.js

document.addEventListener('DOMContentLoaded', function() {
    // Initialize date time pickers with sensible defaults
    const startTimeInput = document.getElementById('startTime');
    const endTimeInput = document.getElementById('endTime');

    if (startTimeInput) {
        // Set default to current time, rounded to next hour
        const now = new Date();
        now.setMinutes(0);
        now.setHours(now.getHours() + 1);
        startTimeInput.valueAsDate = now;

        // When start time changes, update end time to be 1 hour later by default
        startTimeInput.addEventListener('change', function() {
            if (endTimeInput) {
                const startTime = new Date(startTimeInput.value);
                const endTime = new Date(startTime);
                endTime.setHours(endTime.getHours() + 1);

                // Format for datetime-local input
                const year = endTime.getFullYear();
                const month = String(endTime.getMonth() + 1).padStart(2, '0');
                const day = String(endTime.getDate()).padStart(2, '0');
                const hours = String(endTime.getHours()).padStart(2, '0');
                const minutes = String(endTime.getMinutes()).padStart(2, '0');

                endTimeInput.value = `${year}-${month}-${day}T${hours}:${minutes}`;
            }
        });

        // Trigger the event to set initial end time
        const event = new Event('change');
        startTimeInput.dispatchEvent(event);
    }

    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const closeButton = alert.querySelector('.btn-close');
            if (closeButton) {
                closeButton.click();
            }
        }, 5000);
    });

    // Confirmation for delete actions
    const deleteButtons = document.querySelectorAll('button[data-confirm], a[data-confirm]');
    deleteButtons.forEach(function(button) {
        button.addEventListener('click', function(event) {
            const message = button.getAttribute('data-confirm') || 'Are you sure you want to proceed?';
            if (!confirm(message)) {
                event.preventDefault();
            }
        });
    });

    // License plate formatting (uppercase)
    const licensePlateInputs = document.querySelectorAll('input[name="licensePlate"]');
    licensePlateInputs.forEach(function(input) {
        input.addEventListener('input', function() {
            this.value = this.value.toUpperCase();
        });
    });

    // Vehicle entry form validation
    const vehicleEntryForm = document.querySelector('form[action*="/checkin"]');
    if (vehicleEntryForm) {
        vehicleEntryForm.addEventListener('submit', function(event) {
            const licensePlateInput = this.querySelector('input[name="licensePlate"]');
            if (licensePlateInput && licensePlateInput.value.trim() === '') {
                alert('Please enter a license plate number');
                event.preventDefault();
            }
        });
    }

    // Reservation form validation
    const reservationForm = document.querySelector('form[action*="/reserve"]');
    if (reservationForm) {
        reservationForm.addEventListener('submit', function(event) {
            const startTime = new Date(document.getElementById('startTime').value);
            const endTime = new Date(document.getElementById('endTime').value);

            if (endTime <= startTime) {
                alert('End time must be after start time');
                event.preventDefault();
            }
        });
    }
});