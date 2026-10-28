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

package org.wso2.carbon.identity.authzen.core.util;

/**
 * Utilities shared by the AuthZEN connector.
 */
public final class AuthZENUtils {

    private AuthZENUtils() {
    }

    /**
     * Replaces carriage returns and line feeds with {@code _}, so a value taken from a request cannot forge
     * extra log lines.
     *
     * @param value Value to write to a log, may be {@code null}.
     * @return The value without CR or LF characters, or {@code null}.
     */
    public static String sanitizeForLog(String value) {

        if (value == null) {
            return null;
        }
        return value.replace('\r', '_').replace('\n', '_');
    }
}
