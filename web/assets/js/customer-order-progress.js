import { apiRequest, showMessage } from "/assets/js/api.js";
import { requireAuth } from "/assets/js/auth.js";
import { formatPrice } from "/assets/js/cart.js";

const listEl = document.getElementById("orders");
const messageEl = document.getElementById("message");
const refreshBtn = document.getElementById("refreshBtn");

let orders = [];

const STATUS_LABELS = {
  NEW: "已下单",
  CONFIRMED: "商家已确认",
  DONE: "已完成",
  CANCELLED: "已取消"
};

async function loadOrders(options = {}) {
  const { showSummary = true } = options;
  try {
    await requireAuth(["CUSTOMER"]);
    orders = await apiRequest("/api/orders");
    renderOrders(orders);
    if (showSummary) {
      showMessage(messageEl, `共 ${orders.length} 笔订单`, "success");
    }
  } catch (error) {
    showMessage(messageEl, error.message || "加载订单失败");
  }
}

function buildTimelineSteps(order) {
  const steps = [];
  const isCancelled = order.status === "CANCELLED";

  steps.push({
    key: "NEW",
    label: STATUS_LABELS.NEW,
    time: order.createdAt,
    done: true,
    description: "订单已提交，等待商家确认"
  });

  if (isCancelled) {
    steps.push({
      key: "CANCELLED",
      label: STATUS_LABELS.CANCELLED,
      time: order.cancelledAt,
      done: true,
      cancelled: true,
      description: "订单已取消"
    });
  } else {
    steps.push({
      key: "CONFIRMED",
      label: STATUS_LABELS.CONFIRMED,
      time: order.confirmedAt,
      done: !!order.confirmedAt,
      description: "商家已确认订单，正在准备中"
    });

    steps.push({
      key: "DONE",
      label: STATUS_LABELS.DONE,
      time: order.doneAt,
      done: !!order.doneAt,
      description: "订单已完成，请享用"
    });
  }

  return steps;
}

function renderOrders(orders) {
  if (!orders.length) {
    listEl.innerHTML = "<p class='muted' data-testid='order-progress-empty'>暂无订单</p>";
    return;
  }

  listEl.innerHTML = orders
    .map((order) => {
      const itemsHtml = order.items
        .map(
          (item) => `<li>${item.dishNameSnapshot} × ${item.quantity} = ${formatPrice(
            item.priceCentsSnapshot * item.quantity
          )}</li>`
        )
        .join("");

      const steps = buildTimelineSteps(order);
      const timelineHtml = renderTimeline(steps);

      return `
        <section class="panel" data-testid="order-progress-card-${order.id}">
          <div class="header-bar">
            <div>
              <h3>订单 #${order.id} <span class="badge" data-testid="order-progress-status-${order.id}">${order.status}</span></h3>
              <p class="muted">下单时间：${order.createdAt}</p>
            </div>
          </div>
          <p>总金额：${formatPrice(order.totalCents)}</p>
          <ul>${itemsHtml}</ul>
          <div class="order-timeline" data-testid="order-timeline-${order.id}">
            <h4>订单进度</h4>
            ${timelineHtml}
          </div>
        </section>
      `;
    })
    .join("");
}

function renderTimeline(steps) {
  return `
    <ol class="timeline">
      ${steps
        .map((step, idx) => {
          const isLast = idx === steps.length - 1;
          const classes = ["timeline-item"];
          if (step.done) classes.push("timeline-done");
          if (step.cancelled) classes.push("timeline-cancelled");
          if (isLast) classes.push("timeline-last");
          return `
            <li class="${classes.join(" ")}">
              <div class="timeline-dot"></div>
              ${!isLast ? '<div class="timeline-line"></div>' : ""}
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="timeline-label">${step.label}</span>
                  ${step.time ? `<span class="timeline-time" data-testid="timeline-time-${step.key}">${step.time}</span>` : ""}
                </div>
                <p class="timeline-description">${step.description}</p>
              </div>
            </li>
          `;
        })
        .join("")}
    </ol>
  `;
}

refreshBtn.addEventListener("click", () => {
  loadOrders({ showSummary: true });
});

loadOrders();
