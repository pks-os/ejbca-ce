package se.anatom.ejbca.hardtoken;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBHome;


/**
 * @version $Id: IHardTokenSessionHome.java,v 1.3 2003-09-03 12:47:24 herrvendil Exp $
 */

public interface IHardTokenSessionHome extends EJBHome {

    /**
     * Default create method. Maps to ejbCreate in implementation.
     * @throws CreateException
     * @throws RemoteException
     * @return IHardTokenSessionRemote interface
     */

    IHardTokenSessionRemote create() throws CreateException, RemoteException;

}

