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

package org.wso2.carbon.identity.authzen.core.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AuthZENExceptionTest {

    @Test
    public void shouldExposeErrorCodeMetadata() {

        Assert.assertEquals(AuthZENErrorCode.MISSING_SUBJECT.getCode(), "AZ-40001");
        Assert.assertEquals(AuthZENErrorCode.INTERNAL_ERROR.getDefaultMessage(), "Internal server error.");
    }

    @Test
    public void shouldExposeClientExceptionFields() {

        AuthZENClientException exception = new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST,
                "Invalid request.", "Subject type is required.");

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.INVALID_REQUEST);
        Assert.assertEquals(exception.getMessage(), "Invalid request.");
        Assert.assertEquals(exception.getDescription(), "Subject type is required.");
    }
}
