package com.aquiva.autotests.rc.internal.reporting.aspects;

import com.aquiva.autotests.rc.internal.reporting.SelenideListener;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.UUID;

import static io.qameta.allure.util.AspectUtils.getParameters;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * Aspect class for logging some service actions from Selenide library into Allure reports.
 * This class deals with Selenide actions that don't have inner logging with {@link SelenideLogger}:
 * e.g. Selenide.sleep().
 * <p></p>
 * Note: for most Selenide actions, the existing {@link SelenideListener} is enough.
 * This class is a complimentary addition to it.
 */
@SuppressWarnings("unused")
@Aspect
public class SelenideAspect {
    private static final InheritableThreadLocal<AllureLifecycle> lifecycle = new InheritableThreadLocal<>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    /**
     * Pointcut for catching the explicit wait for some duration (sleep).
     */
    @Pointcut("execution(* com.codeborne.selenide.Selenide.sleep(..))")
    public void sleep() {
    }

    /**
     * Report actions before executing any of the Selenide's service actions
     *
     * @param joinPoint current intercepted context for aspect
     */
    @Before("sleep()")
    public void logServiceAction(JoinPoint joinPoint) {
        var methodSignature = (MethodSignature) joinPoint.getSignature();

        var uuid = UUID.randomUUID().toString();
        var name = methodSignature.getName();

        var parameters = getParameters(methodSignature, joinPoint.getArgs());

        var result = new StepResult()
                .setName(name)
                .setParameters(parameters);

        getLifecycle().startStep(uuid, result);
    }

    /**
     * Report action after service method throws an exception.
     *
     * @param e any exception that was thrown during the action
     */
    @AfterThrowing(pointcut = "sleep()", throwing = "e")
    public void stepFailed(Throwable e) {
        getLifecycle().updateStep(s -> s
                .setStatus(getStatus(e).orElse(Status.BROKEN))
                .setStatusDetails(getStatusDetails(e).orElse(null)));
        getLifecycle().stopStep();
    }

    /**
     * Report the end of the successful execution of service action.
     */
    @AfterReturning(pointcut = "sleep()")
    public void stepStop() {
        getLifecycle().updateStep(s -> s.setStatus(Status.PASSED));
        getLifecycle().stopStep();
    }

    /**
     * Get current Allure Lifecycle object.
     * The main object to manipulate test/step results.
     *
     * @return current Allure Lifecycle context (with collection of test results and more)
     */
    public static AllureLifecycle getLifecycle() {
        return lifecycle.get();
    }
}
