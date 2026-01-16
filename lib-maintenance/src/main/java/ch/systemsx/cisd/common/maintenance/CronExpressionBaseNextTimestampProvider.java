package ch.systemsx.cisd.common.maintenance;

import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

final class CronExpressionBaseNextTimestampProvider implements INextTimestampProvider {
    private final CronExpression cron;
    private final ZoneId zone;

    CronExpressionBaseNextTimestampProvider(CronExpression cron, ZoneId zone) {
        this.cron = cron;
        this.zone = zone;
    }

    //@Override
    public Instant next(Instant from) {
        ZonedDateTime start = ZonedDateTime.ofInstant(from, zone);
        ZonedDateTime next = cron.next(start);
        return next.toInstant();
    }

    @Override
    public Date getNextTimestamp(Date from) {
        ZonedDateTime start = ZonedDateTime.ofInstant(from.toInstant(), zone);
        ZonedDateTime next = cron.next(start);
        return (next != null) ? Date.from(next.toInstant()) : null;
    }
}
