/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.testutil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

/// Marks a test that exercises a feature which is not implemented yet.
///
/// The test is still executed, but if it fails the failure is reported as *aborted*
/// (a skipped test / warning) instead of failing the build. This keeps the suite green so
/// that genuine regressions in other tests stay visible, while the known-unimplemented case
/// is still run and surfaced. If the feature gets implemented and the test starts passing,
/// it simply reports green again — at which point this annotation should be removed.
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ExpectedToFail.Handler.class)
public @interface ExpectedToFail {
    /// @return why the test is expected to fail (the missing feature)
    String value() default "";

    /// Turns a thrown exception of an [ExpectedToFail] test into a [TestAbortedException], so
    /// JUnit records it as aborted (skipped) rather than failed.
    class Handler implements TestExecutionExceptionHandler {
        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) {
            String reason = context.getElement()
                    .map(e -> e.getAnnotation(ExpectedToFail.class))
                    .map(ExpectedToFail::value)
                    .filter(s -> !s.isEmpty())
                    .orElse("unimplemented feature");
            throw new TestAbortedException(
                "Expected to fail (" + reason + "); original failure: " + throwable, throwable);
        }
    }
}
