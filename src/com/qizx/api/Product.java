/*
 *    Qizx/open 4.1
 *
 * This code is the open-source version of Qizx.
 * Copyright (C) 2004-2009 Axyana Software -- All rights reserved.
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
package com.qizx.api;

/**
 * Product information.
 */
public interface Product
{
    /** Name of the product, namely: Qizx/open. */
    String PRODUCT_NAME = "Qizx/open";

    /** Name of the vendor of the product. */
    String VENDOR = "XMLmind";

    /** Main web address of the vendor of the product. */
    String VENDOR_URL = "http://www.xmlmind.com/qizx/";

    /** Major version number. */
    int MAJOR_VERSION = 4;

    /** Minor version number. */
    int MINOR_VERSION = 1;

    /** Maintenance version number. */
    int PATCH_VERSION = 0;

    /** Full version number. */
    String FULL_VERSION = MAJOR_VERSION + "." + MINOR_VERSION
                        + (PATCH_VERSION == 0 ? "" : ("p" + PATCH_VERSION));
    
    String VARIANT = "";

    /**
     * Supported major XQuery version number.
     */
    int XQUERY_MAJOR_VERSION = 1;
    /**
     * Supported minor XQuery version number.
     */
    int XQUERY_MINOR_VERSION = 1;
    /**
     * Supported XQuery version.
     */
    String XQUERY_VERSION = XQUERY_MAJOR_VERSION + "." + XQUERY_MINOR_VERSION;
}
