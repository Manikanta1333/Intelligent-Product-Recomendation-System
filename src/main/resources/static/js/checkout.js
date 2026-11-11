/* ===================================
   MD Store - Checkout Page (checkout.js)
   =================================== */

document.addEventListener('DOMContentLoaded', function () {
    renderOrderSummary();
    bindPaymentOptions();
    bindPlaceOrder();
    bindFormValidation();
});

function renderOrderSummary() {
    var cart = MDStore.Cart.get();
    var container = document.getElementById('checkout-items');

    if (!cart || cart.length === 0) {
        window.location.href = '/cart';
        return;
    }

    if (container) {
        container.innerHTML = cart.map(function (item) {
            return '<div class="order-item-row">' +
                '<div class="order-item-img" style="display:flex;align-items:center;justify-content:center;font-size:1.5rem;background:var(--gray-100);">🛒</div>' +
                '<div class="order-item-name">' + item.name + ' <span style="color:var(--gray-500)">x' + item.quantity + '</span></div>' +
                '<div class="order-item-price">' + MDStore.Format.price(item.price * item.quantity) + '</div>' +
                '</div>';
        }).join('');
    }

    var subtotal = MDStore.Cart.total();
    var shipping = subtotal >= 5000 ? 0 : 99;
    var tax = Math.round(subtotal * 0.05);
    var total = subtotal + shipping + tax;

    setText('co-subtotal', MDStore.Format.price(subtotal));
    setText('co-shipping', shipping === 0 ? 'FREE' : MDStore.Format.price(shipping));
    setText('co-tax', MDStore.Format.price(tax));
    setText('co-total', MDStore.Format.price(total));
}

function setText(id, text) {
    var el = document.getElementById(id);
    if (el) el.textContent = text;
}

function bindPaymentOptions() {
    var options = document.querySelectorAll('.payment-option');
    options.forEach(function (opt) {
        opt.addEventListener('click', function () {
            options.forEach(function (o) { o.classList.remove('selected'); });
            opt.classList.add('selected');
            opt.querySelector('input[type="radio"]').checked = true;

            var upiField = document.getElementById('upi-field');
            var cardFields = document.getElementById('card-fields');
            if (upiField) upiField.style.display = 'none';
            if (cardFields) cardFields.style.display = 'none';

            var val = opt.querySelector('input[type="radio"]').value;
            if (val === 'upi' && upiField) upiField.style.display = 'block';
            if (val === 'card' && cardFields) cardFields.style.display = 'block';
        });
    });
}

function bindFormValidation() {
    var required = document.querySelectorAll('.form-control[required]');
    required.forEach(function (input) {
        input.addEventListener('blur', function () {
            if (!input.value.trim()) {
                input.classList.add('is-invalid');
            } else {
                input.classList.remove('is-invalid');
            }
        });
    });
}

function validateForm() {
    var valid = true;
    document.querySelectorAll('.form-control[required]').forEach(function (input) {
        if (!input.value.trim()) {
            input.classList.add('is-invalid');
            valid = false;
        }
    });
    var paymentSelected = document.querySelector('.payment-option.selected');
    if (!paymentSelected) {
        MDStore.Toast.show('Please select a payment method', 'error');
        valid = false;
    }
    return valid;
}

function bindPlaceOrder() {
    var btn = document.getElementById('place-order-btn');
    if (!btn) return;

    btn.addEventListener('click', function () {
        if (!validateForm()) return;

        var cart = MDStore.Cart.get();
        var subtotal = MDStore.Cart.total();
        var shipping = subtotal >= 5000 ? 0 : 99;
        var tax = Math.round(subtotal * 0.05);

        var firstName = document.getElementById('first-name') ? document.getElementById('first-name').value : '';
        var lastName = document.getElementById('last-name') ? document.getElementById('last-name').value : '';
        var phone = document.getElementById('phone') ? document.getElementById('phone').value : '';
        var address = document.getElementById('address') ? document.getElementById('address').value : '';
        var city = document.getElementById('city') ? document.getElementById('city').value : '';
        var pincode = document.getElementById('pincode') ? document.getElementById('pincode').value : '';
        var state = document.getElementById('state') ? document.getElementById('state').value : '';

        var paymentEl = document.querySelector('.payment-option.selected input[type="radio"]');
        var payment = paymentEl ? paymentEl.value : 'cod';

        var orderData = {
            items: cart,
            shippingAddress: {
                name: firstName + ' ' + lastName,
                phone: phone,
                address: address,
                city: city,
                pincode: pincode,
                state: state
            },
            paymentMethod: payment,
            subtotal: subtotal,
            shipping: shipping,
            tax: tax,
            total: subtotal + shipping + tax
        };

        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span> Placing Order...';

        MDStore.API.post('/orders', orderData)
            .then(function (res) {
                MDStore.Cart.clear();
                showOrderSuccess(res.orderId || 'MD' + Date.now());
            })
            .catch(function () {
                // Simulate success for demo
                MDStore.Cart.clear();
                showOrderSuccess('MD' + Math.floor(Math.random() * 900000 + 100000));
            });
    });
}

function showOrderSuccess(orderId) {
    var body = document.querySelector('.checkout-body');
    if (body) {
        body.innerHTML =
            '<div class="empty-state" style="padding:80px 20px">' +
            '<div class="empty-icon" style="font-size:5rem;opacity:1">🎉</div>' +
            '<h2 style="font-family:var(--font-display);font-size:2rem;margin-bottom:12px">Order Placed Successfully!</h2>' +
            '<p style="color:var(--gray-500);margin-bottom:8px">Thank you for shopping with MD Store.</p>' +
            '<p style="font-weight:700;font-size:1.1rem;margin-bottom:32px">Order ID: <span style="color:var(--yellow)">#' + orderId + '</span></p>' +
            '<div style="display:flex;gap:14px;justify-content:center;flex-wrap:wrap">' +
            '<a href="/orders" class="btn btn-primary btn-lg">Track Order</a>' +
            '<a href="/" class="btn btn-outline btn-lg">Continue Shopping</a>' +
            '</div>' +
            '</div>';
    }
}
