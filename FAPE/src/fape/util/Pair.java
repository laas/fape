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
package fape.util;

/**
 *
 * @author FD
 */
public class Pair<T, V> {

    public T value1;
    public V value2;

    public Pair() {
    }

    public Pair(T v1, V v2) {
        this.value1 = v1;
        this.value2 = v2;
    }

    @Override
    public String toString() {
        return "["+value1 + ","+value2+"]";
    }
    
    
}
