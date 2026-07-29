package ch.ethz.sis.afs.api;

import ch.ethz.sis.afs.manager.State;

/*
    This interface is intended to expose information of the transaction without making changes
 */
public interface TransactionConnectionInformation
{
    boolean isTwoPhaseCommit();

    State getState();
}
