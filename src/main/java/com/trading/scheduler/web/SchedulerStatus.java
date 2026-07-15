package com.trading.scheduler.web;

import java.util.List;

public record SchedulerStatus(boolean running, List<JobInfo> jobs) {}
