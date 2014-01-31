/*
 * Copyright 2008-2010 Xebia and the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.xebia.management.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.core.style.ToStringCreator;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.jmx.support.MetricType;

@ManagedResource
public class ServiceStatistics implements SelfNaming {

    /**
     * Returns <code>true</code> if the given <code>throwable</code> or one of
     * its cause is an instance of one of the given <code>throwableTypes</code>.
     */
    public static boolean containsThrowableOfType(Throwable throwable, Class<?>... throwableTypes) {
        List<Throwable> alreadyProcessedThrowables = new ArrayList<Throwable>();
        while (true) {
            if (throwable == null) {
                // end of the list of causes
                return false;
            } else if (alreadyProcessedThrowables.contains(throwable)) {
                // infinite loop in causes
                return false;
            } else {
                for (Class<?> throwableType : throwableTypes) {
                    if (throwableType.isAssignableFrom(throwable.getClass())) {
                        return true;
                    }
                }
                alreadyProcessedThrowables.add(throwable);
                throwable = throwable.getCause();
            }
        }
    }

    private final AtomicInteger businessExceptionCounter = new AtomicInteger();

    private Class<?>[] businessExceptionsTypes;

    private final AtomicInteger communicationExceptionCounter = new AtomicInteger();

    private Class<?>[] communicationExceptionsTypes;

    private final AtomicInteger currentActiveCounter = new AtomicInteger();

    private final AtomicInteger invocationCounter = new AtomicInteger();

    private Semaphore maxActiveSemaphore;

    private long maxActiveSemaphoreAcquisitionMaxTimeInNanos;

    private final ObjectName objectName;

    private final AtomicInteger otherExceptionCounter = new AtomicInteger();

    private final AtomicInteger serviceUnavailableExceptionCounter = new AtomicInteger();

    private final AtomicInteger slowInvocationCounter = new AtomicInteger();

    private long slowInvocationThresholdInNanos;

    private final AtomicLong totalDurationInNanosCounter = new AtomicLong();

    private final AtomicInteger verySlowInvocationCounter = new AtomicInteger();

    private long verySlowInvocationThresholdInNanos;

    /**
     * 
     * @param objectName
     *            identifier of the service
     * @param businessExceptionsTypes
     *            types of exceptions that are categorized as business
     *            exceptions
     * @param communicationExceptionsTypes
     *            types of exceptions that are categorized as communication
     *            exceptions
     */
    public ServiceStatistics(ObjectName objectName,
            Class<?>[] businessExceptionsTypes, Class<?>[] communicationExceptionsTypes) {
        super();
        this.objectName = objectName;
        this.businessExceptionsTypes = businessExceptionsTypes.clone();
        this.communicationExceptionsTypes = communicationExceptionsTypes.clone();
    }

    /**
     * 
     * @param name
     *            identifier of the service
     * @param businessExceptionsTypes
     *            types of exceptions that are categorized as business
     *            exceptions
     * @param communicationExceptionsTypes
     *            types of exceptions that are categorized as communication
     *            exceptions
     * @throws MalformedObjectNameException
     */
    public ServiceStatistics(String name,
            Class<?>[] businessExceptionsTypes, Class<?>[] communicationExceptionsTypes) throws MalformedObjectNameException {
        this(new ObjectName("fr.xebia:type=ServiceStatistics,name=" + name), businessExceptionsTypes, communicationExceptionsTypes);
    }

    /**
     * Decrements the {@link #currentActiveCounter}.
     */
    public void decrementCurrentActiveCount() {
        currentActiveCounter.decrementAndGet();
    }

    /**
     * <p>
     * Number of throwned communication exceptions.
     * </p>
     * <p>
     * Exceptions are categorized in:
     * <ul>
     * <li>{@link #getCommunicationExceptionCount()}</li>
     * <li>{@link #getBusinessExceptionCount()}</li>
     * <li>{@link #getOtherExceptionCount()}</li>
     * </ul>
     * </p>
     * 
     * @see #incrementBusinessExceptionCount()
     */
    @ManagedMetric(description = "Number of business exceptions", metricType = MetricType.COUNTER, category = "throughput")
    public int getBusinessExceptionCount() {
        return businessExceptionCounter.get();
    }

    /**
     * <p>
     * Number of throwned communication exceptions.
     * </p>
     * <p>
     * Exceptions are categorized in:
     * <ul>
     * <li>{@link #getCommunicationExceptionCount()}</li>
     * <li>{@link #getBusinessExceptionCount()}</li>
     * <li>{@link #getOtherExceptionCount()}</li>
     * </ul>
     * </p>
     * 
     * @see #incrementCommunicationExceptionCount()
     */
    @ManagedMetric(description = "Number of communication exceptions (timeout, connection refused, etc)",
            metricType = MetricType.COUNTER, category = "throughput")
    public int getCommunicationExceptionCount() {
        return communicationExceptionCounter.get();
    }

    /**
     * Number of active invocations
     * 
     * see {@link #incrementCurrentActiveCount()}
     * 
     * @see #decrementCurrentActiveCount()
     */
    @ManagedMetric(description = "Number of currently active invocations", metricType = MetricType.GAUGE, category = "utilization")
    public int getCurrentActive() {
        return currentActiveCounter.get();
    }

    /**
     * Number of invocations
     * 
     * @see #incrementInvocationCount()
     */
    @ManagedMetric(description = "Number of invocations", metricType = MetricType.COUNTER, category = "throughput")
    public int getInvocationCount() {
        return invocationCounter.get();
    }

    @ManagedAttribute(description = "Max active connections or -1 if max active invocation is disabled. Can be slightly inacurrate")
    public int getMaxActive() {
        if (this.maxActiveSemaphore == null) {
            return -1;
        } else {
            return this.maxActiveSemaphore.availablePermits() + getCurrentActive();
        }
    }

    @ManagedMetric(description = "Number of available additional active request", category = "dynamic")
    public int getMaxActiveAvailablePermits() {
        if (this.maxActiveSemaphore == null) {
            return -1;
        } else {
            return this.maxActiveSemaphore.availablePermits();
        }
    }

    public Semaphore getMaxActiveSemaphore() {
        return maxActiveSemaphore;
    }

    public long getMaxActiveSemaphoreAcquisitionMaxTimeInNanos() {
        return maxActiveSemaphoreAcquisitionMaxTimeInNanos;
    }

    /**
     * ObjectName of the service statics.
     */
    public ObjectName getObjectName() throws MalformedObjectNameException {
        return objectName;
    }

    @ManagedMetric(description = "Number of non business exceptions excluding communication exceptions", metricType = MetricType.COUNTER,
            category = "throughput")
    public int getOtherExceptionCount() {
        return otherExceptionCounter.get();
    }

    @ManagedMetric(description = "Total Number of non business exceptions", metricType = MetricType.COUNTER,
            category = "throughput")
    public int getTotalExceptionCount() {
        return getBusinessExceptionCount() + //
                getCommunicationExceptionCount() + //
                getOtherExceptionCount() + //
                getServiceUnavailableExceptionCount();
    }

    public AtomicInteger getOtherExceptionCounter() {
        return otherExceptionCounter;
    }

    @ManagedAttribute(description = "Max acquisition duration fur the max active semaphore")
    public long getSemaphoreAcquisitionMaxTimeInMillis() {
        return TimeUnit.MILLISECONDS.convert(maxActiveSemaphoreAcquisitionMaxTimeInNanos, TimeUnit.NANOSECONDS);
    }

    @ManagedMetric(description = "Number of slow 'service unavailable' exceptions",
            metricType = MetricType.COUNTER, category = "throughput")
    public int getServiceUnavailableExceptionCount() {
        return serviceUnavailableExceptionCounter.get();
    }

    @ManagedMetric(description = "Number of slow invocations", metricType = MetricType.COUNTER, category = "throughput")
    public int getSlowInvocationCount() {
        return slowInvocationCounter.get();
    }

    @ManagedAttribute
    public long getSlowInvocationThresholdInMillis() {
        return TimeUnit.MILLISECONDS.convert(slowInvocationThresholdInNanos, TimeUnit.NANOSECONDS);
    }

    public long getSlowInvocationThresholdInNanos() {
        return slowInvocationThresholdInNanos;
    }

    @ManagedAttribute(description = "Total durations in millis of the invocations")
    public long getTotalDurationInMillis() {
        return TimeUnit.MILLISECONDS.convert(getTotalDurationInNanos(), TimeUnit.NANOSECONDS);
    }

    @ManagedMetric(description = "Total durations in nanos of the invocations", metricType = MetricType.COUNTER, unit = "ns",
            category = "throughput")
    public long getTotalDurationInNanos() {
        return totalDurationInNanosCounter.get();
    }

    public AtomicLong getTotalDurationInNanosCounter() {
        return totalDurationInNanosCounter;
    }

    @ManagedMetric(description = "Number of very slow invocations", metricType = MetricType.COUNTER, category = "throughput")
    public int getVerySlowInvocationCount() {
        return verySlowInvocationCounter.get();
    }

    @ManagedAttribute
    public long getVerySlowInvocationThresholdInMillis() {
        return TimeUnit.MILLISECONDS.convert(this.verySlowInvocationThresholdInNanos, TimeUnit.NANOSECONDS);
    }

    public long getVerySlowInvocationThresholdInNanos() {
        return verySlowInvocationThresholdInNanos;
    }

    /**
     * Increment {@link #communicationExceptionCounter}.
     */
    public void incrementBusinessExceptionCount() {
        communicationExceptionCounter.incrementAndGet();
    }

    /**
     * Increment {@link #businessExceptionCounter}.
     */
    public void incrementCommunicationExceptionCount() {
        businessExceptionCounter.incrementAndGet();
    }

    /**
     * <p>
     * Increment {@link #currentActiveCounter}. Pattern :
     * </p>
     * <code><pre>
     * statistics.incrementCurrentActiveCount();
     * try {
     *    ...
     * } finally {
     *    decrementCurrentActiveCount();
     * }
     * <pre></code>
     * 
     * @see #decrementCurrentActiveCount()
     */
    public void incrementCurrentActiveCount() {
        currentActiveCounter.incrementAndGet();
    }

    /**
     * Increment the {@link #communicationExceptionCounter} if the given
     * throwable or one of its cause is an instance of on of
     * {@link #communicationExceptionsTypes} ; otherwise, increment
     * {@link #otherExceptionCounter}.
     */
    public void incrementExceptionCount(Throwable throwable) {

        if (throwable instanceof ServiceUnavailableException) {
            serviceUnavailableExceptionCounter.incrementAndGet();
        } else if (containsThrowableOfType(throwable, communicationExceptionsTypes)) {
            communicationExceptionCounter.incrementAndGet();
        } else if (containsThrowableOfType(throwable, businessExceptionsTypes)) {
            businessExceptionCounter.incrementAndGet();
        } else {
            otherExceptionCounter.incrementAndGet();
        }
    }

    /**
     * Increment {@link #invocationCounter}.
     */
    public void incrementInvocationCount() {
        invocationCounter.incrementAndGet();
    }

    /**
     * Increment {@link #totalDurationInNanosCounter},
     * {@link #invocationCounter} and, if eligible,
     * {@link #verySlowInvocationCounter} or {@link #slowInvocationCounter}.
     * 
     * @param deltaInNanos
     *            delta in nanos
     */
    public void incrementInvocationCounterAndTotalDurationWithNanos(long deltaInNanos) {

        totalDurationInNanosCounter.addAndGet(deltaInNanos);

        invocationCounter.incrementAndGet();

        if (deltaInNanos >= this.verySlowInvocationThresholdInNanos) {
            this.verySlowInvocationCounter.incrementAndGet();
        } else if (deltaInNanos >= this.slowInvocationThresholdInNanos) {
            this.slowInvocationCounter.incrementAndGet();
        }
    }

    /**
     * Increment {@link #otherExceptionCounter}.
     */
    public void incrementOtherExceptionCount() {
        otherExceptionCounter.incrementAndGet();
    }

    /**
     * Increment {@link #serviceUnavailableExceptionCounter}
     */
    public void incrementServiceUnavailableExceptionCount() {
        serviceUnavailableExceptionCounter.incrementAndGet();
    }

    /**
     * Increment {@link #totalDurationInNanosCounter}.
     * 
     * @param deltaInMillis
     *            delta in millis
     */
    public void incrementTotalDurationWithMillis(long deltaInMillis) {
        incrementTotalDurationWithNanos(TimeUnit.NANOSECONDS.convert(deltaInMillis, TimeUnit.MILLISECONDS));
    }

    /**
     * Increment {@link #totalDurationInNanosCounter}.
     * 
     * @param deltaInNanos
     *            delta in nanos
     */
    public void incrementTotalDurationWithNanos(long deltaInNanos) {
        totalDurationInNanosCounter.addAndGet(deltaInNanos);
    }

    public void setBusinessExceptionsTypes(Class<?>[] businessExceptionsTypes) {
        this.businessExceptionsTypes = businessExceptionsTypes.clone();
    }

    public void setCommunicationExceptionsTypes(Class<?>[] communicationExceptionsTypes) {
        this.communicationExceptionsTypes = communicationExceptionsTypes.clone();
    }

    @ManagedAttribute
    public void setMaxActive(int maxActive) {
        if (maxActive > 0) {
            this.maxActiveSemaphore = new Semaphore(maxActive);
        } else {
            this.maxActiveSemaphore = null;
        }
    }

    public void setMaxActiveSemaphoreAcquisitionMaxTimeInNanos(long semaphoreAcquisitionMaxTimeInNanos) {
        this.maxActiveSemaphoreAcquisitionMaxTimeInNanos = semaphoreAcquisitionMaxTimeInNanos;
    }

    @ManagedAttribute(description = "Max acquisition duration fur the max active semaphore")
    public void setSemaphoreAcquisitionMaxTimeInMillis(long semaphoreAcquisitionMaxTimeInMillis) {
        this.maxActiveSemaphoreAcquisitionMaxTimeInNanos = TimeUnit.NANOSECONDS.convert(semaphoreAcquisitionMaxTimeInMillis,
                TimeUnit.MILLISECONDS);
    }

    @ManagedAttribute
    public void setSlowInvocationThresholdInMillis(long slowInvocationThresholdInMillis) {
        this.slowInvocationThresholdInNanos = TimeUnit.NANOSECONDS.convert(slowInvocationThresholdInMillis, TimeUnit.MILLISECONDS);
    }

    public void setSlowInvocationThresholdInNanos(long slowInvocationThresholdInNanos) {
        this.slowInvocationThresholdInNanos = slowInvocationThresholdInNanos;
    }

    @ManagedAttribute
    public void setVerySlowInvocationThresholdInMillis(long verySlowInvocationThresholdInMillis) {
        this.verySlowInvocationThresholdInNanos = TimeUnit.NANOSECONDS.convert(verySlowInvocationThresholdInMillis, TimeUnit.MILLISECONDS);
    }

    public void setVerySlowInvocationThresholdInNanos(long verySlowInvocationThresholdInNanos) {
        this.verySlowInvocationThresholdInNanos = verySlowInvocationThresholdInNanos;
    }

    @Override
    public String toString() {
        return new ToStringCreator(this) //
                .append("objectName", this.objectName) //
                .append("slowInvocationThresholdInMillis", this.getSlowInvocationThresholdInMillis()) //
                .append("verySlowInvocationThresholdInMillis", this.getVerySlowInvocationThresholdInMillis()) //
                .append("communicationExceptionsTypes", this.communicationExceptionsTypes) //
                .append("businessExceptionsTypes", this.businessExceptionsTypes) //
                .append("invocationCount", this.invocationCounter) //
                .append("maxActiveAvailablePermits", getMaxActiveAvailablePermits()) //
                .append("totalDurationInMillis", this.getTotalDurationInMillis()) //
                .toString();
    }
}
