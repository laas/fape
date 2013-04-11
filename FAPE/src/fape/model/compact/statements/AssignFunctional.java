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
package fape.model.compact.statements;

import fape.model.compact.Function;

/**
 *
 * @author FD
 */
public class AssignFunctional extends Statement {

    public String label;
    public Function func;

    @Override
    public String toString() {
        return label + " := " + func;
    }
}
