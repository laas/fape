package fr.laas.fape.planning.core.planning.timelines;

import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.planning.exceptions.FAPEException;
import fr.laas.fape.anml.model.ParameterizedStateVariable;
import planstack.structures.IList;
import planstack.structures.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * records the events for one state variable
 */
public class Timeline {

    public final int mID;

    public final ParameterizedStateVariable stateVariable;

    public final ChainComponent[] chain;

    public Timeline(LogStatement s, int timelineID) {
        chain = new ChainComponent[1];
        chain[0] = new ChainComponent(s);
        mID = timelineID;
        stateVariable = s.sv();
    }

    public Timeline(ParameterizedStateVariable sv, int timelineID) {
        mID =timelineID;
        stateVariable = sv;
        chain = new ChainComponent[0];
    }

    private Timeline(ChainComponent[] chain, int mID, ParameterizedStateVariable sv) {
        this.mID = mID;
        this.chain = chain;
        this.stateVariable = sv;
    }

    public int size() { return chain.length; }

    public ChainComponent get(int position) { return chain[position]; }

    public boolean isEmpty() { return chain.length == 0; }

    public boolean contains(ChainComponent cc) {
        for(ChainComponent cur : chain)
            if(cc.equals(cur))
                return true;
        return false;
    }

    public boolean contains(LogStatement s) {
        for (ChainComponent cc : chain) {
            if (cc.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public Collection<ChainComponent> getComponents() {
        return Collections.unmodifiableList(Arrays.asList(chain));
    }

    /** @return A new Timeline with cc appended to the chain. */
    protected Timeline with(ChainComponent cc) {
        return with(cc, chain.length);
    }

    /** @return A new Timeline with the componenet "toAdd" added at "at" */
    protected Timeline with(ChainComponent toAdd, int at) {
        ChainComponent[] newChain = new ChainComponent[chain.length+1];
        int delta = 0;
        for(int i=0 ; i<chain.length ; i++) {
            if(i == at) // leave space here for element toAdd
                delta = 1;
            newChain[i+delta] = chain[i];
        }
        assert newChain[at] == null;
        newChain[at] = toAdd;

        return new Timeline(newChain, mID, stateVariable);
    }

    protected Timeline without(ChainComponent cc) {
        ChainComponent[] newChain = new ChainComponent[chain.length-1];

        int i = 0;
        for(ChainComponent cur : chain) {
            if(!cur.equals(cc)) {
                newChain[i] = cur;
                i++;
            }
        }

        return new Timeline(newChain, mID, stateVariable);
    }

    public ChainComponent getFirst() { return chain[0]; }

    public ChainComponent getLast() { return chain[chain.length-1]; }

    /** @return A new timeline from which the statement s is removed from cc. */
    public Timeline removeFromChainComponent(ChainComponent cc, LogStatement s) {
        ChainComponent[] newChain = Arrays.copyOf(chain, chain.length);
        boolean found = false;
        for(int i=0 ; i<newChain.length ; i++) {
            if(newChain[i].equals(cc)) {
                newChain[i] = newChain[i].without(s);
                found = true;
                break;
            }
        }
        assert found : "Unable to find this chain component in the timeline.";
        return new Timeline(newChain, mID, stateVariable);
    }

    public Timeline addToChainComponent(ChainComponent cc, ChainComponent toAdd) {
        ChainComponent[] newChain = Arrays.copyOf(chain, chain.length);
        boolean found = false;
        for(int i=0 ; i<newChain.length ; i++) {
            if(newChain[i].equals(cc)) {
                newChain[i] = newChain[i].withAll(toAdd);
                found = true;
                break;
            }
        }
        assert found : "Unable to find this chain component in the timeline.";
        return new Timeline(newChain, mID, stateVariable);
    }

    /** Number of chain components containing a change */
    public int numChanges() {
        int num = 0;
        for(ChainComponent cc : chain)
            if(cc.change)
                num++;
        return num;
    }

    /** Returns the nth chain component containing a change. */
    public ChainComponent getChangeNumber(int changeNumber) {
        assert changeNumber >= 0;
        assert changeNumber < numChanges();
        int currentChange = 0;
        for(ChainComponent cc : chain) {
            if(cc.change) {
                if(currentChange == changeNumber)
                    return cc;
                else
                    currentChange++;
            }
        }
        throw new FAPEException("Problem: finding change number "+changeNumber+" in timeline "+this);
    }

    public LogStatement getEvent(int numEvent) {
        return getChangeNumber(numEvent).getFirst();
    }

    public String Report() {
        String ret = "";
        //ret += "{\n";

        ret += "    " + this.stateVariable + "  :  id=" + mID + "\n";
        for (ChainComponent c : chain) {
            for (LogStatement e : c.statements) {
                ret += "    " + e;
            }
            ret += "\n";
        }

        //ret += "}\n";
        return ret;
    }

    /**
     * @return True if the first statement of this TDB requires support (ie not
     * an assignment)
     */
    public boolean isConsumer() {
        return chain[0].getFirst().needsSupport();
    }

    /**
     * Returns the index of the chain component cc.
     */
    int indexOf(ChainComponent cc) {
        for(int ct = 0; ct < chain.length; ct++) {
            if (chain[ct].equals(cc)) {
                return ct;
            }
        }
        throw new FAPEException("This statement is not present in the database.");
    }

    /**
     * Returns the index of the chain component containing s.
     */
    int indexOfContainer(LogStatement s) {
        for(int ct = 0; ct < chain.length; ct++) {
            if (chain[ct].contains(s)) {
                return ct;
            }
        }
        throw new FAPEException("This statement is not present in the database.");
    }

    /**
     * @return The end time point of the last component inducing a change.
     */
    public TPRef getSupportTimePoint() {
        assert getSupportingComponent() != null : "This database appears to be containing only a persitence. "
                + "Hence it not available for support. " + this.toString();
        return getSupportingComponent().getSupportTimePoint();
    }

    /**
     * @return All time points from the last component.
     */
    public LinkedList<TPRef> getLastTimePoints() {
        assert chain.length > 0 : "Database is empty.";
        LinkedList<TPRef> tps = new LinkedList<>();
        for(LogStatement s : chain[chain.length-1].statements) {
            tps.add(s.end());
        }
        return tps;
    }

    /**
     * @return All time points from the first component.
     */
    public LinkedList<TPRef> getFirstTimePoints() {
        assert chain.length > 0 : "Database is empty";
        LinkedList<TPRef> tps = new LinkedList<>();
        for(LogStatement s : chain[0].statements) {
            tps.add(s.start());
        }
        return tps;
    }

    /**
     * Returns the start time point of the first change statement (assigment or transition) of the database.
     */
    public TPRef getFirstChangeTimePoint() {
        assert chain.length > 0 : "Database is empty";
        assert !hasSinglePersistence() : "Database has no change statements";
        for(ChainComponent cc : chain) {
            if(cc.change)
                return cc.getFirst().start();
        }
        throw new FAPEException("Error: no change statements encountered.");
    }

    public TPRef getConsumeTimePoint() {
        assert chain.length > 0 : "Database is empty.";
        assert chain[0].size() == 1 : "More than one statement in the first component. Should use getFirstTimepoints()";
        return chain[0].getConsumeTimePoint();
    }

    /** Returns a group of timepoints that must be before any change statement occuring after this component */
    public List<TPRef> timepointsPrecedingNextChange(ChainComponent cc) {
        if(!cc.change) {
            return cc.getEndTimepoints();
        } else if(isLastComponent(cc) || getFollowingComponent(cc).change) {
            return cc.getEndTimepoints();
        } else {
            // its a change that supports some persistences
            return getFollowingComponent(cc).getEndTimepoints();
        }
    }

    /**
     * @return The last component of the database containing a change (i.e. an
     * assignment or a transition). It returns null if no such element exists.
     */
    public ChainComponent getSupportingComponent() {
        for (int i = chain.length - 1; i >= 0; i--) {
            if (chain[i].change) {
                assert chain[i].size() == 1;
                return chain[i];
            }
        }
        return null;
    }

    /**
     * @return The first Logstatement of the database producing a change (i.e. an
     * assignment or a transition). It returns null if no such element exists.
     */
    public LogStatement getFirstChange() {
        for (int i = 0 ; i < chain.length ; i++) {
            if (chain[i].change) {
                assert chain[i].size() == 1;
                return chain[i].getFirst() ;
            }
        }
        return null;
    }


    public ChainComponent getChainComponent(int position) {
        return chain[position];
    }

    public boolean isLastComponent(ChainComponent cc) {
        return getLast() == cc;
    }

    public ChainComponent getFollowingComponent(ChainComponent cc) {
        assert !isLastComponent(cc);
        for(int i=0 ; i<size() ; i++) {
            if(get(i) == cc)
                return get(i+1);
        }
        throw new FAPEException("No such component in this timeline");
    }

    /**
     * @return True if there is only persistences
     */
    public boolean hasSinglePersistence() {
        return chain.length == 1 && !chain[0].change;
    }

    /**
     * @return A global variable representing the value at the end of the
     * temporal database
     */
    public VarRef getGlobalSupportValue() {
        return chain[chain.length-1].getSupportValue();
    }

    public VarRef getGlobalConsumeValue() {
        return chain[0].getConsumeValue();
    }

    @Override
    public String toString() {
        String res = "(tdb:" + mID + " dom=[" + this.stateVariable + "] chains=[";

        for (ChainComponent comp : this.chain) {
            for (LogStatement ev : comp.statements) {
                res += ev.toString() + ", ";
            }
        }
        res += "])";

        return res;
    }

    /**
     * Checks if there is not two persistence events following each other in the
     * chain.
     */
    public void checkChainComposition() {
        boolean wasPreviousTransition = true;
        for (ChainComponent cc : this.chain) {
            if (!wasPreviousTransition && !cc.change) {
                throw new FAPEException("We have two persistence events following each other.");
            }
            wasPreviousTransition = cc.change;
        }
    }

    public IList<Pair<LogStatement, LogStatement>> allCausalLinks() {
        IList<Pair<LogStatement, LogStatement>> cls = new IList<>();
        for(int i=0 ; i<chain.length ; i++) {
            ChainComponent supCC = chain[i];

            if(!supCC.change) //supporter must be a change
                continue;

            assert supCC.size() == 1;
            LogStatement sup = supCC.getFirst();

            if(i+1<chain.length) {
                for(LogStatement cons : chain[i+1].statements) {
                    cls = cls.with(new Pair<>(sup, cons));
                }
            }

            if(i+2 < chain.length && !chain[i+1].change) {
                assert chain[i+2].change;
                for(LogStatement cons : chain[i+2].statements) {
                    cls = cls.with(new Pair<>(sup, cons));
                }
            }
        }

        return cls;
    }

    public List<FluentHolding> getCausalLinks() {
        List<FluentHolding> ret = new ArrayList<>();
        if(hasSinglePersistence()) {
            ret.add(new FluentHolding(stateVariable, getGlobalConsumeValue(), getConsumeTimePoint(), getLastTimePoints()));
        } else {
            for (int i = 0; i < numChanges(); i++) {
                ChainComponent cc = getChangeNumber(i);
                if (i + 1 < numChanges()) {
                    TPRef endCausalLink = getChangeNumber(i + 1).getConsumeTimePoint();
                    List<TPRef> endTimes = Collections.singletonList(endCausalLink);
                    FluentHolding cl = new FluentHolding(stateVariable, cc.getSupportValue(), cc.getSupportTimePoint(), endTimes);
                    ret.add(cl);
                } else if (indexOf(cc) < chain.length - 1) {
                    List<TPRef> endTimes = Arrays.asList(chain[indexOf(cc) + 1].statements).stream().map(s -> s.end()).collect(Collectors.toList());
                    FluentHolding cl = new FluentHolding(stateVariable, cc.getSupportValue(), cc.getSupportTimePoint(), endTimes);
                    ret.add(cl);
                }
            }
        }
        return ret;
    }

    public Stream<LogStatement> allStatements() {
        return Arrays.stream(chain).flatMap(cc -> Arrays.stream(cc.statements));
    }
}
