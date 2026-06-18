const CART_KEY = "chuanzi_cart_v1";

function defaultStorage() {
  if (typeof window !== "undefined" && window.localStorage) {
    return window.localStorage;
  }
  return null;
}

export function loadCart(storage = defaultStorage()) {
  if (!storage) return [];
  const raw = storage.getItem(CART_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    return [];
  }
}

export function saveCart(items, storage = defaultStorage()) {
  if (!storage) return;
  storage.setItem(CART_KEY, JSON.stringify(items));
}

export function clearCart(storage = defaultStorage()) {
  if (!storage) return;
  storage.removeItem(CART_KEY);
}

export function addToCart(items, dish) {
  const next = [...items];
  const idx = next.findIndex((item) => item.dishId === dish.id);
  const maxQty = dish.maxQuantityPerOrder || 10;
  if (idx >= 0) {
    const newQty = next[idx].quantity + 1;
    if (newQty > maxQty) {
      return { items: next, error: `「${dish.name}」每单最多 ${maxQty} 份` };
    }
    next[idx] = { ...next[idx], quantity: newQty };
  } else {
    if (1 > maxQty) {
      return { items: next, error: `「${dish.name}」每单最多 ${maxQty} 份` };
    }
    next.push({
      dishId: dish.id,
      name: dish.name,
      priceCents: dish.priceCents,
      maxQuantityPerOrder: maxQty,
      quantity: 1
    });
  }
  return { items: next, error: null };
}

export function canAddToCart(items, dish) {
  const maxQty = dish.maxQuantityPerOrder || 10;
  const current = items.find((item) => item.dishId === dish.id);
  const currentQty = current ? current.quantity : 0;
  return currentQty < maxQty;
}

export function isAtMaxQuantity(items, dishId) {
  const current = items.find((item) => item.dishId === dishId);
  if (!current) return false;
  const maxQty = current.maxQuantityPerOrder || 10;
  return current.quantity >= maxQty;
}

export function getMaxQuantity(items, dishId) {
  const current = items.find((item) => item.dishId === dishId);
  return current ? current.maxQuantityPerOrder || 10 : 10;
}

export function setQuantity(items, dishId, quantity) {
  if (quantity <= 0) {
    return { items: items.filter((item) => item.dishId !== dishId), error: null };
  }
  const current = items.find((item) => item.dishId === dishId);
  if (current) {
    const maxQty = current.maxQuantityPerOrder || 10;
    if (quantity > maxQty) {
      return { items, error: `「${current.name}」每单最多 ${maxQty} 份` };
    }
  }
  return {
    items: items.map((item) =>
      item.dishId === dishId ? { ...item, quantity } : item
    ),
    error: null
  };
}

export function removeItem(items, dishId) {
  return items.filter((item) => item.dishId !== dishId);
}

export function totalCents(items) {
  return items.reduce((sum, item) => sum + item.priceCents * item.quantity, 0);
}

export function toOrderItems(items) {
  return items.map((item) => ({
    dishId: item.dishId,
    quantity: item.quantity
  }));
}

export function formatPrice(cents) {
  return `¥${(cents / 100).toFixed(2)}`;
}
