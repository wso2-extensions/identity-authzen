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

package org.wso2.carbon.identity.authzen.core.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextEntity {

    private Map<String, Object> properties = new LinkedHashMap<>();

    public ContextEntity() {
    }

    public ContextEntity(Map<String, Object> properties) {

        setProperties(properties);
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {

        return properties;
    }

    @JsonIgnore
    public void setProperties(Map<String, Object> properties) {

        this.properties = properties == null ? new LinkedHashMap<>() : new LinkedHashMap<>(properties);
    }

    @JsonAnySetter
    public void addProperty(String key, Object value) {

        properties.put(key, value);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof ContextEntity)) {
            return false;
        }
        ContextEntity that = (ContextEntity) o;
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {

        return Objects.hash(properties);
    }

    @Override
    public String toString() {

        return "ContextEntity{" +
                "properties=" + properties +
                '}';
    }
}
