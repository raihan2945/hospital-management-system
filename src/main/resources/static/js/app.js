(() => {
    const enhanceForms = root => {
        root.querySelectorAll('.is-invalid').forEach(field => field.setAttribute('aria-invalid', 'true'));
        root.querySelectorAll('input[type="number"][step="0.01"]').forEach(field => field.setAttribute('inputmode', 'decimal'));
    };
    enhanceForms(document);
    document.querySelector('[data-error-summary]')?.focus();

    // Enhance real confirmation URLs. Every link remains usable without JavaScript.
    const modalElement = document.getElementById('confirmation-modal');
    let trigger = null;
    let loading = false;
    if (modalElement && window.bootstrap?.Modal) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        document.addEventListener('click', async event => {
            const link = event.target.closest('a[data-confirm]');
            if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
            const url = new URL(link.href, location.href);
            if (url.origin !== location.origin) return;
            event.preventDefault();
            if (loading) return;
            loading = true;
            link.setAttribute('aria-busy', 'true');
            try {
                const response = await fetch(url, { headers: { 'Accept': 'text/html' } });
                if (!response.ok) throw new Error('Confirmation page unavailable');
                const page = new DOMParser().parseFromString(await response.text(), 'text/html');
                const panel = page.querySelector('[data-confirmation-panel]');
                const form = panel?.querySelector('form[method="post"]');
                if (!panel || !form) throw new Error('Missing confirmation form');
                const action = new URL(form.getAttribute('action'), url);
                if (action.origin !== location.origin) throw new Error('Invalid confirmation action');
                form.action = action.href;
                const title = panel.querySelector('h1');
                modalElement.querySelector('.modal-title').textContent = title?.textContent || 'Confirm action';
                title?.remove();
                const originalCancel = panel.querySelector('[data-confirm-cancel]');
                if (originalCancel) {
                    const cancel = document.createElement('button');
                    cancel.type = 'button'; cancel.className = originalCancel.className;
                    cancel.textContent = originalCancel.textContent;
                    cancel.setAttribute('data-bs-dismiss', 'modal');
                    cancel.setAttribute('data-modal-cancel', '');
                    originalCancel.replaceWith(cancel);
                }
                modalElement.querySelector('.modal-body').replaceChildren(...panel.childNodes);
                enhanceForms(modalElement);
                trigger = link;
                modal.show();
            } catch {
                location.assign(url.href);
            } finally {
                loading = false;
                link.removeAttribute('aria-busy');
            }
        });
        modalElement.addEventListener('shown.bs.modal', () => modalElement.querySelector('[data-modal-cancel]')?.focus());
        modalElement.addEventListener('hidden.bs.modal', () => {
            modalElement.querySelector('.modal-body').replaceChildren();
            trigger?.focus(); trigger = null;
        });
    }

    // Guard rapid double-clicks only after native validation has accepted the form.
    document.addEventListener('submit', event => {
        const form = event.target;
        if (form.method.toLowerCase() !== 'post' || event.defaultPrevented) return;
        if (form.dataset.submitting === 'true') { event.preventDefault(); return; }
        form.dataset.submitting = 'true'; form.setAttribute('aria-busy', 'true');
        form.querySelectorAll('button[type="submit"]').forEach(button => button.setAttribute('aria-disabled', 'true'));
    });
    // Back/forward cache restores form DOM state; allow a deliberate new submission.
    window.addEventListener('pageshow', () => {
        document.querySelectorAll('form[data-submitting]').forEach(form => {
            delete form.dataset.submitting; form.removeAttribute('aria-busy');
            form.querySelectorAll('[aria-disabled="true"]').forEach(button => button.removeAttribute('aria-disabled'));
        });
    });
})();
