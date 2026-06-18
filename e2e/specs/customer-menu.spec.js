import { expect, test } from '@playwright/test';
import { apiCall, expectApiOk } from '../helpers/api.js';
import { loginByApi, loginByUI, registerCustomerByApi } from '../helpers/auth.js';
import { buildCustomer } from '../helpers/data.js';

function isMobile(page) {
  const viewport = page.viewportSize();
  return viewport && viewport.width < 900;
}

async function openMobileCartIfNeeded(page) {
  if (isMobile(page)) {
    const cartToggle = page.getByTestId('mobile-cart-toggle');
    await cartToggle.click({ force: true });
    await page.waitForTimeout(300);
  }
}

function getQuantitySelector(page, dishId) {
  if (isMobile(page)) {
    return page.getByTestId(`mobile-cart-quantity-${dishId}`);
  }
  return page.getByTestId(`cart-quantity-${dishId}`);
}

function getIncreaseSelector(page, dishId) {
  if (isMobile(page)) {
    return page.getByTestId(`mobile-cart-increase-${dishId}`);
  }
  return page.getByTestId(`cart-increase-${dishId}`);
}

function getDecreaseSelector(page, dishId) {
  if (isMobile(page)) {
    return page.getByTestId(`mobile-cart-decrease-${dishId}`);
  }
  return page.getByTestId(`cart-decrease-${dishId}`);
}

function getCartEmptySelector(page) {
  if (isMobile(page)) {
    return page.getByTestId('mobile-cart-empty');
  }
  return page.getByTestId('cart-empty');
}

function getTotalSelector(page) {
  if (isMobile(page)) {
    return page.getByTestId('mobile-cart-total');
  }
  return page.getByTestId('sidebar-total');
}

function getSubmitSelector(page) {
  if (isMobile(page)) {
    return page.getByTestId('mobile-submit-order');
  }
  return page.getByTestId('customer-submit-order');
}

test('CUSTOMER-MENU-01 未登录访问跳登录', async ({ page }) => {
  await page.goto('/customer/menu.html');
  await expect(page).toHaveURL(/\/login\.html\?reason=expired/);
});

test('CUSTOMER-MENU-02 购物车分支 + localStorage 恢复 + 正常下单', async ({ page, request }, testInfo) => {
  const customer = buildCustomer(testInfo, 'cm');
  await registerCustomerByApi(request, customer);

  await loginByUI(page, customer.username, customer.password);
  await expect(page).toHaveURL(/\/customer\/menu\.html/);

  await openMobileCartIfNeeded(page);
  await expect(getCartEmptySelector(page)).toBeVisible();
  await expect(getTotalSelector(page)).toHaveText('¥0.00');

  const firstAdd = page.locator('[data-testid^="dish-add-"]').first();
  const dishId = await firstAdd.getAttribute('data-id');
  await firstAdd.click({ force: true });
  await firstAdd.click({ force: true });

  await openMobileCartIfNeeded(page);
  await expect(getQuantitySelector(page, dishId)).toHaveText('2');

  await getIncreaseSelector(page, dishId).click({ force: true });
  await expect(getQuantitySelector(page, dishId)).toHaveText('3');

  await getDecreaseSelector(page, dishId).click({ force: true });
  await expect(getQuantitySelector(page, dishId)).toHaveText('2');

  await getDecreaseSelector(page, dishId).click({ force: true });
  await getDecreaseSelector(page, dishId).click({ force: true });
  await expect(getCartEmptySelector(page)).toBeVisible();

  await getSubmitSelector(page).click({ force: true });
  await expect(page.getByTestId('customer-menu-message')).toContainText('购物车为空');

  await firstAdd.click({ force: true });
  await openMobileCartIfNeeded(page);
  await expect(getQuantitySelector(page, dishId)).toHaveText('1');

  await page.reload();
  await openMobileCartIfNeeded(page);
  await expect(getQuantitySelector(page, dishId)).toHaveText('1');

  await getSubmitSelector(page).click({ force: true });
  await expect(page.getByTestId('customer-menu-message')).toContainText('下单成功');
  await expect(page).toHaveURL(/\/customer\/orders\.html/);
});

