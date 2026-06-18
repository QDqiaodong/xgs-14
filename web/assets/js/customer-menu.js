import { apiRequest, showMessage } from "/assets/js/api.js";
import { requireAuth } from "/assets/js/auth.js";
import {
  addToCart,
  canAddToCart,
  clearCart,
  formatPrice,
  loadCart,
  saveCart,
  setQuantity,
  toOrderItems,
  totalCents
} from "/assets/js/cart.js";

const dishesBody = document.getElementById("dishesBody");
const cartItemsContainer = document.getElementById("cartItemsContainer");
const cartCountBadge = document.getElementById("cartCountBadge");
const sidebarTotal = document.getElementById("sidebarTotal");
const messageEl = document.getElementById("message");
const submitBtn = document.getElementById("submitOrderBtn");
const mobileSubmitBtn = document.getElementById("mobileSubmitBtn");
const mobileCartCount = document.getElementById("mobileCartCount");
const mobileCartTotal = document.getElementById("mobileCartTotal");
const mobileCartBar = document.querySelector(".mobile-cart-bar");
const mobileCartToggle = document.getElementById("mobileCartToggle");
const mobileCartDetail = document.getElementById("mobileCartDetail");
const mobileCartDetailContent = document.getElementById("mobileCartDetailContent");
const mobileCartClose = document.getElementById("mobileCartClose");
const mobileCartOverlay = document.getElementById("mobileCartOverlay");

let dishes = [];
let cart = loadCart();

function renderDishes() {
  dishesBody.innerHTML = dishes
    .map(
      (dish) => {
        const canAdd = canAddToCart(cart, dish);
        const maxQty = dish.maxQuantityPerOrder || 10;
        const currentItem = cart.find((item) => item.dishId === dish.id);
        const currentQty = currentItem ? currentItem.quantity : 0;
        const reason = !canAdd ? `已达每单上限 ${maxQty} 份` : "";
        return `
      <tr data-testid="dish-row-${dish.id}">
        <td>${dish.name}</td>
        <td>${formatPrice(dish.priceCents)}</td>
        <td>${dish.description || "-"}</td>
        <td>
          <button 
            data-action="add" 
            data-id="${dish.id}" 
            data-testid="dish-add-${dish.id}"
            ${!canAdd ? "disabled title=\"" + reason + "\"" : ""}
            class="${!canAdd ? "disabled-with-hint" : ""}"
          >${canAdd ? "加入购物车" : "已达上限"}</button>
          ${!canAdd ? `<div class="dish-limit-hint" data-testid="dish-limit-hint-${dish.id}">${reason}（已选 ${currentQty} 份）</div>` : ""}
        </td>
      </tr>
    `;
      }
    )
    .join("");
}

function renderSidebarCart() {
  if (!cart.length) {
    cartItemsContainer.innerHTML = `
      <div class="cart-empty" data-testid="cart-empty">
        <div class="cart-empty-icon">🛒</div>
        <div class="cart-empty-text">购物车空空如也<br/>快去挑选美味菜品吧</div>
      </div>
    `;
    return;
  }

  cartItemsContainer.innerHTML = cart
    .map(
      (item) => {
        const maxQty = item.maxQuantityPerOrder || 10;
        const atMax = item.quantity >= maxQty;
        return `
      <div class="cart-item" data-testid="cart-row-${item.dishId}">
        <div class="cart-item-info">
          <div class="cart-item-name">${item.name}</div>
          <div class="cart-item-price">${formatPrice(item.priceCents)} / 份</div>
          ${atMax ? `<div class="cart-limit-hint" data-testid="cart-limit-hint-${item.dishId}">每单最多 ${maxQty} 份</div>` : ""}
        </div>
        <div class="cart-item-quantity-controls">
          <button data-action="decrease" data-id="${item.dishId}" class="secondary" data-testid="cart-decrease-${item.dishId}">-</button>
          <span class="cart-item-quantity" data-testid="cart-quantity-${item.dishId}">${item.quantity}</span>
          <button 
            data-action="increase" 
            data-id="${item.dishId}" 
            data-testid="cart-increase-${item.dishId}"
            ${atMax ? "disabled title=\"每单最多 " + maxQty + " 份\"" : ""}
          >+</button>
        </div>
        <div class="cart-item-subtotal" data-testid="cart-subtotal-${item.dishId}">${formatPrice(item.priceCents * item.quantity)}</div>
      </div>
    `;
      }
    )
    .join("");
}

function renderMobileCartDetail() {
  if (!cart.length) {
    mobileCartDetailContent.innerHTML = `
      <div class="mobile-cart-empty" data-testid="mobile-cart-empty">
        <div class="mobile-cart-empty-icon">🛒</div>
        <div class="mobile-cart-empty-text">购物车空空如也<br/>快去挑选美味菜品吧</div>
      </div>
    `;
    return;
  }

  mobileCartDetailContent.innerHTML = cart
    .map(
      (item) => {
        const maxQty = item.maxQuantityPerOrder || 10;
        const atMax = item.quantity >= maxQty;
        return `
      <div class="mobile-cart-item" data-testid="mobile-cart-row-${item.dishId}">
        <div class="mobile-cart-item-info">
          <div class="mobile-cart-item-name">${item.name}</div>
          <div class="mobile-cart-item-price">${formatPrice(item.priceCents)} / 份</div>
          ${atMax ? `<div class="mobile-cart-limit-hint" data-testid="mobile-cart-limit-hint-${item.dishId}">每单最多 ${maxQty} 份</div>` : ""}
        </div>
        <div class="mobile-cart-item-quantity-controls">
          <button data-action="decrease" data-id="${item.dishId}" class="secondary" data-testid="mobile-cart-decrease-${item.dishId}">-</button>
          <span class="mobile-cart-item-quantity" data-testid="mobile-cart-quantity-${item.dishId}">${item.quantity}</span>
          <button 
            data-action="increase" 
            data-id="${item.dishId}" 
            data-testid="mobile-cart-increase-${item.dishId}"
            ${atMax ? "disabled title=\"每单最多 " + maxQty + " 份\"" : ""}
          >+</button>
        </div>
        <div class="mobile-cart-item-subtotal" data-testid="mobile-cart-subtotal-${item.dishId}">${formatPrice(item.priceCents * item.quantity)}</div>
      </div>
    `;
      }
    )
    .join("");
}

