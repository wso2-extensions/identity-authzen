/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.authzen.evaluation;

import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.pip.PIPContext;

import java.util.Collection;

/**
 * An authorization policy. The evaluation service selects an evaluator by {@link #getName()}, which matches the
 * policy identifier an application uses for an API resource (for example {@code RBAC}).
 * <p>
 * The evaluation service checks that the action is defined on the resource and that the application, if any, is
 * authorized for it. An evaluator only answers which actions its policy grants the subject.
 */
public interface PolicyEvaluator {

    /**
     * Policy identifier this evaluator implements.
     */
    default String getName() {

        return getClass().getSimpleName();
    }

    /**
     * When several evaluators implement the same policy, the one with the lowest value is used.
     */
    default int getPriority() {

        return 100;
    }

    /**
     * Returns the actions the policy grants the resolved subject.
     *
     * @param pipContext Resolved subject, resource and application.
     * @return Granted actions. May contain actions that are not defined on the resource.
     * @throws AuthZENException If the policy cannot be evaluated.
     */
    Collection<String> getGrantedActions(PIPContext pipContext) throws AuthZENException;
}
