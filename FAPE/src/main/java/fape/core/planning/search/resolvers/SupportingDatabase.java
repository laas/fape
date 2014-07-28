package fape.core.planning.search.resolvers;

public class SupportingDatabase extends Resolver {

    public final int temporalDatabase;
    public final int precedingChainComponent;

    public SupportingDatabase(int dbID) {
        temporalDatabase = dbID;
        precedingChainComponent = -1;
    }

    public SupportingDatabase(int dbID, int chainComponent) {
        temporalDatabase = dbID;
        precedingChainComponent = chainComponent;
    }

}
