package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.TaskHelper;
import com.sforce.soap.enterprise.sobject.*;

import java.util.Calendar;
import java.util.*;

/**
 * Factory class for creating quick instances of {@link Task} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class TaskFactory extends SObjectFactory {
    //  Default values to include in Task.Subject field
    private static final String DEFAULT_TASK_SUBJECT = "TestTask";

    /**
     * Create Standard Task object with future activity's date and insert it into Salesforce via API.
     *
     * @param ownerLead task owner's Lead (for Task.WhoId field)
     * @param ownerUser task owner's User (for Task.OwnerId field)
     * @return instance of Task with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Task createStandardTask(Lead ownerLead, User ownerUser) throws Exception {
        var task = new Task();

        TaskHelper.setStandardTaskRecordType(task);

        var uniqueName = UUID.randomUUID().toString();
        task.setSubject(DEFAULT_TASK_SUBJECT + " " + uniqueName);

        var activityDateFuture = Calendar.getInstance();
        activityDateFuture.setTime(new Date());
        activityDateFuture.add(Calendar.DATE, 1);
        task.setActivityDate(activityDateFuture);

        task.setOwnerId(ownerUser.getId());
        task.setWhoId(ownerLead.getId());

        CONNECTION_UTILS.insertAndGetIds(task);

        return task;
    }
}
