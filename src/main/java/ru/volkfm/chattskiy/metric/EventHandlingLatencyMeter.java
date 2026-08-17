package ru.volkfm.chattskiy.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.volkfm.chattskiy.model.event.EventType;

@Service
public class EventHandlingLatencyMeter {
    private final String METRIC_NAME_TEMPLATE = "app.event.%s.latency";
    private static final String TAG_EVENT_TYPE = "eventType";

    private final MeterRegistry meterRegistry;

    @AllArgsConstructor
    public enum EventStage {
        INCOMING("incoming"), PUBLISHING("publishing"), OUTSIDE("outside");

        final String name;
    }

    EventHandlingLatencyMeter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        for (EventStage eventStage : EventStage.values()) {
            for (EventType eventType : EventType.values()) {
                Timer.builder(METRIC_NAME_TEMPLATE.formatted(eventStage.name))
                        .tag(TAG_EVENT_TYPE, eventType.name())
                        .description("Time spent handling {} events")
                        .publishPercentileHistogram()
                        .register(this.meterRegistry);
            }
        }
    }

    public Timer.Sample createSample() {
        return Timer.start(meterRegistry);
    }

    public long measure(Timer.Sample sample, EventStage stage, EventType eventType) {
        String metricName = METRIC_NAME_TEMPLATE.formatted(stage.name);
        Timer timer = meterRegistry.timer(metricName, Tags.of(TAG_EVENT_TYPE, eventType.name()));
        return sample.stop(timer);
    }
}
