(() => {
    const form = document.getElementById('bill-form');
    if (!form) return;
    const fields = ['consultationFee', 'serviceCharge', 'medicineCharge', 'otherCharge', 'discount'];
    const preview = document.getElementById('bill-preview');
    // Integer cents keep the preview consistent with the server's decimal arithmetic.
    const cents = value => {
        if (!/^\d{1,11}(\.\d{1,2})?$/.test(value)) return null;
        const [whole, fraction = ''] = value.split('.');
        return Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
    };
    const display = value => `${Math.floor(value / 100)}.${String(value % 100).padStart(2, '0')}`;
    const update = () => {
        const values = fields.map(name => cents(form.elements[name].value));
        preview.hidden = false;
        if (values.some(value => value === null)) {
            preview.textContent = 'Enter valid amounts to preview the total.';
            return;
        }
        const subtotal = values.slice(0, 4).reduce((sum, value) => sum + value, 0);
        preview.textContent = values[4] > subtotal ? 'Discount cannot exceed the subtotal.'
            : `Subtotal: ${display(subtotal)} · Discount: ${display(values[4])} · Total: ${display(subtotal - values[4])}`;
    };
    form.addEventListener('input', update);
    form.elements.appointmentId.addEventListener('change', event => {
        const selected = event.target.selectedOptions[0];
        if (selected.dataset.patient) {
            form.elements.patientId.value = selected.dataset.patient;
            form.elements.consultationFee.value = selected.dataset.fee;
        }
        update();
    });
    update();
})();
