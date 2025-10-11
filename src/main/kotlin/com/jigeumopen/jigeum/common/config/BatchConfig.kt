package com.jigeumopen.jigeum.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
@EnableScheduling
class BatchConfig : SchedulingConfigurer {
    
    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val taskScheduler = ThreadPoolTaskScheduler().apply {
            poolSize = 2
            setThreadNamePrefix("batch-scheduler-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)
            initialize()
        }
        taskRegistrar.setScheduler(taskScheduler)
    }
}
