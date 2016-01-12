/*
 *    Qizx/open 4.1
 *
 * This code is part of the Qizx application components
 * Copyright (C) 2004-2010 Axyana Software -- All rights reserved.
 *
 * The contents of this file are subject to the Mozilla Public License 
 *  Version 1.1 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 * The Initial Developer of the Original Code is Xavier Franc - Axyana Software.
 *
 */
/*
 * Copyright (c) 2002-2008 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.util;

import java.util.Iterator;

/**
 * Base class for objects which can have a set of properties.
 * <p>The implementation attempts to spare memory as much as possible because
 * it assumes that most objects will have no properties at all.
 * <p>This class is <em>not</em> thread-safe.
 */
public abstract class PropertySet {
    protected LinearHashtable<Object,Object> properties = null;

    private static final PropertyIterator NO_PROPERTY_ITERATOR = 
        new PropertyIterator(new LinearHashtable<Object,Object>());

    // -----------------------------------------------------------------------

    /**
     * Adds (or replaces) all the properties found in specified set.
     *
     * @param other contains the properties to be added to this set
     */
    public void putProperties(PropertySet other) {
        if (other.properties == null)
            return;

        if (properties == null)
            properties = new LinearHashtable<Object,Object>();

        Iterator<KeyValuePair<Object,Object>> iter = 
            other.properties.entries();
        while (iter.hasNext()) {
            KeyValuePair<Object,Object> entry = iter.next();

            properties.put(entry.key, entry.value);
        }
    }

    /**
     * Adds or replaces a property.
     * 
     * @param key the property name
     * @param value the property value
     * @return previous property value if property has been replaced or
     * <code>null</code> if property has been added
     */
    public Object putProperty(Object key, Object value) {
        if (properties == null)
            properties = new LinearHashtable<Object,Object>();

        return properties.put(key, value);
    }

    /**
     * Removes a property.
     * 
     * @param key the property name
     * @return previous property value if property has been removed or
     * <code>null</code> otherwise
     */
    public Object removeProperty(Object key) {
        if (properties == null)
            return null;

        Object value = properties.remove(key);

        if (properties.size() == 0)
            properties = null;

        return value;
    }

    /**
     * Removes all properties from set.
     */
    public void removeAllProperties() {
        properties = null;
    }

    /**
     * Tests if this property set contains specified property.
     * 
     * @param key the property name
     * @return <code>true</code> if this property set contains the specified
     * property and <code>false</code> otherwise.
     */
    public boolean hasProperty(Object key) {
        return (properties == null)? false : (properties.get(key) != null);
    }

    /**
     * Returns the value of specified property.
     * 
     * @param key the property name
     * @return the value of the property if found and <code>null</code>
     * otherwise.
     */
    public Object getProperty(Object key) {
        return (properties == null)? null : properties.get(key);
    }

    /**
     * Returns the number of properties contained in this property set.
     * 
     * @return the number of properties contained in this property set
     */
    public int getPropertyCount() {
        return (properties == null)? 0 : properties.size();
    }

    /**
     * Returns an Iterator of the properties contained in this property set.
     * This iterator does not support the remove operation.
     * 
     * @return an Iterator over the properties contained in this property set
     */
    public Iterator<Object[]> getProperties() {
        if (properties == null)
            return NO_PROPERTY_ITERATOR;
        else
            return new PropertyIterator(properties);
    }

    private static class PropertyIterator implements Iterator<Object[]> {
        private Iterator<KeyValuePair<Object,Object>> entries;
        private Object[] keyValuePair = new Object[2];

        public PropertyIterator(LinearHashtable<Object,Object> properties) {
            entries = properties.entries();
        }

        public boolean hasNext() {
            return entries.hasNext();
        }

        public Object[] next() {
            KeyValuePair<Object,Object> entry = entries.next();
            keyValuePair[0] = entry.key;
            keyValuePair[1] = entry.value;
            return keyValuePair;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
