package com.seeyon.apps.pdfCatchButton.config; // 放在你的包下

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import java.util.concurrent.Executors;

@Configuration
public class ScheduledConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 创建一个包含 5 个线程的线程池
        // 这样不同的定时任务就可以并行跑，互不干扰
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(5));
    }
}