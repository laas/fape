/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.model.compact;

/**
 *
 * @author FD
 */
public class TemporalInterval {

    public String s, e;

    @Override
    public String toString() {
        return "[" + e + ", " + s + "]";
    }
}
