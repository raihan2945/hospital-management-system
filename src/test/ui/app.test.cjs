const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');

const root = process.env.HMS_ROOT || path.resolve(__dirname, '../../..');
const app = readFileSync(path.join(root, 'src/main/resources/static/js/app.js'), 'utf8');
const bootstrap = readFileSync(require.resolve('bootstrap/dist/js/bootstrap.bundle.js'), 'utf8');
const tick = () => new Promise(resolve => setTimeout(resolve, 20));
const fixture = (body = '', withBootstrap = true) => {
    const dom = new JSDOM(`<body>${body}<div id="confirmation-modal" class="modal" tabindex="-1"><div class="modal-dialog"><div class="modal-content"><h2 class="modal-title"></h2><div class="modal-body"></div></div></div></div></body>`,
        { url: 'http://localhost:8080/patients', runScripts: 'outside-only', pretendToBeVisual: true });
    if (withBootstrap) dom.window.eval(bootstrap);
    return dom;
};
const confirmation = `<section data-confirmation-panel><h1>Cancel appointment</h1><p>Amina &lt;script&gt;test&lt;/script&gt;</p><form method="post" action="/appointments/42/cancel"><input type="hidden" name="version" value="7"><button type="submit">Confirm cancellation</button><a data-confirm-cancel class="btn" href="/appointments/42">Go back</a></form></section>`;

test('confirmation fetches only GET, preserves action/version and safely renders names', async () => {
    const dom = fixture('<a id="trigger" data-confirm href="/appointments/42/cancel">Cancel</a>');
    const { window } = dom;
    const requests = [];
    window.fetch = async (...args) => { requests.push(args); return { ok: true, text: async () => confirmation }; };
    window.eval(app);
    window.document.getElementById('trigger').click();
    await tick();
    assert.equal(requests.length, 1);
    assert.equal(requests[0][1].method, undefined); // fetch defaults to GET.
    const modal = window.document.getElementById('confirmation-modal');
    assert.ok(modal.classList.contains('show'));
    assert.equal(modal.querySelector('form').action, 'http://localhost:8080/appointments/42/cancel');
    assert.equal(modal.querySelector('[name="version"]').value, '7');
    assert.equal(modal.querySelector('p').textContent, 'Amina <script>test</script>');
    assert.equal(modal.querySelectorAll('script').length, 0);
    assert.equal(window.document.activeElement, modal.querySelector('[data-modal-cancel]'));
    modal.querySelector('[data-modal-cancel]').click();
    await tick();
    assert.ok(!modal.classList.contains('show'));
    assert.equal(modal.querySelectorAll('form').length, 0);
    assert.equal(window.document.activeElement.id, 'trigger');
    assert.equal(requests.length, 1);
    window.close();
});

test('rapid confirmation clicks produce one request and Escape keeps data unchanged', async () => {
    const dom = fixture('<a id="trigger" data-confirm href="/appointments/42/cancel">Cancel</a>');
    const { window } = dom;
    let requests = 0;
    window.fetch = async () => { requests++; await tick(); return { ok: true, text: async () => confirmation }; };
    window.eval(app);
    const link = window.document.getElementById('trigger');
    link.click(); link.click();
    await tick(); await tick();
    assert.equal(requests, 1);
    const modal = window.document.getElementById('confirmation-modal');
    modal.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await tick();
    assert.ok(!modal.classList.contains('show'));
    assert.equal(requests, 1);
    window.close();
});

test('missing Bootstrap leaves original confirmation navigation untouched', () => {
    const dom = fixture('<a id="trigger" data-confirm href="/patients/1/delete">Delete</a>', false);
    dom.window.eval(app);
    const event = new dom.window.MouseEvent('click', { bubbles: true, cancelable: true, ctrlKey: true });
    // Observe whether the enhancer cancels the event; suppress jsdom navigation afterwards.
    let prevented;
    dom.window.document.addEventListener('click', e => { prevented = e.defaultPrevented; e.preventDefault(); });
    dom.window.document.getElementById('trigger').dispatchEvent(event);
    assert.equal(prevented, false);
    dom.window.close();
});

test('modified clicks keep the native open-in-new-tab behavior', () => {
    const dom = fixture('<a id="trigger" data-confirm href="/patients/1/delete">Delete</a>');
    dom.window.fetch = () => { throw new Error('Should not fetch'); };
    dom.window.eval(app);
    let prevented;
    dom.window.document.addEventListener('click', e => { prevented = e.defaultPrevented; e.preventDefault(); });
    dom.window.document.getElementById('trigger').dispatchEvent(new dom.window.MouseEvent('click', { bubbles: true, cancelable: true, ctrlKey: true }));
    assert.equal(prevented, false);
    dom.window.close();
});

test('valid POST submits once and back navigation resets the submission guard', () => {
    const dom = fixture('<form method="post"><input name="name" required value="Amina"><button type="submit">Save</button></form>');
    const { window } = dom;
    window.eval(app);
    const form = window.document.querySelector('form');
    const submit = () => form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
    assert.equal(submit(), true);
    assert.equal(form.getAttribute('aria-busy'), 'true');
    assert.equal(submit(), false);
    window.dispatchEvent(new window.Event('pageshow'));
    assert.equal(form.hasAttribute('aria-busy'), false);
    assert.equal(submit(), true);
    window.close();
});

test('invalid fields and error summary are accessible; money fields request decimal keyboard', () => {
    const dom = fixture('<div tabindex="-1" data-error-summary>Correct the amount</div><input class="is-invalid" type="number" step="0.01">');
    dom.window.eval(app);
    assert.ok(dom.window.document.activeElement.hasAttribute('data-error-summary'));
    const field = dom.window.document.querySelector('input');
    assert.equal(field.getAttribute('aria-invalid'), 'true');
    assert.equal(field.getAttribute('inputmode'), 'decimal');
    dom.window.close();
});
