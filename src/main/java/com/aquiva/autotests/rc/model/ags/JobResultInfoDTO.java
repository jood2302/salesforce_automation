package com.aquiva.autotests.rc.model.ags;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.utilities.ags.AGSRestApiClient;

import java.util.List;

/**
 * Data object for job results information retrieved from AGS.
 * <p></p>
 * Useful data structure for parsing responses from AGS
 * (see {@link AGSRestApiClient} for a reference).
 */
public class JobResultInfoDTO extends DataModel {
    public List<JobsItem> jobs;
    public List<AccountsItem> accounts;

    /**
     * Get the main job's results.
     * <p></p>
     * Normally, jobs are submitted with a single batch, so you get a single job as a result.
     *
     * @return job results with useful data (hostname, scenario, status, error details...)
     */
    public JobsItem getMainJob() {
        return jobs.get(0);
    }

    /**
     * Inner data structure for Job Result Info data object.
     * Represents the info about the generated account: user ID, password, phone, etc...
     */
    public static class AccountsItem extends DataModel {
        public Long id;
        public String scenario;
        public Long userId;
        public String password;
        public String mainPhoneNumber;
        public Long jobId;
        public Long brandId;
    }

    /**
     * Inner data structure for Job Result Info data object.
     * Represents main results for a job: job ID, duration, error, status, etc...
     */
    public static class JobsItem extends DataModel {
        public Long id;
        public Long duration;
        public String error;
        public String hostName;
        public String scenario;
        public String status;
    }
}
