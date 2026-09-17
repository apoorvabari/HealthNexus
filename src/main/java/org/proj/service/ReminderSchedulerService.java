package org.proj.service;

public interface ReminderSchedulerService {

    void processAppointmentReminders();

    void processQueueReminders();
}
