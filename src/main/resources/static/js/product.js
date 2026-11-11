/* ===================================
   MD Store - Product Detail (product.js)
   =================================== */

document.addEventListener('DOMContentLoaded', function () {
    var id = MDStore.Params.get('id');
    if (!id) { window.location.href = '/products'; return; }
    loadProduct(id);
});

var MOCK_PRODUCT = {
    id: 1,
    name: 'Premium Wireless Headphones',
    brand: 'SoundMax',
    price: 4999,
    oldPrice: 8999,
    image: '',
    category: 'Electronics',
    rating: 4.5,
    reviews: 1240,
    badge: 'hot',
    description: 'Experience sound like never before with the SoundMax Premium Wireless Headphones. Featuring 40mm drivers delivering crisp highs and powerful bass, these headphones are perfect for music lovers and audiophiles alike. With up to 30 hours of battery life, Bluetooth 5.0 connectivity, and a comfortable over-ear design, they\'re built for all-day listening. Active Noise Cancellation blocks out the world so you can focus on what matters — your music.',
    specs: [
        { key: 'Driver Size', value: '40mm' },
        { key: 'Frequency Response', value: '20Hz - 20kHz' },
        { key: 'Battery Life', value: '30 Hours' },
        { key: 'Connectivity', value: 'Bluetooth 5.0 / 3.5mm Jack' },
        { key: 'Weight', value: '280g' },
        { key: 'Noise Cancellation', value: 'Active (ANC)' },
        { key: 'Microphone', value: 'Built-in with Noise Filter' },
        { key: 'Warranty', value: '1 Year Manufacturer' }
    ],
    images: ['', '', '']
};

function loadProduct(id) {
    MDStore.API.get('/products/' + id)
        .then(function (product) { renderProduct(product); })
        .catch(function () { renderProduct(MOCK_PRODUCT); });
}

function renderProduct(p) {
    var discount = p.oldPrice ? Math.round((1 - p.price / p.oldPrice) * 100) : 0;

    document.title = p.name + ' - MD Store';

    var breadcrumb = document.getElementById('product-breadcrumb');
    if (breadcrumb) {
        breadcrumb.innerHTML = '<a href="/">Home</a><span>/</span><a href="/products">Products</a><span>/</span><a href="/products?category=' + (p.category || '').toLowerCase() + '">' + (p.category || '') + '</a><span>/</span><span>' + p.name + '</span>';
    }

    var mainImg = document.getElementById('main-product-image');
    if (mainImg) {
        if (p.image) {
            mainImg.innerHTML = '<img src="' + p.image + '" alt="' + p.name + '">';
        } else {
            mainImg.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;font-size:6rem;background:var(--gray-100)">📱</div>';
        }
    }

    var thumbs = document.getElementById('product-thumbnails');
    if (thumbs) {
        var thumbHtml = '';
        for (var i = 0; i < 4; i++) {
            thumbHtml += '<div class="thumb' + (i === 0 ? ' active' : '') + '" onclick="selectThumb(this,' + i + ')"><div style="display:flex;align-items:center;justify-content:center;height:100%;font-size:1.8rem;background:var(--gray-100)">📱</div></div>';
        }
        thumbs.innerHTML = thumbHtml;
    }

    var brandEl = document.getElementById('product-brand');
    if (brandEl) brandEl.textContent = p.brand;

    var nameEl = document.getElementById('product-name');
    if (nameEl) nameEl.textContent = p.name;

    var ratingEl = document.getElementById('product-rating');
    if (ratingEl) {
        ratingEl.innerHTML = '<span class="stars">' + MDStore.Stars(p.rating) + '</span>' +
            '<span style="font-weight:600;margin-left:6px">' + p.rating + '</span>' +
            '<span class="rating-count">(' + (p.reviews || 0) + ' reviews)</span>';
    }

    var priceEl = document.getElementById('product-price');
    if (priceEl) {
        priceEl.innerHTML = '<span class="price-main">' + MDStore.Format.price(p.price) + '</span>' +
            (p.oldPrice ? '<span class="price-old">' + MDStore.Format.price(p.oldPrice) + '</span>' : '') +
            (discount > 0 ? '<span class="price-discount">' + discount + '% off</span>' : '');
    }

    var descEl = document.getElementById('product-description');
    if (descEl) descEl.textContent = p.description || 'No description available.';

    var specsEl = document.getElementById('product-specs');
    if (specsEl && p.specs) {
        var rows = p.specs.map(function (s) {
            return '<tr><td>' + s.key + '</td><td>' + s.value + '</td></tr>';
        }).join('');
        specsEl.innerHTML = rows;
    }

    var addCartBtn = document.getElementById('add-to-cart-btn');
    if (addCartBtn) {
        addCartBtn.addEventListener('click', function () {
            var qty = parseInt(document.getElementById('qty-value').textContent) || 1;
            MDStore.Cart.add({ id: p.id, name: p.name, brand: p.brand, price: p.price, image: p.image || '' }, qty);
            addCartBtn.textContent = '✓ Added to Cart';
            setTimeout(function () { addCartBtn.innerHTML = '🛒 Add to Cart'; }, 2000);
        });
    }

    var wishlistBtn = document.getElementById('add-to-wishlist-btn');
    if (wishlistBtn) {
        var isWishlisted = MDStore.Wishlist.has(p.id);
        wishlistBtn.innerHTML = (isWishlisted ? '♥ Wishlisted' : '♡ Wishlist');
        wishlistBtn.classList.toggle('btn-outline-yellow', isWishlisted);
        wishlistBtn.addEventListener('click', function () {
            var added = MDStore.Wishlist.toggle(p);
            wishlistBtn.innerHTML = added ? '♥ Wishlisted' : '♡ Wishlist';
            wishlistBtn.classList.toggle('btn-outline-yellow', added);
        });
    }

    initQtySelector();
    loadRelatedProducts(p.category);
}