test('CUSTOMER-MENU-03 购物车含下架菜品下单失败', async ({ page, request }, testInfo) => {
  const customer = buildCustomer(testInfo, 'cmf');
  await registerCustomerByApi(request, customer);

  await loginByUI(page, customer.username, customer.password);
  await expect(page).toHaveURL(/\/customer\/menu\.html/);

  const firstAdd = page.locator('[data-testid^="dish-add-"]').first();
  const dishId = Number(await firstAdd.getAttribute('data-id'));
  await firstAdd.click({ force: true });

  const merchant = await loginByApi(request, 'merchant_admin', 'Merchant@123');
  const allDishes = await apiCall(request, 'GET', '/api/dishes?scope=all', {
    cookie: merchant.cookie
  });
  expectApiOk(allDishes);
  const targetDish = allDishes.payload.data.find((dish) => dish.id === dishId);

  const downResult = await apiCall(request, 'PUT', `/api/dishes/${dishId}`, {
    cookie: merchant.cookie,
    body: {
      name: targetDish.name,
      priceCents: targetDish.priceCents,
      description: targetDish.description,
      isAvailable: false
    }
  });
  expectApiOk(downResult);

  await getSubmitSelector(page).click({ force: true });
  await expect(page.getByTestId('customer-menu-message')).toContainText('已下架');
  await expect(page).toHaveURL(/\/customer\/menu\.html/);
});

test('CUSTOMER-MENU-04 localStorage 损坏 JSON 分支恢复为空', async ({ page, request }, testInfo) => {
  const customer = buildCustomer(testInfo, 'cml');
  await registerCustomerByApi(request, customer);

  await loginByUI(page, customer.username, customer.password);
  await expect(page).toHaveURL(/\/customer\/menu\.html/);

  await page.evaluate(() => {
    localStorage.setItem('chuanzi_cart_v1', '{bad-json');
  });
  await page.reload();

  await openMobileCartIfNeeded(page);
  await expect(getCartEmptySelector(page)).toBeVisible();
  await expect(getTotalSelector(page)).toHaveText('¥0.00');
});

test('CUSTOMER-MENU-05 移动端购物车展开收起功能', async ({ page, request }, testInfo) => {
  test.skip(!isMobile(page), '仅在移动端运行');

  const customer = buildCustomer(testInfo, 'cmm');
  await registerCustomerByApi(request, customer);

  await loginByUI(page, customer.username, customer.password);
  await expect(page).toHaveURL(/\/customer\/menu\.html/);

  const firstAdd = page.locator('[data-testid^="dish-add-"]').first();
  const dishId = await firstAdd.getAttribute('data-id');
  await firstAdd.click({ force: true });

  await expect(page.getByTestId('mobile-cart-count')).toHaveText('1');
  await expect(page.getByTestId('mobile-cart-total')).not.toHaveText('¥0.00');

  await expect(page.getByTestId('mobile-cart-detail')).not.toHaveClass(/open/);
  await page.getByTestId('mobile-cart-toggle').click({ force: true });
  await expect(page.getByTestId('mobile-cart-detail')).toHaveClass(/open/);
  await expect(page.getByTestId('mobile-cart-overlay')).toBeVisible();

  await expect(page.getByTestId(`mobile-cart-row-${dishId}`)).toBeVisible();
  await expect(page.getByTestId(`mobile-cart-quantity-${dishId}`)).toHaveText('1');
  await expect(page.getByTestId(`mobile-cart-subtotal-${dishId}`)).not.toHaveText('¥0.00');

  await page.getByTestId('mobile-cart-close').click({ force: true });
  await expect(page.getByTestId('mobile-cart-detail')).not.toHaveClass(/open/);

  await page.getByTestId('mobile-cart-toggle').click({ force: true });
  await expect(page.getByTestId('mobile-cart-detail')).toHaveClass(/open/);

  await page.getByTestId('mobile-cart-overlay').click({ force: true });
  await expect(page.getByTestId('mobile-cart-detail')).not.toHaveClass(/open/);
});
