/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.asserts.javaagent.extension;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.View;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class HistogramBucketConfig implements AutoConfigurationCustomizerProvider {

    private static final List<Double> DEFAULT_HISTOGRAM_BUCKET_BOUNDARIES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            0d, 5d, 10d, 25d, 50d, 75d, 100d, 250d, 500d, 750d, 1_000d, 2_500d, 5_000d, 7_500d,
                            10_000d, 30_000d, 60_000d, 90_000d, 120_000d));

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        List<Double> histogramBuckets = getHistogramBuckets();
        System.out.println("[HistogramBucketConfig] buckets customized to " + histogramBuckets);

        autoConfiguration.addMeterProviderCustomizer(
                (sdkMeterProviderBuilder, configProperties) ->
                        sdkMeterProviderBuilder.registerView(
                                InstrumentSelector.builder()
                                        .setName("*duration")
                                        .setType(InstrumentType.HISTOGRAM)
                                        .build(),
                                View.builder()
                                        .setAggregation(
                                                Aggregation.explicitBucketHistogram(histogramBuckets))
                                        .build()));
    }

    private List<Double> getHistogramBuckets() {
        List<Double> buckets = getUserSuppliedBuckets();
        if (buckets.size() == 0) {
            buckets = DEFAULT_HISTOGRAM_BUCKET_BOUNDARIES;
        }
        return buckets;
    }

    private List<Double> getUserSuppliedBuckets() {
        try {
            String userSuppliedBuckets = System.getProperty("otel.histogram.buckets");
            if (userSuppliedBuckets == null || userSuppliedBuckets.length() == 0) {
                userSuppliedBuckets = System.getenv("OTEL_HISTOGRAM_BUCKETS");
            }

            if (userSuppliedBuckets != null) {
                List<Double> buckets = Arrays.stream(userSuppliedBuckets.split(","))
                        .map(String::trim)
                        .map(Double::parseDouble)
                        .collect(Collectors.toList());
                return Collections.unmodifiableList(buckets);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