function selectThumb(el, idx) {
    document.querySelectorAll('.thumb').forEach(function (t) { t.classList.remove('active'); });
    el.classList.add('active');
}

function initQtySelector() {
    var qtyValue = document.getElementById('qty-value');
    var minusBtn = document.getElementById('qty-minus');
    var plusBtn = document.getElementById('qty-plus');

    if (!qtyValue) return;

    if (minusBtn) {
        minusBtn.addEventListener('click', function () {
            var val = parseInt(qtyValue.textContent) || 1;
            if (val > 1) qtyValue.textContent = val - 1;
        });
    }
    if (plusBtn) {
        plusBtn.addEventListener('click', function () {
            var val = parseInt(qtyValue.textContent) || 1;
            if (val < 99) qtyValue.textContent = val + 1;
        });
    }
}

function loadRelatedProducts(category) {
    var container = document.getElementById('related-products');
    if (!container) return;

    container.innerHTML = MDStore.Skeleton.grid(4);

    MDStore.API.get('/products?category=' + (category || '') + '&limit=4')
        .catch(function () {
            return [
                { id: 5, name: 'Yoga Mat Premium', brand: 'FlexFit', price: 1599, oldPrice: 2499, image: '', rating: 4.4, reviews: 692, badge: '' },
                { id: 7, name: 'Water Bottle 1L', brand: 'HydroKing', price: 599, oldPrice: 999, image: '', rating: 4.3, reviews: 2100, badge: '' },
                { id: 9, name: 'Running Shoes', brand: 'SpeedRunner', price: 3299, oldPrice: 5499, image: '', rating: 4.3, reviews: 785, badge: '' },
                { id: 14, name: 'Cricket Bat', brand: 'SportsKing', price: 1899, oldPrice: 3299, image: '', rating: 4.2, reviews: 445, badge: '' }
            ];
        })
        .then(function (products) {
            container.innerHTML = products.slice(0, 4).map(function (p, idx) {
                var discount = p.oldPrice ? Math.round((1 - p.price / p.oldPrice) * 100) : 0;
                return '<div class="product-card">' +
                    '<div class="product-img-wrap"><div class="product-img-placeholder">🛒</div></div>' +
                    '<div class="product-info">' +
                    '<div class="product-brand">' + p.brand + '</div>' +
                    '<div class="product-name">' + p.name + '</div>' +
                    '<div class="product-price-row"><span class="product-price">' + MDStore.Format.price(p.price) + '</span>' +
                    (discount > 0 ? '<span class="product-discount">' + discount + '% off</span>' : '') + '</div>' +
                    '<div class="product-actions">' +
                    '<button class="btn btn-primary btn-sm" onclick="MDStore.Cart.add({id:' + p.id + ',name:\'' + p.name + '\',brand:\'' + p.brand + '\',price:' + p.price + ',image:\'\'})">🛒 Add</button>' +
                    '<a href="/product-detail?id=' + p.id + '" class="btn btn-outline btn-sm">View</a>' +
                    '</div></div></div>';
            }).join('');
        });
}