function renderCart() {
  const total = totalCents(cart);
  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);

  sidebarTotal.textContent = formatPrice(total);
  mobileCartTotal.textContent = formatPrice(total);
  cartCountBadge.textContent = totalItems;
  mobileCartCount.textContent = totalItems;

  if (submitBtn) submitBtn.disabled = cart.length === 0;
  if (mobileSubmitBtn) mobileSubmitBtn.disabled = cart.length === 0;

  if (mobileCartBar) {
    mobileCartBar.classList.toggle("empty", cart.length === 0);
  }

  renderSidebarCart();
  renderMobileCartDetail();
}

function openMobileCart() {
  if (mobileCartDetail) mobileCartDetail.classList.add("open");
  if (mobileCartOverlay) mobileCartOverlay.classList.add("open");
  if (mobileCartBar) mobileCartBar.classList.add("open");
  document.body.style.overflow = "hidden";
}

function closeMobileCart() {
  if (mobileCartDetail) mobileCartDetail.classList.remove("open");
  if (mobileCartOverlay) mobileCartOverlay.classList.remove("open");
  if (mobileCartBar) mobileCartBar.classList.remove("open");
  document.body.style.overflow = "";
}

function toggleMobileCart() {
  if (mobileCartDetail && mobileCartDetail.classList.contains("open")) {
    closeMobileCart();
  } else {
    openMobileCart();
  }
}

async function loadDishes() {
  await requireAuth(["CUSTOMER"]);
  dishes = await apiRequest("/api/dishes");
  renderDishes();
  renderCart();
}

dishesBody.addEventListener("click", (event) => {
  const target = event.target.closest("button");
  if (!target) return;
  const id = Number(target.dataset.id);
  const dish = dishes.find((d) => d.id === id);
  if (!dish) return;
  const result = addToCart(cart, dish);
  if (result.error) {
    showMessage(messageEl, result.error);
    return;
  }
  cart = result.items;
  saveCart(cart);
  renderDishes();
  renderCart();
  showMessage(messageEl, "已加入购物车", "success");
});

cartItemsContainer.addEventListener("click", (event) => {
  const target = event.target.closest("button");
  if (!target) return;
  const id = Number(target.dataset.id);
  const action = target.dataset.action;
  const current = cart.find((item) => item.dishId === id);
  if (!current) return;

  let result;
  if (action === "increase") {
    result = setQuantity(cart, id, current.quantity + 1);
  }
  if (action === "decrease") {
    result = setQuantity(cart, id, current.quantity - 1);
  }
  if (!result) return;

  if (result.error) {
    showMessage(messageEl, result.error);
    return;
  }
  cart = result.items;
  saveCart(cart);
  renderDishes();
  renderCart();
});

if (mobileCartDetailContent) {
  mobileCartDetailContent.addEventListener("click", (event) => {
    const target = event.target.closest("button");
    if (!target) return;
    const id = Number(target.dataset.id);
    const action = target.dataset.action;
    const current = cart.find((item) => item.dishId === id);
    if (!current) return;

    let result;
    if (action === "increase") {
      result = setQuantity(cart, id, current.quantity + 1);
    }
    if (action === "decrease") {
      result = setQuantity(cart, id, current.quantity - 1);
    }
    if (!result) return;

    if (result.error) {
      showMessage(messageEl, result.error);
      return;
    }
    cart = result.items;
    saveCart(cart);
    renderDishes();
    renderCart();
  });
}

if (mobileCartToggle) {
  mobileCartToggle.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    toggleMobileCart();
  });
}

if (mobileCartClose) {
  mobileCartClose.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    closeMobileCart();
  });
}

if (mobileCartOverlay) {
  mobileCartOverlay.addEventListener("click", (e) => {
    e.preventDefault();
    closeMobileCart();
  });
}

async function submitOrder() {
  if (!cart.length) {
    showMessage(messageEl, "购物车为空");
    return;
  }

  try {
    const data = await apiRequest("/api/orders", {
      method: "POST",
      body: { items: toOrderItems(cart) }
    });
    clearCart();
    cart = [];
    closeMobileCart();
    renderCart();
    showMessage(messageEl, `下单成功，订单号 ${data.orderId}`, "success");
    setTimeout(() => {
      window.location.href = "/customer/orders.html";
    }, 500);
  } catch (error) {
    showMessage(messageEl, error.message || "下单失败");
  }
}

submitBtn.addEventListener("click", submitOrder);
if (mobileSubmitBtn) {
  mobileSubmitBtn.addEventListener("click", submitOrder);
}

loadDishes().catch((error) => {
  showMessage(messageEl, error.message || "加载菜品失败");
});
