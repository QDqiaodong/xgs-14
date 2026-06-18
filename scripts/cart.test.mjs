import assert from "node:assert/strict";
import {
  addToCart,
  loadCart,
  saveCart,
  setQuantity,
  removeItem,
  totalCents,
  toOrderItems
} from "../web/assets/js/cart.js";

class MockStorage {
  constructor() {
    this.map = new Map();
  }

  getItem(key) {
    return this.map.has(key) ? this.map.get(key) : null;
  }

  setItem(key, value) {
    this.map.set(key, value);
  }

  removeItem(key) {
    this.map.delete(key);
  }
}

const storage = new MockStorage();
let cart = loadCart(storage);
assert.equal(cart.length, 0);

cart = addToCart(cart, { id: 11, name: "宫保鸡丁", priceCents: 3200, maxQuantityPerOrder: 10 }).items;
cart = addToCart(cart, { id: 11, name: "宫保鸡丁", priceCents: 3200, maxQuantityPerOrder: 10 }).items;
cart = addToCart(cart, { id: 22, name: "鱼香肉丝", priceCents: 2800, maxQuantityPerOrder: 10 }).items;
assert.equal(cart.length, 2);
assert.equal(totalCents(cart), 9200);

cart = setQuantity(cart, 11, 3).items;
assert.equal(totalCents(cart), 12400);

saveCart(cart, storage);
const recovered = loadCart(storage);
assert.equal(recovered.length, 2);
assert.equal(recovered.find((item) => item.dishId === 11).quantity, 3);

const removed = removeItem(recovered, 22);
assert.equal(removed.length, 1);
assert.deepEqual(toOrderItems(removed), [{ dishId: 11, quantity: 3 }]);

const maxLimitDish = { id: 33, name: "限量菜品", priceCents: 5000, maxQuantityPerOrder: 3 };
let limitCart = [];
for (let i = 0; i < 3; i++) {
  const result = addToCart(limitCart, maxLimitDish);
  assert.equal(result.error, null);
  limitCart = result.items;
}
assert.equal(limitCart.find((item) => item.dishId === 33).quantity, 3);

const overLimitResult = addToCart(limitCart, maxLimitDish);
assert.equal(overLimitResult.error, "「限量菜品」每单最多 3 份");
assert.equal(limitCart.find((item) => item.dishId === 33).quantity, 3);

const setOverLimitResult = setQuantity(limitCart, 33, 5);
assert.equal(setOverLimitResult.error, "「限量菜品」每单最多 3 份");

let updatedCart = setQuantity(limitCart, 33, 2).items;
assert.equal(updatedCart.find((item) => item.dishId === 33).quantity, 2);

import { canAddToCart, isAtMaxQuantity, getMaxQuantity } from "../web/assets/js/cart.js";
assert.equal(canAddToCart(updatedCart, maxLimitDish), true);
let fullCart = addToCart(updatedCart, maxLimitDish).items;
fullCart = addToCart(fullCart, maxLimitDish).items;
assert.equal(canAddToCart(fullCart, maxLimitDish), false);
assert.equal(isAtMaxQuantity(fullCart, 33), true);
assert.equal(getMaxQuantity(fullCart, 33), 3);

console.log("cart test passed");
