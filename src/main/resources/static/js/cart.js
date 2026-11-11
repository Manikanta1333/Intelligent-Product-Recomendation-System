/* ===================================
   MD Store - Cart Page (cart.js)
   =================================== */

document.addEventListener('DOMContentLoaded', function () {
    renderCart();
    bindCoupon();
});

var SHIPPING_THRESHOLD = 5000;
var appliedDiscount = 0;

function renderCart() {
    var cart = MDStore.Cart.get();
    var container = document.getElementById('cart-items-list');
    var countEl = document.getElementById('cart-count-label');

    if (countEl) countEl.textContent = cart.length;

    if (!container) return;

    if (cart.length === 0) {
        container.innerHTML = '<div class="empty-state"><div class="empty-icon">🛒</div><h3>Your Cart is Empty</h3><p>Looks like you haven\'t added anything yet.</p><a href="/products" class="btn btn-primary btn-lg">Start Shopping</a></div>';
        hideOrderSummary();
        return;
    }

    container.innerHTML = cart.map(function (item) { return buildCartItem(item); }).join('');
    bindCartActions(container);
    updateSummary();
}

function buildCartItem(item) {
    return '<div class="cart-item" id="cart-item-' + item.id + '">' +
        '<div class="cart-item-img">' +
        (item.image ? '<img src="' + item.image + '" alt="' + item.name + '">' : '<div class="product-img-placeholder" style="height:100%;font-size:2rem;display:flex;align-items:center;justify-content:center;background:var(--gray-100)">🛒</div>') +
        '</div>' +
        '<div class="cart-item-details">' +
        '<div class="cart-item-brand">' + (item.brand || '') + '</div>' +
        '<div class="cart-item-name">' + item.name + '</div>' +
        '<div class="cart-item-price">' + MDStore.Format.price(item.price) + ' each</div>' +
        '<div class="cart-item-actions">' +
        '<div class="quantity-selector">' +
        '<button class="qty-btn" onclick="changeQty(' + item.id + ', -1)">−</button>' +
        '<div class="qty-input" id="qty-' + item.id + '">' + item.quantity + '</div>' +
        '<button class="qty-btn" onclick="changeQty(' + item.id + ', 1)">+</button>' +
        '</div>' +
        '<button class="remove-btn" onclick="removeItem(' + item.id + ')" title="Remove">🗑</button>' +
        '</div>' +
        '</div>' +
        '<div class="cart-item-total">' +
        '<div class="total">' + MDStore.Format.price(item.price * item.quantity) + '</div>' +
        '</div>' +
        '</div>';
}

function bindCartActions(container) {
    // Actions are bound via inline onclick — nothing additional needed
}

function changeQty(id, delta) {
    var cart = MDStore.Cart.get();
    var item = cart.find(function (i) { return i.id === id; });
    if (!item) return;
    var newQty = item.quantity + delta;
    if (newQty < 1) { removeItem(id); return; }
    MDStore.Cart.updateQty(id, newQty);
    var qtyEl = document.getElementById('qty-' + id);
    if (qtyEl) qtyEl.textContent = newQty;
    updateSummary();
}

function removeItem(id) {
    MDStore.Cart.remove(id);
    var el = document.getElementById('cart-item-' + id);
    if (el) {
        el.style.opacity = '0';
        el.style.transform = 'translateX(-30px)';
        el.style.transition = 'all 0.3s ease';
        setTimeout(function () { el.remove(); renderCart(); }, 300);
    } else {
        renderCart();
    }
    MDStore.Toast.show('Item removed from cart');
}

function updateSummary() {
    var cart = MDStore.Cart.get();
    var subtotal = cart.reduce(function (s, i) { return s + i.price * i.quantity; }, 0);
    var shipping = subtotal >= SHIPPING_THRESHOLD ? 0 : 99;
    var total = subtotal + shipping - appliedDiscount;

    setText('summary-subtotal', MDStore.Format.price(subtotal));
    setText('summary-shipping', shipping === 0 ? 'FREE' : MDStore.Format.price(shipping));
    setText('summary-discount', appliedDiscount > 0 ? '−' + MDStore.Format.price(appliedDiscount) : '—');
    setText('summary-total', MDStore.Format.price(Math.max(0, total)));

    var shippingRow = document.getElementById('shipping-row');
    if (shippingRow && shipping === 0) {
        shippingRow.style.color = 'var(--green)';
    }
    var shippingMsg = document.getElementById('shipping-message');
    if (shippingMsg) {
        if (shipping > 0) {
            var remaining = SHIPPING_THRESHOLD - subtotal;
            shippingMsg.textContent = 'Add ' + MDStore.Format.price(remaining) + ' more to get FREE shipping!';
            shippingMsg.style.display = 'block';
        } else {
            shippingMsg.textContent = '🎉 You\'ve unlocked FREE shipping!';
            shippingMsg.style.color = 'var(--green)';
            shippingMsg.style.display = 'block';
        }
    }
}

function setText(id, text) {
    var el = document.getElementById(id);
    if (el) el.textContent = text;
}

function hideOrderSummary() {
    var summary = document.getElementById('cart-summary');
    if (summary) summary.style.display = 'none';
}

function bindCoupon() {
    var couponBtn = document.getElementById('apply-coupon');
    if (!couponBtn) return;
    couponBtn.addEventListener('click', function () {
        var code = document.getElementById('coupon-code');
        if (!code) return;
        var val = code.value.trim().toUpperCase();
        var codes = { 'MD100': 100, 'MD200': 200, 'SAVE10': 0, 'WELCOME': 150 };
        if (codes[val] !== undefined) {
            if (val === 'SAVE10') {
                var cart = MDStore.Cart.get();
                var sub = cart.reduce(function (s, i) { return s + i.price * i.quantity; }, 0);
                appliedDiscount = Math.round(sub * 0.1);
            } else {
                appliedDiscount = codes[val];
            }
            MDStore.Toast.show('Coupon "' + val + '" applied! You saved ' + MDStore.Format.price(appliedDiscount), 'success');
            updateSummary();
        } else {
            MDStore.Toast.show('Invalid coupon code', 'error');
        }
    });
}
