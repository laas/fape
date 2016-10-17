package fr.laas.fape.planning.core.planning.timelines;


import fr.laas.fape.anml.model.concrete.statements.Assignment;
import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.anml.model.concrete.statements.Persistence;
import fr.laas.fape.anml.model.concrete.statements.Transition;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.exceptions.FAPEException;
import fr.laas.fape.planning.util.Reporter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TimelinesManager implements Reporter, Iterable<Timeline> {


    /** All timelines, indexed by their ID */
    private Timeline[] timelines;
    private final State listener;

    private final List<Timeline> consumers;

    private int nextTimelineID;

    public TimelinesManager(TimelinesManager toCopy, State containingState) {
        listener = containingState;
        this.consumers = new LinkedList<>(toCopy.consumers);
        this.timelines = Arrays.copyOf(toCopy.timelines, toCopy.timelines.length);
        this.nextTimelineID = toCopy.nextTimelineID;

        if(Planner.debugging) {
            for (Timeline a : consumers) assert hasTimeline(a);
            for (Timeline a : timelines) if (a != null && a.isConsumer()) assert consumers.contains(a);
        }
    }

    public TimelinesManager(State containingState) {
        timelines = new Timeline[100];
        listener = containingState;
        consumers = new LinkedList<>();
    }

    private boolean hasTimeline(Timeline tl) {
        for(Timeline o : timelines)
            if(o == tl)
                return true;
        return false;
    }

    public Collection<Timeline> getConsumers() {
        return Collections.unmodifiableList(consumers);
    }

    /**
     * Creates a new timeline containing the Statement s and adds it to this Manager
     */
    public Timeline addNewTimeline(LogStatement s) {
        Timeline tl = new Timeline(s, nextTimelineID++);
        addTimeline(tl);
        return tl;
    }

    public Collection<FluentHolding> getAllCausalLinks() {
        return StreamSupport.stream(getTimelines().spliterator(), false)
                .flatMap(tl -> tl.getCausalLinks().stream())
                .collect(Collectors.toList());
    }

    public void addTimeline(Timeline tl) {
        assert !hasTimeline(tl);
        if(tl.mID >= timelines.length)
            timelines = Arrays.copyOf(timelines, timelines.length*2);
        timelines[tl.mID] = tl;
        if(tl.isConsumer())
            consumers.add(tl);

        listener.timelineAdded(tl);
    }

    public void removeTimeline(Timeline tl) {
        assert timelines[tl.mID] == tl;
        timelines[tl.mID] = null;
        consumers.remove(tl);

        listener.timelineRemoved(tl);
    }

    public Timeline getTimeline(int tdbID) {
        assert timelines[tdbID].mID == tdbID;
        return timelines[tdbID];
    }

    public boolean containsTimelineWithID(int tlID) {
        return tlID < timelines.length && timelines[tlID] != null;
    }

    public Timeline getTimelineContaining(LogStatement s) {
        for(Timeline tl : timelines) {
            if(tl != null && tl.contains(s))
                return tl;
        }
        throw new FAPEException("Unable to find a timeline containing the statement "+s);
    }

    private void update(Timeline tl) {
        assert timelines[tl.mID] != null;
        assert timelines[tl.mID].mID == tl.mID;

        // replace var with the same ID
        timelines[tl.mID] = tl;

        // update consumer list if necessary
        if(tl.size() == 0 || tl.isConsumer()) {
            boolean found = false;
            for(int i=0 ; i<consumers.size() ; i++) {
                if(tl.mID == consumers.get(i).mID) {
                    if(tl.size() == 0) {
                        consumers.remove(i);
                    } else {
                        consumers.set(i, tl);
                    }
                    found = true;
                    break;
                }
            }
            if(!found)
                consumers.add(tl);
        }
    }

    public Timeline extendTimelineWithComponent(Timeline tl, ChainComponent cc, int at) {
        Timeline newTL = tl.with(cc, at);
        update(newTL);
        return newTL;
    }

    public Timeline extendTimelineWithComponent(Timeline tl, ChainComponent cc) {
        Timeline newTL = tl.with(cc);
        update(newTL);
        return newTL;
    }

    public Timeline removeFromTimeline(Timeline tl, ChainComponent cc) {
        Timeline newTL = tl.without(cc);
        update(newTL);
        return newTL;
    }

    public Timeline removeAllFromTimeline(Timeline tl, Collection<ChainComponent> components) {
        Timeline newTL = tl;//tl.without(cc);
        for(ChainComponent cc : components)
            newTL = newTL.without(cc);
        update(newTL);
        return newTL;
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
        if(included.mID == 16)
            assert tdb.size() != 0;
        assert tdb.contains(after);

        int afterIndex = tdb.indexOf(after);

        if(afterIndex+1 < tdb.size() && !tdb.get(afterIndex+1).change) {
            // we were about to perform the insertion just before a persistence event.
            // instead, we make the insertion after the persistence
            afterIndex = afterIndex + 1;
            after = tdb.get(afterIndex);
        }

        if(!included.getFirst().change && !after.change) {
            //  'after' and first ChainComp of 'included'  are both persistence events. We merge them
            // into 'after' before going any further.
            st.addUnificationConstraint(after.getSupportValue(), included.getGlobalConsumeValue());

            // add persistence events to after, and removing them from the included timeline
            tdb = tdb.addToChainComponent(after, included.getFirst());
            update(tdb);
            included = removeFromTimeline(included, included.getFirst());

            enforceChainConstraints(st, tdb, afterIndex - 1);
            enforceChainConstraints(st, tdb, afterIndex);
        }

        // copy all remaining components
        if(included.size() > 0) {
            assert tdb.getLast() == after || included.size() == 1 && !included.getFirst().change:
                    "Integrating a timeline with transitions in the middle of another one. " +
                    "While this should work with the current implementation it might create unexpected problems " +
                    "because of unexpected unification constraints between two non adjacent chain components.";

            int nextInclusion = afterIndex + 1;
            for (ChainComponent c : included.chain) {
                tdb = extendTimelineWithComponent(tdb, c, nextInclusion);
                nextInclusion += 1;
            }

            // add constraints before and after the inserted chain
            enforceChainConstraints(st, tdb, afterIndex);
            enforceChainConstraints(st, tdb, nextInclusion - 1);
        }
        enforceAllConstraints(st, tdb);

        // the new domain is the intersection of both domains
        st.addUnificationConstraint(tdb.stateVariable, included.stateVariable);

        // Remove the included timeline from the system
        removeTimeline(included);
        st.timelineExtended(tdb);
    }

    /**
     * Given a timeline tdb, enforces the unification and temporal constraints between
     * the elements of indexes chainCompIndex and chainCompIndex+1
     */
    public void enforceChainConstraints(State st, Timeline tdb, int chainCompIndex) {
        assert chainCompIndex < tdb.size();

        if(chainCompIndex < tdb.size()-1 && chainCompIndex >= 0) {
            // if we are not already the last element of the chain, we add constraints between
            // the component and its direct follower.
            ChainComponent first = tdb.get(chainCompIndex);
            ChainComponent second = tdb.get(chainCompIndex + 1);

            assert first.change || second.change : "There should not be two persistence following each other";

            st.addUnificationConstraint(first.getSupportValue(), second.getConsumeValue());

            // Enforce all statements of first to be before all statements of second
            for(LogStatement sa : first.statements) {
                for(LogStatement sb : second.statements) {
                    st.enforceBefore(sa.end(), sb.start());
                }
            }
        }
    }

    public void enforceAllConstraints(State st, Timeline tdb) {
        for(int i=0 ; i<tdb.size()-1 ; i++) {
            //enforceChainConstraints(st, tdb, i);
            int j = i+1;
            //for(int j=i+1 ; j<tdb.chain.size() ; j++) {
                for(LogStatement a : tdb.get(i).statements) {
                    for(LogStatement b : tdb.get(j).statements) {
                        st.enforceBefore(a.end(), b.start());
                    }
                }
            //}

        }
    }

    public void breakCausalLink(LogStatement supporter, LogStatement consumer) {
        Timeline db = getTimelineContaining(supporter);
        assert db == getTimelineContaining(consumer) : "The statements are not in the same database.";

        int index = db.indexOfContainer(consumer);
        Timeline newTL = new Timeline(consumer.sv(), nextTimelineID++);
        //add all extra chain components to the new database

        List<ChainComponent> toRemove = new LinkedList<>();
        for (int i = index; i < db.size(); i++) {
            ChainComponent origComp = db.get(i);
            toRemove.add(origComp);
            ChainComponent pc = origComp.deepCopy();
            newTL = extendTimelineWithComponent(newTL, pc);
        }
        db = removeAllFromTimeline(db, toRemove);
        assert !db.isEmpty();
        assert !newTL.isEmpty();

        addTimeline(newTL);
        listener.timelineExtended(db);
    }

    /**
     * Remove a statement from the state. It does so by identifying the temporal
     * database in which the statement appears and removing it from the
     * database. If necessary, the database is split in two.
     *
     * @param s Statement to remove.
     */
    public void removeStatement(LogStatement s) {
        Timeline theDatabase = getTimelineContaining(s);

        // First find which component contains s
        final int ct = theDatabase.indexOfContainer(s);
        final ChainComponent comp = theDatabase.get(ct);

        assert comp != null && theDatabase.get(ct) == comp;

        if (s instanceof Transition) {
            if (ct + 1 < theDatabase.size()) {
                //this was not the last element, we need to create another database and make split

                // the two databases share the same state variable
                Timeline newDB = new Timeline(theDatabase.stateVariable, nextTimelineID++);

                //add all extra chain components to the new database
                List<ChainComponent> remove = new LinkedList<>();
                for (int i = ct + 1; i < theDatabase.size(); i++) {
                    ChainComponent origComp = theDatabase.get(i);
                    remove.add(origComp);
                    ChainComponent pc = origComp.deepCopy();
                    newDB = extendTimelineWithComponent(newDB, pc);
                }
                addTimeline(newDB);
                theDatabase = removeFromTimeline(theDatabase, comp);
                theDatabase = removeAllFromTimeline(theDatabase, remove);
                assert !newDB.isEmpty();
            } else {
                assert comp.size() == 1;
                //this was the last element so we can just remove it and we are done
                theDatabase = removeFromTimeline(theDatabase, comp);
            }

            if(theDatabase.isEmpty()) {
                removeTimeline(theDatabase);
            }
        } else if (s instanceof Persistence) {
            if (comp.size() == 1) {
                // only one statement, remove the whole component
                theDatabase = removeFromTimeline(theDatabase, comp);
            } else {
                // more than one statement, remove only this statement
                theDatabase = theDatabase.removeFromChainComponent(comp, s);
                update(theDatabase);
            }
            if(theDatabase.isEmpty()) {
                removeTimeline(theDatabase);
            }

        } else if(s instanceof Assignment) {
            theDatabase = removeFromTimeline(theDatabase, comp);
            if(theDatabase.isEmpty()) {
                removeTimeline(theDatabase);
            } else {
                assert theDatabase.isConsumer() : "Removing the first element yields a non-consuming database.";
                listener.timelineExtended(theDatabase);
            }
        } else {
            throw new FAPEException("Unknown event type: "+s);
        }
        for(Timeline db : getTimelines())
            assert !db.isEmpty();
    }

    @Override
    public String report() {
        String ret = "";

        for (Timeline b : timelines) {
            if(b != null)
                ret += b.Report();
        }
        ret += "\n";

        return ret;
    }

    public Iterable<Timeline> getTimelines() { return this; }
    public Stream<Timeline> getTimelinesStream() { return StreamSupport.stream(getTimelines().spliterator(), false); }

    public Stream<LogStatement> allStatements() {
        return StreamSupport.stream(getTimelines().spliterator(), false).flatMap(Timeline::allStatements);
    }

    @Override
    public Iterator<Timeline> iterator() {
        return new Iterator<Timeline>() {
            int next = 0;
            @Override
            public boolean hasNext() {
                while(next < timelines.length && timelines[next] == null) next++;
                return next < timelines.length;
            }

            @Override
            public Timeline next() {
                Timeline ret = timelines[next];
                next += 1;
                return ret;
            }
        };
    }
}
