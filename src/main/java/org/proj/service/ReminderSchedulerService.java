package org.proj.service;

/**
 * Runs scheduled appointment and queue reminder processing.
 */
public interface ReminderSchedulerService {

    void processAppointmentReminders();

    void processQueueReminders();
}
