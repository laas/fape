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

package fape.core.planning.constraints;

/**
 *
 * @author FD
 */
public class UnifyingConstraintSchema {

    /**
     *
     */
    public int mDecompositionID;

    /**
     *
     */
    public int mEventID;

    /**
     *
     */
    public int decompositionActionID;

    /**
     *
     */
    public int actionEventID;

    /**
     *
     * @param mDecompositionID_
     * @param mEventID_
     * @param decompositionActionID_
     * @param actionEventID_
     */
    public UnifyingConstraintSchema(int mDecompositionID_, int mEventID_, int decompositionActionID_, int actionEventID_){
        mDecompositionID = mDecompositionID_;
        mEventID = mEventID_;
        decompositionActionID = decompositionActionID_;
        actionEventID = actionEventID_;
    }
}
