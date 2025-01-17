package com.dtstack.engine.alert.client;

import com.dtstack.engine.alert.AlterConfig;
import com.dtstack.engine.alert.AlterContext;
import com.dtstack.engine.alert.EventMonitor;
import com.dtstack.engine.alert.exception.AlterEventInterruptException;
import com.dtstack.engine.alert.pool.AlterDiscardPolicy;
import com.dtstack.engine.alert.pool.CustomThreadFactory;
import com.dtstack.lang.data.R;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Auther: dazhi
 * @Date: 2021/1/15 10:39 上午
 * @Email:dazhi@dtstack.com
 * @Description:
 */
public abstract class AbstractAlterClient implements AlterClient,Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private AlterConfig alterConfig;
    private LinkedBlockingQueue<AlterContext> alterQueue;
    private ExecutorService executorService;
    private final AtomicBoolean open = new AtomicBoolean(Boolean.TRUE);

    @Override
    public void setConfig(AlterConfig config) {
        if (config == null) {
            this.alterConfig = new AlterConfig();
        } else {
            this.alterConfig = config;
        }

        init();
    }

    private void init(){
        if (alterQueue == null) {
            alterQueue = new LinkedBlockingQueue<>();
        }

        String threadName = this.getClass().getSimpleName() + "_" + getAlertGateCode()+"_alterJob";

        executorService = new ThreadPoolExecutor(
                alterConfig.getJobExecutorPoolCorePoolSize(),
                alterConfig.getJobExecutorPoolMaximumPoolSize(),
                alterConfig.getJobExecutorPoolKeepAliveTime(),
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new CustomThreadFactory(threadName),
                new AlterDiscardPolicy(threadName, getAlertGateCode()));
        executorService.submit(this);
    }


    @Override
    public R sendSyncAlter(AlterContext alterContext, List<EventMonitor> eventMonitors) throws Exception {
        eventMonitors = setDefaultEvent(alterContext, eventMonitors);
        for (EventMonitor eventMonitor : eventMonitors) {
            if (!eventMonitor.startEvent(alterContext)) {
                return R.ok();
            }
        }
        return sendAlter(alterContext, eventMonitors);
    }

    @Override
    public void sendAsyncAAlter(AlterContext alterContext, List<EventMonitor> eventMonitors) throws Exception {
        logger.info("开始进入队列: id {}", alterContext.getMark());
        eventMonitors = setDefaultEvent(alterContext, eventMonitors);

        if (alterQueue.contains(alterContext)) {
            logger.info("元素:" + alterContext.getMark() + "已在队列中存在");
            return;
        }

        if (alterQueue.size() > alterConfig.getQueueSize()) {
            // 响应告警拒绝事件
            logger.info("元素:" + alterContext.getMark() + "被拒绝");

            eventMonitors.forEach(eventMonitor -> eventMonitor.refuseEvent(alterContext));
        } else {
            logger.info("元素:" + alterContext.getMark() + "进入队列");

            eventMonitors.forEach(eventMonitor -> eventMonitor.joiningQueueEvent(alterContext));
            alterQueue.put(alterContext);
        }
    }

    private List<EventMonitor> setDefaultEvent(AlterContext alterContext, List<EventMonitor> eventMonitors) {
        if (CollectionUtils.isEmpty(alterContext.getEventMonitors())) {
            if (CollectionUtils.isEmpty(eventMonitors)) {
                eventMonitors = Lists.newArrayList();
            }

            alterContext.setEventMonitors(eventMonitors);
        }

        return eventMonitors;
    }

    public void close(){
        open.compareAndSet( Boolean.TRUE,Boolean.FALSE);
        executorService.shutdown();
    }

    @Override
    public void run() {
        while (open.get()) {
            try {
                AlterContext alterContext = alterQueue.poll(30, TimeUnit.SECONDS);
                if (alterContext != null) {
                    // 响应出队事件
                    logger.info("元素: id {} 离开队列",alterContext.getMark());
                    List<EventMonitor> eventMonitors = alterContext.getEventMonitors();
                    sendAlter(alterContext,eventMonitors);
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    private R sendAlter(AlterContext alterContext, List<EventMonitor> eventMonitors) throws Exception {
        R r = null;
        try {
            eventMonitors.forEach(eventMonitor ->eventMonitor.leaveQueueAndSenderBeforeEvent(alterContext));
            logger.info("元素: id {} 开始发送告警", alterContext.getMark());
            r = send(alterContext);
            R finalR1 = r;

            eventMonitors.forEach(eventMonitor -> eventMonitor.alterSuccess(alterContext, finalR1));
            return r;
        } catch (Exception e) {
            logger.error("", e);
            // 触发告警失败事件
            R finalR = r;
            eventMonitors.forEach(eventMonitor -> eventMonitor.alterFailure(alterContext, finalR, e));
            throw e;
        }
    }

    /**
     * 发送告警
     *
     * @param alterContext
     * @return
     */
    protected abstract R send(AlterContext alterContext) throws Exception;

    /**
     * 获得告警类型
     *
     * @return
     */
    protected abstract String getAlertGateCode();

}
