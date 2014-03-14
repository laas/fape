///*
// * Author:  Filip Dvořák <filip.dvorak@runbox.com>
// *
// * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
// *
// * Publishing, providing further or using this program is prohibited
// * without previous written permission of the author. Publishing or providing
// * further the contents of this file is prohibited without previous written
// * permission of the author.
// */
//
//package fape.core.planning.model;
//
//import java.util.Objects;
//
///**
// *
// * @author FD
// */
////public class StateVariable {
////
////    /**
////     *
////     */
////    public enum EStateVariableType{
////
////        /**
////         *
////         */
////        BOOLEAN,
////
////        /**
////         *
////         */
////        FLOAT,
////
////        /**
////         *
////         */
////        INTEGER,
////
////        /**
////         *
////         */
////        ENUM
////    }
////
////    /**
////     *
////     */
////    public String typeDerivationName; // e.g. Robot.mLocation ...
////
////    /**
////     *
////     */
////    public EStateVariableType mType;
////    /**
////     * fully qualifying name, list of nesting separated by dots
////     */
////    public String name;
////
////    /**
////     *
////     */
////    public String type;
////
////    /**
////     *
////     * @return
////     */
////    public String GetObjectConstant(){
////        return name.substring(0, name.indexOf("."));
////    }
////
////    @Override
////    public boolean equals(Object obj) { //TODO, should consider equality to String
////        return ((StateVariable)obj).name.equals(name);
////    }
////
////    @Override
////    public int hashCode() {
////        int hash = 3;
////        hash = 17 * hash + Objects.hashCode(this.name);
////        return hash;
////    }
////
////    @Override
////    public String toString() {
////        return this.name;
////    }
////
////
////
////
////}
