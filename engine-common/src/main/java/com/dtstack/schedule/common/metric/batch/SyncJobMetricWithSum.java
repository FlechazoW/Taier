package com.dtstack.schedule.common.metric.batch;



import com.dtstack.schedule.common.metric.Filter;
import com.dtstack.schedule.common.metric.QueryInfo;
import com.dtstack.schedule.common.metric.prometheus.func.CommonFunc;

import java.util.ArrayList;
import java.util.List;

/**
 * Reason:
 * Date: 2019/4/29
 * Company: www.dtstack.com
 * @author xuchao
 */

public class SyncJobMetricWithSum extends BaseMetric {

    @Override
    protected QueryInfo buildQueryInfo() {
        QueryInfo queryInfo = new QueryInfo();

        Filter filter = new Filter();
        filter.setType("=");
        filter.setFilter(jobId);
        filter.setTagk("job_id");

        List<Filter> filterList = new ArrayList<>();
        filterList.add(filter);

        queryInfo.setFilters(filterList);

        CommonFunc func = new CommonFunc("sum");
        queryInfo.addAggregator(func);
        return queryInfo;
    }
}
