package com.aquiva.autotests.rc.internal.reporting.aspects;

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
 * Aspect class for logging actions from any existing Page Object (PO) methods into Allure reports.
 * <p></p>
 * Right now, this class helps to create step definitions in Allure Reports using the following logic:
 * <p> - All class methods in {@link com.aquiva.autotests.rc.page} are logged as a separate Step. </p>
 * <p> - Exception: methods in {@link com.aquiva.autotests.rc.page} with {@link io.qameta.allure.Step}
 * annotation are NOT logged. </p>
 * Details are in the implementation below (see class methods).
 * <p></p>
 * The implementation of this logic may be (and should be) changed whether structure of the framework
 * (with dependent packages) is changed. E.g. package with POs is moved.
 */
@SuppressWarnings("unused")
@Aspect
public class PageObjectStepAspect {
    private static final InheritableThreadLocal<AllureLifecycle> lifecycle = new InheritableThreadLocal<>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    /**
     * Pointcut for catching any PO method as is.
     */
    @Pointcut("execution(* com.aquiva.autotests.rc.page..*(..))")
    public void anyMethod() {
    }

    /**
     * Pointcut for catching any methods annotated with Allure's @Step.
     */
    @Pointcut("@annotation(io.qameta.allure.Step)")
    public void withStepAnnotation() {
    }

    /**
     * Report actions before executing any of the PO methods,
     * that's NOT annotated with @Step.
     * <p></p>
     * This the main method that creates detailed steps for PO methods like this:
     * <p></p>
     * <b> methodName [MethodClass] </b>
     * <p>
     * <i> Parameters list: </i>
     * <p> - param_1 = value_1 </p>
     * <p> - param_2 = value_2 </p>
     *
     * @param joinPoint current intercepted context for aspect
     */
    @Before("anyMethod() && !withStepAnnotation()")
    public void step(JoinPoint joinPoint) {
        var methodSignature = (MethodSignature) joinPoint.getSignature();
        var sourceLocation = joinPoint.getSourceLocation();

        var uuid = UUID.randomUUID().toString();
        var stepName = String.format("%s [%s]",
                methodSignature.getName(),
                methodSignature.getDeclaringType().getSimpleName()
        );

        var parameters = getParameters(methodSignature, joinPoint.getArgs());

        //  Masking password values in page object's parameters in test steps
        for (var parameter : parameters) {
            if (parameter.getName().toLowerCase().contains("password")) {
                parameter.setValue("***");
            }
        }

        var result = new StepResult()
                .setName(stepName)
                .setParameters(parameters);

        getLifecycle().startStep(uuid, result);
    }

    /**
     * Report action after PO method throws an exception.
     * Note: only works on PO methods WITHOUT Allure's @Step annotation.
     *
     * @param e any exception that was thrown during the assertion
     */
    @AfterThrowing(pointcut = "anyMethod() && !withStepAnnotation()", throwing = "e")
    public void stepFailed(Throwable e) {
        getLifecycle().updateStep(s -> s
                .setStatus(getStatus(e).orElse(Status.BROKEN))
                .setStatusDetails(getStatusDetails(e).orElse(null)));
        getLifecycle().stopStep();
    }

    /**
     * Report the end of the successful execution of PO method.
     */
    @AfterReturning(pointcut = "anyMethod() && !withStepAnnotation()")
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