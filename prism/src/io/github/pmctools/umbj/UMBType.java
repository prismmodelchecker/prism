/*
 * Copyright 2025 Dave Parker (University of Oxford)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pmctools.umbj;

import java.util.Objects;

/**
 * Class representing a UMB data type.
 */
public class UMBType
{
    /** UMB types */
    public enum Type implements UMBIndex.UMBField
    {
        BOOL,
        INT, UINT, INT_INTERVAL, UINT_INTERVAL,
        DOUBLE, DOUBLE_INTERVAL, RATIONAL, RATIONAL_INTERVAL,
        STRING;
        @Override
        public String toString()
        {
            return description();
        }

        public boolean isContinuousNumeric()
        {
            switch (this) {
                case DOUBLE:
                case DOUBLE_INTERVAL:
                case RATIONAL:
                case RATIONAL_INTERVAL:
                    return true;
                default:
                    return false;
            }
        }

        public boolean isDouble()
        {
            switch (this) {
                case DOUBLE:
                case DOUBLE_INTERVAL:
                    return true;
                default:
                    return false;
            }
        }

        public boolean isRational()
        {
            switch (this) {
                case RATIONAL:
                case RATIONAL_INTERVAL:
                    return true;
                default:
                    return false;
            }
        }

        public boolean isInterval()
        {
            switch (this) {
                case DOUBLE_INTERVAL:
                case RATIONAL_INTERVAL:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Get the default size (in bits) for this type.
         */
        public Integer defaultSize()
        {
            switch (this) {
                case BOOL:
                    return 1;
                case INT:
                case UINT:
                case DOUBLE:
                    return 64;
                case INT_INTERVAL:
                case UINT_INTERVAL:
                case DOUBLE_INTERVAL:
                    return 128;
                case RATIONAL:
                    return 128;
                case RATIONAL_INTERVAL:
                    return 256;
                case STRING:
                    return 64;
                default:
                    return null;
            }
        }
    }

    /** The type */
    public Type type;

    /** Size (number of bits) for the type; optional */
    public Integer size;

    public static UMBType create(Type type)
    {
        UMBType t = new UMBType();
        t.type = type;
        t.size = type.defaultSize();
        return t;
    }

    public static UMBType create(Type type, Integer size)
    {
        UMBType t = new UMBType();
        t.type = type;
        t.size = size;
        return t;
    }

    public static UMBType contNum(boolean rational)
    {
        return contNum(rational, false);
    }

    public static UMBType contNum(boolean rational, boolean interval)
    {
        if (rational) {
            if (interval) {
                return create(Type.RATIONAL_INTERVAL);
            } else {
                return create(Type.RATIONAL);
            }
        } else {
            if (interval) {
                return create(Type.DOUBLE_INTERVAL);
            } else {
                return create(Type.DOUBLE);
            }
        }
    }

    /**
     * Is the size of this type equal to the default size?
     * (either because it is not specified, or it matches the default)
     */
    public boolean isDefaultSize()
    {
        return size == null || Objects.equals(size, type.defaultSize());
    }
}
