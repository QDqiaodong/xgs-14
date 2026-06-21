package com.chuanzi.app.service;

import com.chuanzi.app.infra.ApiException;
import com.chuanzi.app.model.DishDailyQuota;
import com.chuanzi.app.repository.DishDailyQuotaRepository;
import com.chuanzi.app.repository.DishRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DishDailyQuotaService {
    private final DishDailyQuotaRepository quotaRepository;
    private final DishRepository dishRepository;

    public DishDailyQuotaService(DishDailyQuotaRepository quotaRepository, DishRepository dishRepository) {
        this.quotaRepository = quotaRepository;
        this.dishRepository = dishRepository;
    }

    public List<Map<String, Object>> listDailyQuotas(String dateText) {
        LocalDate date = parseSaleDate(dateText);
        List<DishDailyQuota> quotas = quotaRepository.listByDate(date);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DishDailyQuota quota : quotas) {
            result.add(quotaRepository.toDto(quota));
        }
        return result;
    }

    public void setDailyQuota(long dishId, String dateText, Integer availableQuantity) {
        LocalDate date = parseSaleDate(dateText);
        if (dishId <= 0) {
            throw ApiException.badRequest("dishId 必须为正整数");
        }
        if (dishRepository.findByIds(Set.of(dishId)).get(dishId) == null) {
            throw ApiException.notFound("菜品不存在");
        }
        int quantity = requireAvailableQuantity(availableQuantity);
        Optional<DishDailyQuota> existing = quotaRepository.findQuota(dishId, date);
        if (existing.isPresent() && quantity < existing.get().soldQuantity()) {
            throw ApiException.conflict("可售数量不能小于已售数量 " + existing.get().soldQuantity());
        }
        quotaRepository.upsertAvailable(dishId, date, quantity);
    }

    private int requireAvailableQuantity(Integer availableQuantity) {
        if (availableQuantity == null || availableQuantity < 0 || availableQuantity > 999999) {
            throw ApiException.badRequest("可售份数需在 0-999999");
        }
        return availableQuantity;
    }

    private LocalDate parseSaleDate(String dateText) {
        if (dateText == null || dateText.isBlank() || "today".equalsIgnoreCase(dateText)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateText.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("saleDate 格式错误，需为 YYYY-MM-DD");
        }
    }
}
