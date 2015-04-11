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
package fape.core.planning.timelines;

import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import fape.util.Reporter;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TimelinesManager implements Reporter {


    /**
     * All temporal timelines.
     */
    private List<Timeline> vars = new LinkedList<>();
    private final State listener;

    public TimelinesManager(TimelinesManager toCopy, State containingState) {
        listener = containingState;

        for (Timeline b : toCopy.getTimelines())
            this.vars.add(b.deepCopy());
    }

    public TimelinesManager(State containingState) {
        listener = containingState;
    }

    /**
     * Creates a new timeline containing the Statement s and adds it to this Manager
     */
    public Timeline getNewTimeline(LogStatement s) {
        Timeline db = new Timeline(s);
        vars.add(db);
        return db;
    }

    public void addTimeline(Timeline tl) {
        for(Timeline existing : vars)
            assert existing.mID != tl.mID : "Timeline already recorded";
        vars.add(tl);
    }

    public void removeTimeline(Timeline tl) {
        assert vars.contains(tl);
        vars.remove(tl);
    }

    public Timeline getTimeline(int tdbID) {
        for(Timeline db : vars) {
            if(db.mID == tdbID)
                return db;
        }
        throw new FAPEException("DB with id "+tdbID+" does not appears in vars \n"+ report());
    }

    public Timeline getTimelineContaining(LogStatement s) {
        for(Timeline db : vars) {
            if(db.contains(s)) {
                return db;
            }
        }
        throw new FAPEException("Unable to find a timeline containing the statement "+s);
    }

    /**
     * Inserts all chains of the timeline @included after the ChainComponent @after of timeline @tdb.
     * All necessary constraints (temporal precedence and unification) are added to State st.
     *
     * If the both the first chain component of @included and the chain component @after are persistence events, those
     * two are merged before proceeding to the insertion.
     *
     * At the the included timeline is completely remove from the search state. All reference to it are replaced by references to tdb
     *
     * @param st State in which the changes are applied
     * @param tdb timeline in which the events will we included
     * @param included timeline that will disappear, all of its components being included in tdb
     * @param after a chain component of tdb after which the chain of included will be added
     */
    public void insertTimelineAfter(State st, Timeline tdb, Timeline included, ChainComponent after) {
        assert tdb.chain.size() != 0;
        assert tdb.chain.contains(after);

        int afterIndex = tdb.chain.indexOf(after);

        if(afterIndex+1 < tdb.chain.size() && !tdb.chain.get(afterIndex+1).change) {
            // we were about to perform the insertion just before a persistence event.
            // instead, we make the insertion after the persistence
            afterIndex = afterIndex + 1;
            after = tdb.chain.get(afterIndex);
        }

        if(!included.chain.getFirst().change && !after.change) {
            //  'after' and first ChainComp of 'included'  are both persistence events. We merge them
            // into 'after' before going any further.
            st.addUnificationConstraint(after.getSupportValue(), included.getGlobalConsumeValue());

            // add persitence events to after, and removing them from the included timeline
            after.add(included.chain.getFirst());
            included.chain.removeFirst();

            enforceChainConstraints(st, tdb, afterIndex - 1);
            enforceChainConstraints(st, tdb, afterIndex);
        }

        // copy all remaining components
        if(included.chain.size() > 0) {
            assert tdb.chain.getLast() == after || included.chain.size() == 1 && !included.chain.getFirst().change:
                    "Integrating a timeline with transitions in the middle of another one. " +
                    "While this should work with the current implementation it might create unexpected problems " +
                    "because of unexpected unification constraints between two non adjacent chain components.";

            int nextInclusion = afterIndex + 1;
            for (ChainComponent c : included.chain) {
                tdb.chain.add(nextInclusion, c);
                nextInclusion += 1;
            }

            // add connstraints before and after the inserted chain
            enforceChainConstraints(st, tdb, afterIndex);
            enforceChainConstraints(st, tdb, nextInclusion - 1);
        }

        enforceAllConstraints(st, tdb);

        // the new domain is the intersection of both domains
        st.addUnificationConstraint(tdb.stateVariable, included.stateVariable);

        // Remove the included timeline from the system
        st.removeTimeline(included);
    }

    /**
     * Given a timeline tdb, enforces the unification and temporal constraints between
     * the elements of indexes chainCompIndex and chainCompIndex+1
     * @param st
     * @param tdb
     * @param chainCompIndex
     */
    public void enforceChainConstraints(State st, Timeline tdb, int chainCompIndex) {
        assert chainCompIndex < tdb.chain.size();

        if(chainCompIndex < tdb.chain.size()-1 && chainCompIndex >= 0) {
            // if we are not already the last element of the chain, we add constraints between
            // the component and its direct follower.
            ChainComponent first = tdb.chain.get(chainCompIndex);
            ChainComponent second = tdb.chain.get(chainCompIndex + 1);

            assert first.change || second.change : "There should not be two persistence following each other";

            st.addUnificationConstraint(first.getSupportValue(), second.getConsumeValue());

            // Enforce all statements of first to be before all statements of second
            for(LogStatement sa : first.contents) {
                for(LogStatement sb : second.contents) {
                    st.enforceStrictlyBefore(sa.end(), sb.start());
                }
            }
        }
    }

    public void enforceAllConstraints(State st, Timeline tdb) {
        for(int i=0 ; i<tdb.chain.size()-1 ; i++) {
            //enforceChainConstraints(st, tdb, i);
            int j = i+1;
            //for(int j=i+1 ; j<tdb.chain.size() ; j++) {
                for(LogStatement a : tdb.chain.get(i).contents) {
                    for(LogStatement b : tdb.chain.get(j).contents) {
                        st.enforceStrictlyBefore(a.end(), b.start());
                    }
                }
            //}

        }
    }

    @Override
    public String report() {
        String ret = "";

        ret += "  size: " + this.vars.size() + "\n";
        for (Timeline b : vars) {
            ret += b.Report();
        }
        ret += "\n";

        return ret;
    }

    public List<Timeline> getTimelines() { return vars; }
}
