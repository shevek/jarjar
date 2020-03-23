/* This is a local copy of 
 * https://github.com/spring-gradle-plugins/dependency-management-plugin/blob/master/src/main/java/io/spring/gradle/dependencymanagement/internal/dsl/ClosureBackedAction.java
 * This class used to be public and we could import it directly
 * but that is no longer the case.
 */
package org.anarres.gradle.plugin.jarjar;

import groovy.lang.Closure;
import org.gradle.api.Action;

class ClosureBackedAction<T> implements Action<T> {

    private final Closure closure;

    ClosureBackedAction(Closure closure) {
        this.closure = closure;
    }

    @Override
    public void execute(T delegate) {
        this.closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        this.closure.setDelegate(delegate);
        if (this.closure.getMaximumNumberOfParameters() == 0) {
            this.closure.call();
        }
        else {
            this.closure.call(delegate);
        }
    }

}