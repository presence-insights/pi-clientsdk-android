/**
 * Copyright (c) 2015-2016 IBM Corporation. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.geofencing.geofencing;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Possible types for the value of a variable.
 */
enum ValueType {
    TEXT,
    BOOLEAN {
        @Override
        public Object fromString(String source) {
            return Boolean.valueOf(source);
        }
    },
    NUMBER {
        @Override
        public Object fromString(String source) {
            return Double.valueOf(source);
        }
    },
    DATE {
        @Override
        public Object fromString(String source) throws Exception {
            if ((source == null) || "".equals(source.trim())) {
                return null;
            }
            return createDateFormat().parse(source);
        }

        @Override
        public Object toJSonType(Object value) throws Exception {
            return createDateFormat().format((Date) value);
        }
    };

    /**
     * Date format used to convert dates from/to UTC format such as "2015-08-24T09:00:00-05:00".
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";

    public Object fromString(String source) throws Exception {
        return source;
    }

    public Object toJSonType(Object value) throws Exception {
        return value;
    }

    ;

    private static SimpleDateFormat createDateFormat() {
        // example date: "2015-04-16T14:49:44-05:00"
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }
}
