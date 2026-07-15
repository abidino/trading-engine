package com.trading.notification.domain.port.out;

import com.trading.notification.domain.model.AlertLog;

import java.util.List;

/** Outbound port: alert log persistence. */
public interface AlertLogRepository {
    AlertLog save(AlertLog log);
    List<AlertLog> findAll();
}
