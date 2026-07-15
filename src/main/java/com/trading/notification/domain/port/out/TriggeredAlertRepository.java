package com.trading.notification.domain.port.out;

import com.trading.notification.domain.model.AlertType;
import com.trading.notification.domain.model.TriggeredAlert;

import java.time.LocalDate;
import java.util.List;

/** Outbound port: persistence + de-duplication for proactive threshold alerts. */
public interface TriggeredAlertRepository {

    TriggeredAlert save(TriggeredAlert alert);

    /** True when an alert of this type already fired for the ticker on the given day. */
    boolean existsForDay(String ticker, AlertType alertType, LocalDate day);

    /** Recent triggered alerts, newest first. */
    List<TriggeredAlert> findRecent(int limit);
}
