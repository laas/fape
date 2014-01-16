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
public class UnificationConstraintSchema {
    
    public enum EConType{
        FIRST_VALUE, SECOND_VALUE, EVENT
    }
    public EConType typeLeft, typeRight;
    
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
     * @param left
     * @param right
     */
    public UnificationConstraintSchema(int mDecompositionID_, int mEventID_, int decompositionActionID_, int actionEventID_, EConType left, EConType right){
        mDecompositionID = mDecompositionID_;
        mEventID = mEventID_;
        decompositionActionID = decompositionActionID_;
        actionEventID = actionEventID_;
        typeLeft = left;
        typeRight = right;
    }
}
